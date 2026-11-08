create table agriquest_private.account_usernames (
    user_id uuid primary key references auth.users(id) on delete cascade,
    username text not null unique check (username = lower(btrim(username)) and length(username) between 1 and 128)
);
alter table agriquest_private.account_usernames enable row level security;
revoke all on agriquest_private.account_usernames from public, anon, authenticated;

-- Preserve saved signup usernames. Older accounts use their existing display name.
insert into agriquest_private.account_usernames(user_id,username)
select u.id, lower(btrim(coalesce(nullif(btrim(u.raw_user_meta_data->>'username'),''),
    nullif(btrim(p.display_name),''),split_part(u.email,'@',1))))
from auth.users u left join public.profiles p on p.id=u.id
where u.email is not null;

-- Separate trigger: do not replace the existing profile/plant creation trigger.
create function agriquest_private.register_account_username()
returns trigger language plpgsql security definer set search_path = '' as $$
declare v_username text := lower(btrim(new.raw_user_meta_data->>'username'));
begin
    if v_username is null or v_username = '' then return new; end if;
    if length(v_username) > 128 then raise exception 'Invalid username' using errcode='22023'; end if;
    insert into agriquest_private.account_usernames(user_id,username) values (new.id,v_username);
    return new;
end; $$;
revoke all on function agriquest_private.register_account_username() from public, anon, authenticated;
create trigger register_account_username after insert on auth.users
for each row execute function agriquest_private.register_account_username();

-- Never grant anonymous clients a username-to-email lookup.
create function public.resolve_login_username(p_username text)
returns text language sql stable security definer set search_path = '' as $$
    select u.email from agriquest_private.account_usernames n
    join auth.users u on u.id=n.user_id
    where n.username=lower(btrim(p_username));
$$;
revoke all on function public.resolve_login_username(text) from public, anon, authenticated;
grant execute on function public.resolve_login_username(text) to service_role;
notify pgrst, 'reload schema';
