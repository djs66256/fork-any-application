-- add_comments_tables: Create comments and comment_likes tables

CREATE TABLE IF NOT EXISTS public.comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  drama_id UUID NOT NULL REFERENCES public.dramas(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  content TEXT NOT NULL,
  like_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT comments_content_length_check CHECK (char_length(btrim(content)) BETWEEN 1 AND 500),
  CONSTRAINT comments_like_count_non_negative_check CHECK (like_count >= 0)
);

CREATE TABLE IF NOT EXISTS public.comment_likes (
  comment_id UUID NOT NULL REFERENCES public.comments(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (comment_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_comments_drama_created_at
  ON public.comments (drama_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_comments_drama_like_created_at
  ON public.comments (drama_id, like_count DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_comments_user_id
  ON public.comments (user_id);

CREATE INDEX IF NOT EXISTS idx_comment_likes_user_id
  ON public.comment_likes (user_id);

DROP TRIGGER IF EXISTS trg_comments_updated_at ON public.comments;
CREATE TRIGGER trg_comments_updated_at
  BEFORE UPDATE ON public.comments
  FOR EACH ROW
  EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.comment_likes ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "all_roles_can_read_comments" ON public.comments;
DROP POLICY IF EXISTS "authenticated_users_can_insert_comments" ON public.comments;
DROP POLICY IF EXISTS "owners_can_update_comments" ON public.comments;
DROP POLICY IF EXISTS "owners_can_delete_comments" ON public.comments;
DROP POLICY IF EXISTS "all_roles_can_read_comment_likes" ON public.comment_likes;
DROP POLICY IF EXISTS "authenticated_users_can_insert_comment_likes" ON public.comment_likes;
DROP POLICY IF EXISTS "owners_can_delete_comment_likes" ON public.comment_likes;

CREATE POLICY "all_roles_can_read_comments" ON public.comments
  FOR SELECT TO authenticated
  USING (true);

CREATE POLICY "authenticated_users_can_insert_comments" ON public.comments
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "owners_can_update_comments" ON public.comments
  FOR UPDATE TO authenticated
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "owners_can_delete_comments" ON public.comments
  FOR DELETE TO authenticated
  USING (auth.uid() = user_id);

CREATE POLICY "all_roles_can_read_comment_likes" ON public.comment_likes
  FOR SELECT TO authenticated
  USING (true);

CREATE POLICY "authenticated_users_can_insert_comment_likes" ON public.comment_likes
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "owners_can_delete_comment_likes" ON public.comment_likes
  FOR DELETE TO authenticated
  USING (auth.uid() = user_id);
