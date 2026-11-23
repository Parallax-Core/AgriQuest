-- Lets the sign-up screen check a username before creating the account, so a taken
-- username gets a clear message. The unique rule on account_usernames still blocks duplicates.
create or replace function public.is_username_available(p_username text)
returns boolean language sql stable security definer set search_path = ''
as $$
    select coalesce(btrim(p_username), '') <> ''
       and not exists (select 1 from agriquest_private.account_usernames
                       where username = lower(btrim(p_username)));
$$;
revoke all on function public.is_username_available(text) from public;
grant execute on function public.is_username_available(text) to anon, authenticated;
