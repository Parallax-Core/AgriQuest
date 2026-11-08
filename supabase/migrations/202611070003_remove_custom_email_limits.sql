-- Disable custom cooldowns for existing hook deployments too.
-- Keep existing tables intact; their counters no longer affect requests.
create or replace function public.reserve_auth_email(p_user_id uuid, p_action text)
returns jsonb language sql security definer set search_path = pg_catalog
as $$ select jsonb_build_object('allowed', true, 'retry_after_seconds', 0); $$;
revoke all on function public.reserve_auth_email(uuid, text) from public, anon, authenticated;
grant execute on function public.reserve_auth_email(uuid, text) to service_role;

create or replace function public.reserve_recovery_email(p_user_id uuid)
returns jsonb language sql security definer set search_path = pg_catalog
as $$ select jsonb_build_object('allowed', true, 'retry_after_seconds', 0); $$;
revoke all on function public.reserve_recovery_email(uuid) from public, anon, authenticated;
grant execute on function public.reserve_recovery_email(uuid) to service_role;
