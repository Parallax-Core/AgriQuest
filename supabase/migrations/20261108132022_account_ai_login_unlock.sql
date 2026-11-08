-- Login days are independent of editable plant-care streaks.
create table agriquest_private.account_login_days (
    user_id uuid not null references auth.users(id) on delete cascade,
    login_date date not null,
    primary key (user_id, login_date)
);
alter table agriquest_private.account_login_days enable row level security;
revoke all on agriquest_private.account_login_days from public, anon, authenticated;

create function public.ai_login_days()
returns integer language plpgsql security definer set search_path = '' as $$
declare v_user uuid := auth.uid(); v_days integer;
begin
    if v_user is null then raise exception 'Login required' using errcode = '42501'; end if;
    select least(count(*),7)::integer into v_days from agriquest_private.account_login_days where user_id = v_user;
    return v_days;
end; $$;
revoke all on function public.ai_login_days() from public, anon;
grant execute on function public.ai_login_days() to authenticated;

create function public.record_account_login()
returns integer language plpgsql security definer set search_path = '' as $$
declare v_user uuid := auth.uid();
begin
    if v_user is null then raise exception 'Login required' using errcode = '42501'; end if;
    -- One distinct server calendar day (Philippine/Singapore time) per account.
    insert into agriquest_private.account_login_days(user_id,login_date)
    values (v_user,(clock_timestamp() at time zone 'Asia/Manila')::date)
    on conflict (user_id,login_date) do nothing;
    return public.ai_login_days();
end; $$;
revoke all on function public.record_account_login() from public, anon;
grant execute on function public.record_account_login() to authenticated;
notify pgrst, 'reload schema';
