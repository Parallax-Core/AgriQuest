-- Lets the signed auth-email hook (service role only) put the player's login username
-- in password reset emails. Never exposed to anonymous or signed-in clients.
create function public.account_login_username(p_user_id uuid)
returns text language sql stable security definer set search_path = '' as $$
    select n.username from agriquest_private.account_usernames n where n.user_id = p_user_id;
$$;
revoke all on function public.account_login_username(uuid) from public, anon, authenticated;
grant execute on function public.account_login_username(uuid) to service_role;
notify pgrst, 'reload schema';
