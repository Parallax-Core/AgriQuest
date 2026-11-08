-- Share the policy for signup and recovery, keeping their counters separate.
create table if not exists agriquest_private.auth_email_limits (
    -- The signup hook can run before Auth commits its new user transaction.
    -- A foreign key here would wait on that transaction while Auth waits on us.
    user_id uuid not null,
    action text not null check (action in ('signup', 'recovery')),
    requests bigint not null default 0 check (requests >= 0),
    available_at timestamptz not null default '-infinity',
    primary key (user_id, action)
);
alter table agriquest_private.auth_email_limits enable row level security;
revoke all on agriquest_private.auth_email_limits from public, anon, authenticated;

insert into agriquest_private.auth_email_limits(user_id, action, requests, available_at)
select user_id, 'recovery', requests, available_at from agriquest_private.recovery_email_limits
on conflict (user_id, action) do nothing;

create or replace function public.reserve_auth_email(p_user_id uuid, p_action text)
returns jsonb language plpgsql security definer set search_path = pg_catalog
as $$
declare
    current_limit agriquest_private.auth_email_limits%rowtype;
    request_count bigint;
    wait_minutes bigint;
begin
    if p_action not in ('signup', 'recovery') or p_action is null then
        raise exception 'Unsupported email action';
    end if;
    insert into agriquest_private.auth_email_limits(user_id, action) values (p_user_id, p_action)
    on conflict (user_id, action) do nothing;
    select * into current_limit from agriquest_private.auth_email_limits
    where user_id = p_user_id and action = p_action for update;
    if current_limit.available_at > clock_timestamp() then
        return jsonb_build_object('allowed', false, 'retry_after_seconds',
            ceil(extract(epoch from current_limit.available_at - clock_timestamp())));
    end if;
    request_count := current_limit.requests + 1;
    wait_minutes := case when request_count <= 3 then 1 else 3 + (request_count - 4) * 2 end;
    update agriquest_private.auth_email_limits
    set requests = request_count,
        available_at = clock_timestamp() + make_interval(secs => (wait_minutes * 60)::double precision)
    where user_id = p_user_id and action = p_action;
    return jsonb_build_object('allowed', true, 'retry_after_seconds', wait_minutes * 60);
end;
$$;
revoke all on function public.reserve_auth_email(uuid, text) from public, anon, authenticated;
grant execute on function public.reserve_auth_email(uuid, text) to service_role;

-- Keep existing callers on the same recovery counter after the migration.
create or replace function public.reserve_recovery_email(p_user_id uuid)
returns jsonb language sql security definer set search_path = pg_catalog
as $$ select public.reserve_auth_email(p_user_id, 'recovery'); $$;
revoke all on function public.reserve_recovery_email(uuid) from public, anon, authenticated;
grant execute on function public.reserve_recovery_email(uuid) to service_role;
