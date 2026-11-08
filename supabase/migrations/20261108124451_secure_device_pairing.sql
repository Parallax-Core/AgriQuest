-- Reuse devices/plants/sensor_readings. This private table is a credential/proof
-- registry for pre-provisioned hardware, not a second ownership table.
create table agriquest_private.device_pairing (
    device_id text primary key check (device_id ~ '^[A-Za-z0-9_-]{3,40}$'),
    token_hash text not null check (token_hash ~ '^[0-9a-f]{64}$'),
    pairing_code_hash text check (pairing_code_hash ~ '^[0-9a-f]{64}$'),
    expires_at timestamptz not null default '-infinity'
);
alter table agriquest_private.device_pairing enable row level security;
revoke all on agriquest_private.device_pairing from public, anon, authenticated;

-- Keep the registered owner and original device identifiers when unassigning.
alter table public.devices alter column plant_id drop not null;
alter table public.devices drop constraint devices_plant_id_fkey;
alter table public.devices add constraint devices_plant_id_fkey
    foreign key (plant_id) references public.plants(id) on delete set null;
revoke all on public.devices from anon, authenticated;
grant select (id, plant_id, owner_id, created_at, last_seen_at) on public.devices to authenticated;
revoke insert, update, delete, truncate, references, trigger on public.sensor_readings from anon, authenticated;

-- Public identifiers alone must no longer reach the legacy registration path.
-- Retain its implementation/signature for trusted backend compatibility.
revoke execute on function public.pair_device(uuid, text) from public, anon, authenticated;

create function agriquest_private.validate_device_assignment()
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
    return new;
end; $$;
revoke all on function agriquest_private.validate_device_assignment() from public, anon, authenticated;
create trigger validate_device_assignment before insert or update of owner_id, plant_id
on public.devices for each row execute function agriquest_private.validate_device_assignment();

-- Only firmware possessing its provisioned secret may publish a proof window.
-- Firmware MUST call this only after a physical button action, then expose the
-- random 256-bit code only on an authenticated/MITM-protected BLE connection.
create function public.open_device_pairing(p_device_id text, p_token text, p_pairing_code_hash text)
returns timestamptz language plpgsql security definer set search_path = '' as $$
declare
    v_device public.devices;
    v_registry agriquest_private.device_pairing;
    v_expiry timestamptz := clock_timestamp() + interval '2 minutes';
begin
    if p_device_id is null or p_device_id !~ '^[A-Za-z0-9_-]{3,40}$'
       or p_token is null or length(p_token) < 32
       or p_pairing_code_hash is null or p_pairing_code_hash !~ '^[0-9a-f]{64}$' then
        raise exception 'Invalid device proof' using errcode = '28000';
    end if;
    perform pg_advisory_xact_lock(hashtextextended(p_device_id, 80293));
    select * into v_registry from agriquest_private.device_pairing where device_id = p_device_id for update;
    select * into v_device from public.devices where id = p_device_id for update;
    if v_device.id is not null then
        if public.hash_token(p_token) is distinct from v_device.token_hash then
            raise exception 'Invalid device proof' using errcode = '28000';
        end if;
        -- Existing deployed devices keep their existing credential unchanged.
        insert into agriquest_private.device_pairing(device_id, token_hash)
        values (p_device_id, v_device.token_hash)
        on conflict (device_id) do update set token_hash = excluded.token_hash;
    elsif v_registry.device_id is null or public.hash_token(p_token) is distinct from v_registry.token_hash then
        raise exception 'Invalid device proof' using errcode = '28000';
    end if;
    update agriquest_private.device_pairing set pairing_code_hash = p_pairing_code_hash,
        expires_at = v_expiry where device_id = p_device_id;
    return v_expiry;
end; $$;
revoke all on function public.open_device_pairing(text,text,text) from public;
grant execute on function public.open_device_pairing(text,text,text) to anon, authenticated, service_role;

-- Authenticated identity is always auth.uid(), never a client-supplied user ID.
create function public.pair_device(p_plant_id uuid, p_device_id text, p_pairing_code text)
returns text language plpgsql security definer set search_path = '' as $$
declare
    v_owner uuid := auth.uid();
    v_device public.devices;
    v_registry agriquest_private.device_pairing;
    v_plant uuid;
begin
    if v_owner is null then raise exception 'Login required' using errcode = '42501'; end if;
    if p_device_id is null or p_device_id !~ '^[A-Za-z0-9_-]{3,40}$'
       or p_pairing_code is null or p_pairing_code !~ '^[0-9a-f]{64}$' then
        raise exception 'Physical pairing proof required' using errcode = '28000';
    end if;
    perform pg_advisory_xact_lock(hashtextextended(p_device_id, 80293));
    select id into v_plant from public.plants where id = p_plant_id and owner_id = v_owner for share;
    if v_plant is null then raise exception 'Plant not found' using errcode = '42501'; end if;
    select * into v_registry from agriquest_private.device_pairing where device_id = p_device_id for update;
    select * into v_device from public.devices where id = p_device_id for update;
    if v_device.id is not null and v_device.owner_id is distinct from v_owner then
        raise exception 'This device is paired to another account' using errcode = '42501';
    end if;
    if v_registry.device_id is null or v_registry.expires_at <= clock_timestamp()
       or public.hash_token(p_pairing_code) is distinct from v_registry.pairing_code_hash then
        raise exception 'Pairing proof is invalid, expired, or already used' using errcode = '28000';
    end if;
    if v_device.id is null then
        insert into public.devices(id, plant_id, owner_id, token_hash)
        values (p_device_id, p_plant_id, v_owner, v_registry.token_hash);
    else
        update public.devices set plant_id = p_plant_id where id = p_device_id;
    end if;
    update agriquest_private.device_pairing set pairing_code_hash = null, expires_at = '-infinity'
    where device_id = p_device_id;
    -- Commands for the original assignment must never run on the new plant.
    update public.actuator_commands set status = 'expired', completed_at = clock_timestamp(),
        reason = 'Device assignment changed' where device_id = p_device_id
        and plant_id is distinct from p_plant_id and status in ('pending','running');
    return p_device_id; -- Never return the device's long-lived credential to Android.
end; $$;
revoke all on function public.pair_device(uuid,text,text) from public, anon;
grant execute on function public.pair_device(uuid,text,text) to authenticated;

create function public.unpair_device(p_device_id text)
returns void language plpgsql security definer set search_path = '' as $$
declare v_owner uuid := auth.uid();
begin
    if v_owner is null then raise exception 'Login required' using errcode = '42501'; end if;
    perform pg_advisory_xact_lock(hashtextextended(p_device_id, 80293));
    perform 1 from public.devices where id = p_device_id and owner_id = v_owner for update;
    if not found then raise exception 'Device not found' using errcode = '42501'; end if;
    update public.devices set plant_id = null where id = p_device_id;
    update agriquest_private.device_pairing set pairing_code_hash = null, expires_at = '-infinity'
    where device_id = p_device_id;
    update public.actuator_commands set status = 'expired', completed_at = clock_timestamp(),
        reason = 'Device unassigned' where device_id = p_device_id and status in ('pending','running');
end; $$;
revoke all on function public.unpair_device(text) from public, anon;
grant execute on function public.unpair_device(text) to authenticated;

-- All existing telemetry/command RPCs already call this credential verifier.
-- Lock the assignment until the enclosing reading/command transaction commits.
create or replace function public.check_device(p_device_id text, p_token text)
returns public.devices language plpgsql security definer set search_path = '' as $$
declare v_device public.devices;
begin
    select * into v_device from public.devices
    where id = p_device_id and token_hash = public.hash_token(p_token) for update;
    if not found then raise exception 'Unknown device or wrong token' using errcode = '28000'; end if;
    if v_device.plant_id is null or not exists (
        select 1 from public.plants where id = v_device.plant_id and owner_id = v_device.owner_id
    ) then raise exception 'Device is unassigned or unauthorized' using errcode = '42501'; end if;
    update public.devices set last_seen_at = now() where id = p_device_id;
    return v_device;
end; $$;
revoke all on function public.check_device(text,text) from public, anon, authenticated;
grant execute on function public.check_device(text,text) to service_role;

-- Preserve the collaborator's command behavior; narrow only the assignment filter.
create or replace function public.get_pending_commands(p_device_id text, p_token text)
returns setof public.actuator_commands language plpgsql security definer set search_path = '' as $$
declare v_device public.devices := public.check_device(p_device_id, p_token);
begin
    update public.actuator_commands set status = 'expired', completed_at = now()
    where device_id = v_device.id and status = 'pending' and created_at < now() - interval '60 seconds';
    update public.actuator_commands set status = 'failed', completed_at = now(), reason = 'No result from the device.'
    where device_id = v_device.id and status = 'running' and picked_up_at < now() - interval '120 seconds';
    return query update public.actuator_commands set status = 'running', picked_up_at = now()
    where device_id = v_device.id and plant_id = v_device.plant_id and status = 'pending' returning *;
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
    where id = p_command_id and device_id = v_device.id and plant_id = v_device.plant_id and status = 'running';
    if not found then raise exception 'No running command % for this device', p_command_id using errcode = 'P0002'; end if;
end; $$;

notify pgrst, 'reload schema';
