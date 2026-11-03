-- =====================================================================
-- AgriQuest database schema for Supabase (PostgreSQL)
--
-- Paste this whole file into Supabase > SQL Editor > New query > Run.
-- Safe to run on a new, empty project. It creates:
--   * tables for the six data stores in the paper (Figure 3.10, D1-D6) + devices
--   * Row Level Security on every table (each player sees only their own data)
--   * a trigger that gives every new account a profile and a first plant
--   * functions the app calls (pair a device, request a pump/fan/light command)
--   * functions the ESP32 calls with its device token (send readings,
--     get pending commands, report results) - the ESP32 never holds an admin key
--   * a trigger that turns new readings into care suggestions (process 3.0)
-- =====================================================================


-- ---------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------

-- D6: one row per player (same id as the login account)
create table public.profiles (
    id                  uuid primary key references auth.users (id) on delete cascade,
    display_name        text,
    exp                 integer not null default 0 check (exp >= 0),
    level               integer not null default 0 check (level >= 0),
    streak_days         integer not null default 0 check (streak_days >= 0),
    last_reset_date     date,
    quiz_completed_date date,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

-- D1: the player's plant(s). The app shows one plant per account for now.
create table public.plants (
    id           uuid primary key default gen_random_uuid(),
    owner_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    name         text not null default 'Flora',
    cultivar     text,
    sowing_date  date not null default current_date,
    growth_stage text,
    created_at   timestamptz not null default now()
);
create index plants_owner_idx on public.plants (owner_id);

-- D1: configured care ranges and actuator safety limits, one row per plant.
-- The default ranges are PLACEHOLDERS: replace them with the values from your references.
create table public.plant_settings (
    plant_id              uuid primary key references public.plants (id) on delete cascade,
    moisture_min          real not null default 45,     -- %  (below this the app alerts and enables Water Plant)
    moisture_max          real not null default 80,     -- %
    temp_min              real not null default 18,     -- deg C
    temp_max              real not null default 30,     -- deg C
    humidity_min          real not null default 50,     -- %
    humidity_max          real not null default 80,     -- %
    light_min             real not null default 5000,   -- lux
    light_max             real not null default 40000,  -- lux
    pump_max_seconds      integer not null default 5  check (pump_max_seconds between 1 and 30),
    pump_cooldown_seconds integer not null default 60 check (pump_cooldown_seconds >= 0),
    low_water_cm          real,                          -- null = no water-level sensor yet
    updated_at            timestamptz not null default now(),
    check (moisture_min < moisture_max and temp_min < temp_max
           and humidity_min < humidity_max and light_min < light_max)
);

-- The ESP32 unit(s). id = the name it shows over Bluetooth, e.g. 'AgriQuest-7F3A'.
create table public.devices (
    id           text primary key,
    plant_id     uuid not null references public.plants (id) on delete cascade,
    owner_id     uuid not null references auth.users (id) on delete cascade,
    token_hash   text not null,             -- sha256 of the device token; the token itself is never stored
    created_at   timestamptz not null default now(),
    last_seen_at timestamptz
);
create index devices_plant_idx on public.devices (plant_id);

-- D2: sensor readings
create table public.sensor_readings (
    id             bigint generated always as identity primary key,
    plant_id       uuid not null references public.plants (id) on delete cascade,
    device_id      text references public.devices (id) on delete set null,
    soil_moisture  real,   -- %
    air_temp       real,   -- deg C
    humidity       real,   -- %
    light_lux      real,   -- lux
    water_level_cm real,   -- cm of water in the reservoir
    recorded_at    timestamptz not null default now()
);
create index sensor_readings_plant_time_idx on public.sensor_readings (plant_id, recorded_at desc);

-- D3: actuator commands and their results (rejected ones are kept as a log)
create table public.actuator_commands (
    id           bigint generated always as identity primary key,
    plant_id     uuid not null references public.plants (id) on delete cascade,
    device_id    text references public.devices (id) on delete set null,
    actuator     text not null check (actuator in ('pump', 'fan', 'light')),
    duration_s   integer not null check (duration_s > 0),
    status       text not null default 'pending'
                 check (status in ('pending', 'rejected', 'running', 'done', 'failed', 'expired')),
    reason       text,                       -- why it was rejected or failed
    source       text not null default 'app' check (source in ('app', 'auto')),
    requested_by uuid default auth.uid() references auth.users (id) on delete set null,
    created_at   timestamptz not null default now(),
    picked_up_at timestamptz,
    completed_at timestamptz
);
create index actuator_commands_plant_time_idx on public.actuator_commands (plant_id, created_at desc);
create index actuator_commands_pending_idx on public.actuator_commands (device_id) where status = 'pending';

-- D4: care suggestions and alerts (written by the database, see evaluate_reading below)
create table public.care_suggestions (
    id         bigint generated always as identity primary key,
    plant_id   uuid not null references public.plants (id) on delete cascade,
    metric     text not null check (metric in ('soil_moisture', 'air_temp', 'humidity', 'light_lux')),
    status     text not null check (status in ('ok', 'approaching', 'outside')),
    value      real,
    message    text not null,
    reading_id bigint references public.sensor_readings (id) on delete set null,
    created_at timestamptz not null default now()
);
create index care_suggestions_plant_time_idx on public.care_suggestions (plant_id, created_at desc);

-- D5: leaf scans (later written by the assess-leaf Edge Function)
create table public.leaf_scans (
    id             uuid primary key default gen_random_uuid(),
    plant_id       uuid not null references public.plants (id) on delete cascade,
    owner_id       uuid not null default auth.uid() references auth.users (id) on delete cascade,
    image_path     text,                     -- path in the leaf-photos storage bucket
    label          text not null,            -- e.g. 'Early Blight'
    class_id       text,
    confidence     real check (confidence between 0 and 1),
    is_healthy     boolean not null,
    low_confidence boolean not null default false,
    care_guidance  text,
    created_at     timestamptz not null default now()
);
create index leaf_scans_plant_time_idx on public.leaf_scans (plant_id, created_at desc);

-- D6: daily quest completions (quest definitions stay in the app's code)
create table public.quest_progress (
    id          bigint generated always as identity primary key,
    user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
    quest_id    text not null,
    quest_date  date not null default current_date,
    exp_awarded integer not null default 0 check (exp_awarded >= 0),
    claimed_at  timestamptz not null default now(),
    unique (user_id, quest_id, quest_date)
);

-- D6: plant journal
create table public.journal_entries (
    id         bigint generated always as identity primary key,
    plant_id   uuid not null references public.plants (id) on delete cascade,
    owner_id   uuid not null default auth.uid() references auth.users (id) on delete cascade,
    note       text not null check (length(note) between 1 and 2000),
    category   text not null default 'GENERAL',
    created_at timestamptz not null default now()
);
create index journal_entries_plant_time_idx on public.journal_entries (plant_id, created_at desc);


-- ---------------------------------------------------------------------
-- Row Level Security: a player can only reach rows of their own plants
-- ---------------------------------------------------------------------

alter table public.profiles          enable row level security;
alter table public.plants            enable row level security;
alter table public.plant_settings    enable row level security;
alter table public.devices           enable row level security;
alter table public.sensor_readings   enable row level security;
alter table public.actuator_commands enable row level security;
alter table public.care_suggestions  enable row level security;
alter table public.leaf_scans        enable row level security;
alter table public.quest_progress    enable row level security;
alter table public.journal_entries   enable row level security;

-- true when the signed-in player owns the plant
create function public.owns_plant(p_plant_id uuid)
returns boolean
language sql stable security definer set search_path = ''
as $$
    select exists (select 1 from public.plants
                   where id = p_plant_id and owner_id = (select auth.uid()));
$$;

create policy "own profile: read"   on public.profiles for select to authenticated using (id = (select auth.uid()));
create policy "own profile: update" on public.profiles for update to authenticated
    using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy "own plants" on public.plants for all to authenticated
    using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));

create policy "own plant settings" on public.plant_settings for all to authenticated
    using (public.owns_plant(plant_id)) with check (public.owns_plant(plant_id));

-- devices are added only through pair_device(); players can see and unpair (delete) their own
create policy "own devices: read"   on public.devices for select to authenticated using (owner_id = (select auth.uid()));
create policy "own devices: unpair" on public.devices for delete to authenticated using (owner_id = (select auth.uid()));

-- readings come only from submit_reading(); players can read them
create policy "own readings: read" on public.sensor_readings for select to authenticated
    using (public.owns_plant(plant_id));

-- commands are created only through request_command(); players can read them
create policy "own commands: read" on public.actuator_commands for select to authenticated
    using (public.owns_plant(plant_id));

create policy "own suggestions: read" on public.care_suggestions for select to authenticated
    using (public.owns_plant(plant_id));

-- until the Edge Function exists, the app may save its own scans
create policy "own scans: read"   on public.leaf_scans for select to authenticated using (owner_id = (select auth.uid()));
create policy "own scans: insert" on public.leaf_scans for insert to authenticated
    with check (owner_id = (select auth.uid()) and public.owns_plant(plant_id));

create policy "own quest progress" on public.quest_progress for all to authenticated
    using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy "own journal" on public.journal_entries for all to authenticated
    using (owner_id = (select auth.uid()))
    with check (owner_id = (select auth.uid()) and public.owns_plant(plant_id));


-- ---------------------------------------------------------------------
-- New account: create the profile, a first plant and its settings
-- ---------------------------------------------------------------------

create function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    v_plant_id uuid;
begin
    insert into public.profiles (id, display_name)
    values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1)));

    insert into public.plants (owner_id, name)
    values (new.id, 'Flora')
    returning id into v_plant_id;

    insert into public.plant_settings (plant_id) values (v_plant_id);
    return new;
end;
$$;

create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();


-- ---------------------------------------------------------------------
-- Device tokens
-- ---------------------------------------------------------------------

create function public.hash_token(p_token text)
returns text
language sql immutable set search_path = ''
as $$ select encode(sha256(convert_to(p_token, 'UTF8')), 'hex'); $$;

-- returns the device id when the token is right, otherwise raises an error
create function public.check_device(p_device_id text, p_token text)
returns public.devices
language plpgsql security definer set search_path = ''
as $$
declare
    v_device public.devices;
begin
    select * into v_device from public.devices
    where id = p_device_id and token_hash = public.hash_token(p_token);
    if not found then
        raise exception 'Unknown device or wrong token' using errcode = '28000';
    end if;
    update public.devices set last_seen_at = now() where id = p_device_id;
    return v_device;
end;
$$;


-- ---------------------------------------------------------------------
-- Called by the APP (signed-in player)
-- ---------------------------------------------------------------------

-- Pairing: links an ESP32 to one of the player's plants and returns a new device token.
-- The app sends this token to the ESP32 over Bluetooth. Pairing again replaces the old token.
create function public.pair_device(p_plant_id uuid, p_device_id text)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
    v_token text := replace(gen_random_uuid()::text || gen_random_uuid()::text, '-', '');
    v_owner uuid;
begin
    if not public.owns_plant(p_plant_id) then
        raise exception 'Plant not found' using errcode = '42501';
    end if;
    if p_device_id !~ '^[A-Za-z0-9_-]{3,40}$' then
        raise exception 'Invalid device id' using errcode = '22023';
    end if;

    select owner_id into v_owner from public.devices where id = p_device_id;
    if found and v_owner <> auth.uid() then
        raise exception 'This device is paired to another account' using errcode = '42501';
    end if;

    insert into public.devices (id, plant_id, owner_id, token_hash)
    values (p_device_id, p_plant_id, auth.uid(), public.hash_token(v_token))
    on conflict (id) do update
        set plant_id = excluded.plant_id, token_hash = excluded.token_hash, last_seen_at = null;

    return v_token;
end;
$$;

-- Paper process 4.0: validate an actuator command before the ESP32 may run it.
-- Always returns the saved command; if it breaks a safety rule, status = 'rejected' and reason says why.
create function public.request_command(p_plant_id uuid, p_actuator text, p_duration_s integer)
returns public.actuator_commands
language plpgsql security definer set search_path = ''
as $$
declare
    v_settings public.plant_settings;
    v_device   public.devices;
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
    select * into v_device from public.devices where plant_id = p_plant_id
        order by last_seen_at desc nulls last limit 1;

    -- expire commands the device never picked up, and fail ones it never reported back
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

    insert into public.actuator_commands (plant_id, device_id, actuator, duration_s, status, reason, source)
    values (p_plant_id, v_device.id, p_actuator, greatest(coalesce(p_duration_s, 1), 1),
            case when v_reason is null then 'pending' else 'rejected' end, v_reason, 'app')
    returning * into v_command;
    return v_command;
end;
$$;


-- ---------------------------------------------------------------------
-- Called by the ESP32 (or the virtual ESP32) with its device token
-- ---------------------------------------------------------------------

create function public.submit_reading(
    p_device_id      text,
    p_token          text,
    p_soil_moisture  real default null,
    p_air_temp       real default null,
    p_humidity       real default null,
    p_light_lux      real default null,
    p_water_level_cm real default null
)
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
    v_device public.devices := public.check_device(p_device_id, p_token);
    v_id     bigint;
begin
    insert into public.sensor_readings
        (plant_id, device_id, soil_moisture, air_temp, humidity, light_lux, water_level_cm)
    values (v_device.plant_id, v_device.id, p_soil_moisture, p_air_temp, p_humidity, p_light_lux, p_water_level_cm)
    returning id into v_id;
    return v_id;
end;
$$;

-- The ESP32 calls this every 5 seconds. Returned commands are marked 'running'.
create function public.get_pending_commands(p_device_id text, p_token text)
returns setof public.actuator_commands
language plpgsql security definer set search_path = ''
as $$
declare
    v_device public.devices := public.check_device(p_device_id, p_token);
begin
    update public.actuator_commands set status = 'expired', completed_at = now()
    where device_id = v_device.id and status = 'pending' and created_at < now() - interval '60 seconds';
    update public.actuator_commands set status = 'failed', completed_at = now(),
        reason = 'No result from the device.'
    where device_id = v_device.id and status = 'running' and picked_up_at < now() - interval '120 seconds';

    return query
    update public.actuator_commands set status = 'running', picked_up_at = now()
    where device_id = v_device.id and status = 'pending'
    returning *;
end;
$$;

-- The ESP32 reports how a command went.
create function public.report_command(
    p_device_id  text,
    p_token      text,
    p_command_id bigint,
    p_success    boolean,
    p_message    text default null
)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
    v_device public.devices := public.check_device(p_device_id, p_token);
begin
    update public.actuator_commands
    set status = case when p_success then 'done' else 'failed' end,
        reason = case when p_success then reason else coalesce(p_message, 'The device reported a failure.') end,
        completed_at = now()
    where id = p_command_id and device_id = v_device.id and status = 'running';
    if not found then
        raise exception 'No running command % for this device', p_command_id using errcode = 'P0002';
    end if;
end;
$$;


-- ---------------------------------------------------------------------
-- Paper process 3.0: turn each new reading into care suggestions.
-- A suggestion is saved only when a metric's status changes, so the table
-- holds alerts, not one row per reading. (Trend-based "time to limit" comes later.)
-- ---------------------------------------------------------------------

create function public.evaluate_reading()
returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
    s       public.plant_settings;
    m       record;
    v_band  real;
    v_state text;
    v_msg   text;
    v_prev  text;
begin
    select * into s from public.plant_settings where plant_id = new.plant_id;
    if not found then return new; end if;

    for m in
        select * from (values
            ('soil_moisture', new.soil_moisture, s.moisture_min, s.moisture_max, '%',
             'Soil is getting dry. Water the plant soon.', 'Soil is too dry. Water the plant now.',
             'Soil is getting wet. Hold off on watering.', 'Soil is too wet. Stop watering and let it drain.'),
            ('air_temp', new.air_temp, s.temp_min, s.temp_max, ' C',
             'It is getting cold for the plant.', 'Too cold for the plant. Move it somewhere warmer.',
             'It is getting hot. Consider ventilating.', 'Too hot for the plant. Turn on the fan or add shade.'),
            ('humidity', new.humidity, s.humidity_min, s.humidity_max, '%',
             'Air is getting dry.', 'Air is too dry for the plant.',
             'Air is getting humid. Improve airflow.', 'Air is too humid. Ventilate to prevent disease.'),
            ('light_lux', new.light_lux, s.light_min, s.light_max, ' lux',
             'Light is getting low.', 'Too little light. Turn on the grow light.',
             'Light is getting strong.', 'Too much light. Add shade.')
        ) as t(metric, val, lo, hi, unit, near_low, out_low, near_high, out_high)
    loop
        continue when m.val is null;
        v_band := (m.hi - m.lo) * 0.10;   -- "approaching" = within 10% of the range from an edge
        if m.val < m.lo then
            v_state := 'outside';     v_msg := m.out_low;
        elsif m.val > m.hi then
            v_state := 'outside';     v_msg := m.out_high;
        elsif m.val < m.lo + v_band then
            v_state := 'approaching'; v_msg := m.near_low;
        elsif m.val > m.hi - v_band then
            v_state := 'approaching'; v_msg := m.near_high;
        else
            v_state := 'ok';          v_msg := 'Within range. Keep the current care routine.';
        end if;

        select status into v_prev from public.care_suggestions
        where plant_id = new.plant_id and metric = m.metric
        order by created_at desc, id desc limit 1;

        if v_prev is distinct from v_state then
            insert into public.care_suggestions (plant_id, metric, status, value, message, reading_id)
            values (new.plant_id, m.metric, v_state, m.val,
                    v_msg || format(' (%s%s)', round(m.val::numeric, 1), m.unit), new.id);
        end if;
    end loop;
    return new;
end;
$$;

create trigger on_sensor_reading
    after insert on public.sensor_readings
    for each row execute function public.evaluate_reading();


-- ---------------------------------------------------------------------
-- Who may call which function
-- ---------------------------------------------------------------------

revoke execute on all functions in schema public from public, anon, authenticated;

grant execute on function public.owns_plant(uuid)                           to authenticated;
grant execute on function public.pair_device(uuid, text)                    to authenticated;
grant execute on function public.request_command(uuid, text, integer)       to authenticated;

-- the ESP32 uses the public (anon) key plus its own device token
grant execute on function public.submit_reading(text, text, real, real, real, real, real) to anon, authenticated;
grant execute on function public.get_pending_commands(text, text)                         to anon, authenticated;
grant execute on function public.report_command(text, text, bigint, boolean, text)        to anon, authenticated;


-- ---------------------------------------------------------------------
-- Live updates in the app (Supabase Realtime) for readings, commands and alerts
-- ---------------------------------------------------------------------

do $$
begin
    if exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
        alter publication supabase_realtime
            add table public.sensor_readings, public.actuator_commands, public.care_suggestions;
    end if;
end;
$$;
