create table public.app_releases (
    version_code integer primary key check (version_code > 1),
    version_name text not null check (length(version_name) between 1 and 64),
    apk_path text not null unique,
    sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
    size_bytes bigint not null check (size_bytes between 1 and 52428800),
    commit_sha text not null check (commit_sha ~ '^[0-9a-f]{40}$'),
    created_at timestamptz not null default now(),
    check (apk_path = 'releases/' || version_code::text || '/AgriQuest.apk')
);
alter table public.app_releases enable row level security;
revoke all on public.app_releases from public, anon, authenticated;
grant select on public.app_releases to anon, authenticated;
grant all on public.app_releases to service_role;
create policy "Public app release information" on public.app_releases
for select to anon, authenticated using (true);

-- APKs are public downloads; only trusted publishing credentials may write them.
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('app-updates','app-updates',true,52428800,array['application/vnd.android.package-archive']);
notify pgrst, 'reload schema';
