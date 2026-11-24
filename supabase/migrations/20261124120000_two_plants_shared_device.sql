-- Two plants per account, both served by one ESP32.
--
-- * plants.cultivar holds the species ('tomato' or 'eggplant'); an account may own at most 2 plants.
-- * add_plant() creates the second plant with the same care settings as the first.
-- * A device has two sensor/actuator slots: slot 1 = devices.plant_id (paired with the physical
--   pairing proof, unchanged), slot 2 = devices.plant_b_id (linked from the app by the owner).
-- * Readings and commands carry the slot, so a Plant A command never runs Plant B's pump.
-- Older firmware keeps working: submit_reading() defaults to slot 1, and slot-1 commands are unchanged.

-- ------------------------------------------------------------------ plants
alter table public.plants alter column cultivar set default 'tomato';
update public.plants set cultivar = 'tomato' where cultivar is null or cultivar not in ('tomato', 'eggplant');
alter table public.plants alter column cultivar set not null;
alter table public.plants add constraint plants_cultivar_check check (cultivar in ('tomato', 'eggplant'));

create function agriquest_private.limit_plants_per_owner()
returns trigger language plpgsql set search_path = '' as $$
begin
    perform pg_advisory_xact_lock(hashtextextended(new.owner_id::text, 80294));
    if (select count(*) from public.plants where owner_id = new.owner_id) >= 2 then
        raise exception 'An account can have at most 2 plants' using errcode = '23514';
    end if;
    return new;
end; $$;
revoke all on function agriquest_private.limit_plants_per_owner() from public, anon, authenticated;
create trigger limit_plants_per_owner before insert on public.plants
for each row execute function agriquest_private.limit_plants_per_owner();

-- Adds the signed-in player's second plant. Care settings are copied from their first plant.
create function public.add_plant(p_species text, p_name text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
    v_owner uuid := auth.uid();
    v_name text := btrim(coalesce(p_name, ''));
    v_plant uuid;
    v_first uuid;
begin
    if v_owner is null then raise exception 'Login required' using errcode = '42501'; end if;
    if p_species not in ('tomato', 'eggplant') then
        raise exception 'Unknown plant type' using errcode = '22023';
    end if;
    if v_name = '' or length(v_name) > 40 then
        raise exception 'Plant name must be 1 to 40 characters' using errcode = '22023';
    end if;
    select id into v_first from public.plants where owner_id = v_owner order by created_at limit 1;
    insert into public.plants (owner_id, name, cultivar) values (v_owner, v_name, p_species)
    returning id into v_plant;
    insert into public.plant_settings (plant_id, moisture_min, moisture_max, temp_min, temp_max,
        humidity_min, humidity_max, light_min, light_max, pump_max_seconds, pump_cooldown_seconds, low_water_cm)
    select v_plant, moisture_min, moisture_max, temp_min, temp_max, humidity_min, humidity_max,
        light_min, light_max, pump_max_seconds, pump_cooldown_seconds, low_water_cm
    from public.plant_settings where plant_id = v_first;
    if not found then insert into public.plant_settings (plant_id) values (v_plant); end if;
    return v_plant;
end; $$;
revoke all on function public.add_plant(text, text) from public, anon;
grant execute on function public.add_plant(text, text) to authenticated;

-- ------------------------------------------------------------------ device slot 2
alter table public.devices add column plant_b_id uuid references public.plants (id) on delete set null;
-- Unassigned boards have plant_id null; only compare when slot 2 is in use.
alter table public.devices add constraint devices_two_different_plants
    check (plant_b_id is null or plant_id is null or plant_b_id <> plant_id);
create index devices_plant_b_idx on public.devices (plant_b_id);
grant select (plant_b_id) on public.devices to authenticated;

create or replace function agriquest_private.validate_device_assignment()
returns trigger language plpgsql set search_path = '' as $$
begin
    if tg_op = 'UPDATE' and new.owner_id is distinct from old.owner_id then
        raise exception 'Device ownership cannot be changed' using errcode = '42501';
    end if;
    if new.plant_id is not null and not exists (
        select 1 from public.plants where id = new.plant_id and owner_id = new.owner_id
    ) then
        raise exception 'Plant and device owner must match' using errcode = '42501';
    end if;
    if new.plant_b_id is not null and not exists (
        select 1 from public.plants where id = new.plant_b_id and owner_id = new.owner_id
    ) then
        raise exception 'Plant and device owner must match' using errcode = '42501';
    end if;
    return new;
end; $$;
drop trigger validate_device_assignment on public.devices;
create trigger validate_device_assignment before insert or update of owner_id, plant_id, plant_b_id
on public.devices for each row execute function agriquest_private.validate_device_assignment();

alter table public.actuator_commands add column slot smallint not null default 1 check (slot in (1, 2));

-- The owner links another of their plants to slot 2 of a board already paired to their account.
create function public.link_plant_to_device(p_plant_id uuid, p_device_id text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_owner uuid := auth.uid();
begin
    if v_owner is null then raise exception 'Login required' using errcode = '42501'; end if;
    perform pg_advisory_xact_lock(hashtextextended(p_device_id, 80293));
    if not exists (select 1 from public.plants where id = p_plant_id and owner_id = v_owner) then
        raise exception 'Plant not found' using errcode = '42501';
    end if;
    perform 1 from public.devices where id = p_device_id and owner_id = v_owner for update;
    if not found then raise exception 'Device not found' using errcode = '42501'; end if;
    if exists (select 1 from public.devices where id = p_device_id and plant_id = p_plant_id) then
        raise exception 'This plant already uses slot 1 of this device' using errcode = '22023';
    end if;
    -- A plant is served by one slot only.
    update public.devices set plant_b_id = null where owner_id = v_owner and plant_b_id = p_plant_id;
    update public.devices set plant_b_id = p_plant_id where id = p_device_id;
    update public.actuator_commands set status = 'expired', completed_at = clock_timestamp(),
        reason = 'Device assignment changed' where device_id = p_device_id and slot = 2
        and status in ('pending', 'running');
end; $$;
revoke all on function public.link_plant_to_device(uuid, text) from public, anon;
grant execute on function public.link_plant_to_device(uuid, text) to authenticated;

create function public.unlink_plant_b(p_device_id text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_owner uuid := auth.uid();
begin
    if v_owner is null then raise exception 'Login required' using errcode = '42501'; end if;
    perform pg_advisory_xact_lock(hashtextextended(p_device_id, 80293));
    update public.devices set plant_b_id = null where id = p_device_id and owner_id = v_owner;
    if not found then raise exception 'Device not found' using errcode = '42501'; end if;
    update public.actuator_commands set status = 'expired', completed_at = clock_timestamp(),
        reason = 'Device unassigned' where device_id = p_device_id and slot = 2 and status in ('pending', 'running');
end; $$;
revoke all on function public.unlink_plant_b(text) from public, anon;
grant execute on function public.unlink_plant_b(text) to authenticated;

-- ------------------------------------------------------------------ readings: slot aware
drop function public.submit_reading(text, text, real, real, real, real, real);
create function public.submit_reading(
    p_device_id      text,
    p_token          text,
    p_soil_moisture  real default null,
    p_air_temp       real default null,
    p_humidity       real default null,
    p_light_lux      real default null,
    p_water_level_cm real default null,
    p_slot           smallint default 1
)
returns bigint language plpgsql security definer set search_path = '' as $$
declare
    v_device public.devices := public.check_device(p_device_id, p_token);
    v_plant  uuid;
    v_id     bigint;
begin
    if p_slot = 1 then
        v_plant := v_device.plant_id;
    elsif p_slot = 2 then
        v_plant := v_device.plant_b_id;
        if v_plant is null then raise exception 'No plant is linked to slot 2' using errcode = 'P0002'; end if;
    else
        raise exception 'Unknown slot %', p_slot using errcode = '22023';
    end if;
    insert into public.sensor_readings
        (plant_id, device_id, soil_moisture, air_temp, humidity, light_lux, water_level_cm)
    values (v_plant, v_device.id, p_soil_moisture, p_air_temp, p_humidity, p_light_lux, p_water_level_cm)
    returning id into v_id;
    return v_id;
end; $$;
revoke all on function public.submit_reading(text, text, real, real, real, real, real, smallint) from public;
grant execute on function public.submit_reading(text, text, real, real, real, real, real, smallint) to anon, authenticated;

-- ------------------------------------------------------------------ commands: slot aware
create or replace function public.request_command(p_plant_id uuid, p_actuator text, p_duration_s integer)
returns public.actuator_commands language plpgsql security definer set search_path = '' as $$
declare
    v_settings public.plant_settings;
    v_device   public.devices;
    v_slot     smallint;
    v_last     timestamptz;
    v_water    real;
    v_reason   text;
    v_command  public.actuator_commands;
begin
    if not public.owns_plant(p_plant_id) then
        raise exception 'Plant not found' using errcode = '42501';
    end if;
    if p_actuator not in ('pump', 'fan', 'light') then
        raise exception 'Unknown actuator %', p_actuator using errcode = '22023';
    end if;

    select * into v_settings from public.plant_settings where plant_id = p_plant_id;
    select * into v_device from public.devices where plant_id = p_plant_id or plant_b_id = p_plant_id
        order by last_seen_at desc nulls last limit 1;
    v_slot := case when v_device.plant_b_id = p_plant_id then 2 else 1 end;

    update public.actuator_commands set status = 'expired', completed_at = now()
    where plant_id = p_plant_id and status = 'pending' and created_at < now() - interval '60 seconds';
    update public.actuator_commands set status = 'failed', completed_at = now(),
        reason = 'No result from the device.'
    where plant_id = p_plant_id and status = 'running' and picked_up_at < now() - interval '120 seconds';

    if v_device.id is null then
        v_reason := 'No sensor is paired with this plant.';
    elsif p_duration_s is null or p_duration_s < 1 then
        v_reason := 'Duration must be at least 1 second.';
    elsif p_actuator = 'pump' and p_duration_s > v_settings.pump_max_seconds then
        v_reason := format('The pump can run at most %s seconds.', v_settings.pump_max_seconds);
    elsif exists (select 1 from public.actuator_commands
                  where plant_id = p_plant_id and actuator = p_actuator and status in ('pending', 'running')) then
        v_reason := format('A %s command is already in progress.', p_actuator);
    else
        if p_actuator = 'pump' then
            select max(coalesce(completed_at, picked_up_at, created_at)) into v_last
            from public.actuator_commands
            where plant_id = p_plant_id and actuator = 'pump' and status in ('running', 'done');
            if v_last is not null
               and v_last > now() - make_interval(secs => v_settings.pump_cooldown_seconds) then
                v_reason := format('Please wait %s more seconds before watering again.',
                    ceil(extract(epoch from v_last + make_interval(secs => v_settings.pump_cooldown_seconds) - now())));
            elsif v_settings.low_water_cm is not null then
                select water_level_cm into v_water from public.sensor_readings
                where plant_id = p_plant_id and water_level_cm is not null
                order by recorded_at desc limit 1;
                if v_water is not null and v_water < v_settings.low_water_cm then
                    v_reason := 'The water reservoir is low. Refill it before watering.';
                end if;
            end if;
        end if;
    end if;

    insert into public.actuator_commands (plant_id, device_id, actuator, duration_s, status, reason, source, slot)
    values (p_plant_id, v_device.id, p_actuator, greatest(coalesce(p_duration_s, 1), 1),
            case when v_reason is null then 'pending' else 'rejected' end, v_reason, 'app', v_slot)
    returning * into v_command;
    return v_command;
end; $$;

-- Returned rows include "slot", so the firmware drives that slot's pump or lamp only.
create or replace function public.get_pending_commands(p_device_id text, p_token text)
returns setof public.actuator_commands language plpgsql security definer set search_path = '' as $$
declare v_device public.devices := public.check_device(p_device_id, p_token);
begin
    update public.actuator_commands set status = 'expired', completed_at = now()
    where device_id = v_device.id and status = 'pending' and created_at < now() - interval '60 seconds';
    update public.actuator_commands set status = 'failed', completed_at = now(), reason = 'No result from the device.'
    where device_id = v_device.id and status = 'running' and picked_up_at < now() - interval '120 seconds';
    return query update public.actuator_commands set status = 'running', picked_up_at = now()
    where device_id = v_device.id and status = 'pending'
      and ((slot = 1 and plant_id = v_device.plant_id)
        or (slot = 2 and v_device.plant_b_id is not null and plant_id = v_device.plant_b_id))
    returning *;
end; $$;

create or replace function public.report_command(p_device_id text, p_token text, p_command_id bigint,
    p_success boolean, p_message text default null)
returns void language plpgsql security definer set search_path = '' as $$
declare v_device public.devices := public.check_device(p_device_id, p_token);
begin
    update public.actuator_commands
    set status = case when p_success then 'done' else 'failed' end,
        reason = case when p_success then reason else coalesce(p_message, 'The device reported a failure.') end,
        completed_at = now()
    where id = p_command_id and device_id = v_device.id and status = 'running'
      and ((slot = 1 and plant_id = v_device.plant_id)
        or (slot = 2 and v_device.plant_b_id is not null and plant_id = v_device.plant_b_id));
    if not found then raise exception 'No running command % for this device', p_command_id using errcode = 'P0002'; end if;
end; $$;

notify pgrst, 'reload schema';
