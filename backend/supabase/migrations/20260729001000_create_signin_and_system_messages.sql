CREATE TABLE IF NOT EXISTS public.check_in_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  subject_type TEXT NOT NULL CHECK (subject_type IN ('user', 'installation')),
  subject_id TEXT NOT NULL,
  business_date DATE NOT NULL,
  streak_day INTEGER NOT NULL CHECK (streak_day BETWEEN 1 AND 7),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_check_in_records_subject_date
  ON public.check_in_records(subject_type, subject_id, business_date);

CREATE INDEX IF NOT EXISTS idx_check_in_records_subject_created_at
  ON public.check_in_records(subject_type, subject_id, created_at DESC);

ALTER TABLE public.check_in_records ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "service_role_manage_check_in_records" ON public.check_in_records;
CREATE POLICY "service_role_manage_check_in_records" ON public.check_in_records
  FOR ALL TO service_role
  USING (true)
  WITH CHECK (true);

CREATE TABLE IF NOT EXISTS public.system_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title TEXT NOT NULL,
  summary TEXT NOT NULL,
  sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_system_messages_sent_at
  ON public.system_messages(sent_at DESC);

ALTER TABLE public.system_messages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "service_role_manage_system_messages" ON public.system_messages;
CREATE POLICY "service_role_manage_system_messages" ON public.system_messages
  FOR ALL TO service_role
  USING (true)
  WITH CHECK (true);
