-- AgriQuest database update (Nov 3): "too dry" threshold 40% -> 45%, to match the app.
-- Run once in Supabase > SQL Editor. Safe to run more than once.

alter table public.plant_settings alter column moisture_min set default 45;

-- existing plants that still have the old default
update public.plant_settings set moisture_min = 45, updated_at = now() where moisture_min = 40;
