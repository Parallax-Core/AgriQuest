-- Only the signed Auth email hook (using service_role) can reserve email sends.
create schema if not exists agriquest_private;
revoke all on schema agriquest_private from public, anon, authenticated;

create table if not exists agriquest_private.recovery_email_limits (
    user_id uuid primary key references auth.users(id) on delete cascade,
    requests bigint not null default 0 check (requests >= 0),
    available_at timestamptz not null default '-infinity'
);
alter table agriquest_private.recovery_email_limits enable row level security;
revoke all on agriquest_private.recovery_email_limits from public, anon, authenticated;

create or replace function public.reserve_recovery_email(p_user_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog
as $$
declare
    current_limit agriquest_private.recovery_email_limits%rowtype;
    request_count bigint;
    wait_minutes bigint;
begin
    insert into agriquest_private.recovery_email_limits(user_id) values (p_user_id)
    on conflict (user_id) do nothing;
    select * into current_limit from agriquest_private.recovery_email_limits
    where user_id = p_user_id for update;
    if current_limit.available_at > clock_timestamp() then
        return jsonb_build_object('allowed', false, 'retry_after_seconds',
            ceil(extract(epoch from current_limit.available_at - clock_timestamp())));
    end if;
    request_count := current_limit.requests + 1;
    wait_minutes := case when request_count <= 3 then 1 else 3 + (request_count - 4) * 2 end;
    update agriquest_private.recovery_email_limits
    set requests = request_count,
        available_at = clock_timestamp() + make_interval(secs => (wait_minutes * 60)::double precision)
    where user_id = p_user_id;
    return jsonb_build_object('allowed', true, 'retry_after_seconds', wait_minutes * 60);
end;
$$;
revoke all on function public.reserve_recovery_email(uuid) from public, anon, authenticated;
grant execute on function public.reserve_recovery_email(uuid) to service_role;
