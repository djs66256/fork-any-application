-- backfill_auth_profiles_trigger: attach auth.users -> profiles trigger and backfill missing profiles

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_auth_user_created();

INSERT INTO public.profiles (id, email, role)
SELECT
  users.id,
  users.email,
  CASE
    WHEN users.raw_app_meta_data ->> 'role' IN ('admin', 'editor', 'viewer')
      THEN (users.raw_app_meta_data ->> 'role')::user_role
    ELSE 'viewer'::user_role
  END AS role
FROM auth.users AS users
LEFT JOIN public.profiles AS profiles
  ON profiles.id = users.id
WHERE profiles.id IS NULL;
