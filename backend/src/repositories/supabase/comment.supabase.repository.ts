import { z } from 'zod';
import { getSupabaseAdmin } from '@/infrastructure/supabase';
import { Errors } from '@/lib/errors';
import {
  Comment,
  CommentListResponse,
  CommentListResponseSchema,
  CommentSchema,
  ToggleCommentLikeResponse,
  ToggleCommentLikeResponseSchema,
} from '@/lib/schemas';
import {
  CommentRepositoryInterface,
  CreateCommentParams,
  ListDramaCommentsParams,
  ToggleCommentLikeParams,
} from '@/repositories/interfaces/comment.repository.interface';

const CommentAuthorRowSchema = z.object({
  id: z.string().uuid(),
  display_name: z.string().nullable().optional(),
  avatar_url: z.string().url().nullable().optional(),
});

const CommentRowSchema = z.object({
  id: z.string().uuid(),
  drama_id: z.string().uuid(),
  user_id: z.string().uuid(),
  content: z.string(),
  like_count: z.number().int().min(0),
  created_at: z.string(),
  updated_at: z.string(),
  profiles: z.union([CommentAuthorRowSchema, z.null()]).optional(),
});

const COMMENT_SELECT_COLUMNS = 'id,drama_id,user_id,content,like_count,created_at,updated_at,profiles(id,display_name,avatar_url)';

function isSupabaseAvailabilityError(error: { message?: string | null; code?: string | null }): boolean {
  const message = (error.message ?? '').toLowerCase();
  const code = (error.code ?? '').toLowerCase();

  return message.includes('failed to fetch')
    || message.includes('network')
    || message.includes('timeout')
    || message.includes('unavailable')
    || message.includes('connection')
    || code === '08000'
    || code === '08003'
    || code === '08006'
    || code === '57p01';
}

function mapRowToComment(row: unknown, liked: boolean): Comment {
  const parsed = CommentRowSchema.safeParse(row);
  if (!parsed.success) {
    throw Errors.internal('Invalid comment row returned from Supabase');
  }

  const profile = parsed.data.profiles;

  return CommentSchema.parse({
    id: parsed.data.id,
    drama_id: parsed.data.drama_id,
    content: parsed.data.content,
    like_count: parsed.data.like_count,
    liked,
    created_at: parsed.data.created_at,
    updated_at: parsed.data.updated_at,
    user: {
      id: parsed.data.user_id,
      display_name: profile?.display_name?.trim() || '用户',
      avatar_url: profile?.avatar_url ?? null,
    },
  });
}

function computeTotalPages(total: number, pageSize: number): number {
  return total === 0 ? 0 : Math.ceil(total / pageSize);
}

export class CommentSupabaseRepository implements CommentRepositoryInterface {
  async listByDrama(params: ListDramaCommentsParams): Promise<CommentListResponse> {
    const supabase = getSupabaseAdmin();
    const from = (params.page - 1) * params.pageSize;
    const to = from + params.pageSize - 1;

    let query = supabase
      .from('comments')
      .select(COMMENT_SELECT_COLUMNS, { count: 'exact', head: false })
      .eq('drama_id', params.dramaId);

    if (params.sort === 'hot') {
      query = query.order('like_count', { ascending: false });
    }

    const { data, error, count } = await query
      .range(from, to)
      .order('created_at', { ascending: false });

    if (error) {
      if (isSupabaseAvailabilityError(error)) {
        throw Errors.serviceUnavailable('comments');
      }
      throw Errors.internal(`Failed to fetch comments: ${error.message}`);
    }

    const rows = (data ?? []).map((row) => {
      const parsed = CommentRowSchema.safeParse(row);
      if (!parsed.success) {
        throw Errors.internal('Invalid comment row returned from Supabase');
      }
      return parsed.data;
    });
    const likedCommentIds = new Set<string>();

    if (params.userId && rows.length > 0) {
      const { data: likes, error: likesError } = await supabase
        .from('comment_likes')
        .select('comment_id')
        .eq('user_id', params.userId)
        .in('comment_id', rows.map((row) => row.id));

      if (likesError) {
        if (isSupabaseAvailabilityError(likesError)) {
          throw Errors.serviceUnavailable('comment_likes');
        }
        throw Errors.internal(`Failed to fetch comment likes: ${likesError.message}`);
      }

      for (const like of likes ?? []) {
        if (typeof like.comment_id === 'string') {
          likedCommentIds.add(like.comment_id);
        }
      }
    }

    return CommentListResponseSchema.parse({
      data: rows.map((row) => mapRowToComment(row, likedCommentIds.has(row.id))),
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total: count ?? 0,
        total_pages: computeTotalPages(count ?? 0, params.pageSize),
      },
    });
  }

  async create(params: CreateCommentParams): Promise<Comment> {
    const supabase = getSupabaseAdmin();
    const trimmedContent = params.content.trim();

    const { data, error } = await supabase
      .from('comments')
      .insert({
        drama_id: params.dramaId,
        user_id: params.userId,
        content: trimmedContent,
      })
      .select(COMMENT_SELECT_COLUMNS)
      .single();

    if (error) {
      if (isSupabaseAvailabilityError(error)) {
        throw Errors.serviceUnavailable('comments');
      }
      throw Errors.internal(`Failed to create comment: ${error.message}`);
    }

    return mapRowToComment(data, false);
  }

  async toggleLike(params: ToggleCommentLikeParams): Promise<ToggleCommentLikeResponse> {
    const supabase = getSupabaseAdmin();
    const { data: commentRow, error: commentError } = await supabase
      .from('comments')
      .select('id,drama_id,like_count')
      .eq('id', params.commentId)
      .single();

    if (commentError) {
      if (commentError.code === 'PGRST116') {
        throw Errors.commentNotFound(params.commentId);
      }
      if (isSupabaseAvailabilityError(commentError)) {
        throw Errors.serviceUnavailable('comments');
      }
      throw Errors.internal(`Failed to load comment: ${commentError.message}`);
    }

    const parsedComment = z.object({
      id: z.string().uuid(),
      drama_id: z.string().uuid(),
      like_count: z.number().int().min(0),
    }).safeParse(commentRow);

    if (!parsedComment.success) {
      throw Errors.internal('Invalid comment like row returned from Supabase');
    }

    if (parsedComment.data.drama_id !== params.dramaId) {
      throw Errors.commentNotFound(params.commentId);
    }

    const { data: existingLike, error: existingLikeError } = await supabase
      .from('comment_likes')
      .select('comment_id')
      .eq('comment_id', params.commentId)
      .eq('user_id', params.userId)
      .maybeSingle();

    if (existingLikeError) {
      if (isSupabaseAvailabilityError(existingLikeError)) {
        throw Errors.serviceUnavailable('comment_likes');
      }
      throw Errors.internal(`Failed to inspect existing comment like: ${existingLikeError.message}`);
    }

    const currentlyLiked = Boolean(existingLike?.comment_id);
    const liked = !currentlyLiked;
    const nextLikeCount = liked
      ? parsedComment.data.like_count + 1
      : Math.max(0, parsedComment.data.like_count - 1);

    if (liked) {
      const { error: insertError } = await supabase
        .from('comment_likes')
        .insert({
          comment_id: params.commentId,
          user_id: params.userId,
        });

      if (insertError && insertError.code !== '23505') {
        if (isSupabaseAvailabilityError(insertError)) {
          throw Errors.serviceUnavailable('comment_likes');
        }
        throw Errors.internal(`Failed to create comment like: ${insertError.message}`);
      }
    } else {
      const { error: deleteError } = await supabase
        .from('comment_likes')
        .delete()
        .eq('comment_id', params.commentId)
        .eq('user_id', params.userId);

      if (deleteError) {
        if (isSupabaseAvailabilityError(deleteError)) {
          throw Errors.serviceUnavailable('comment_likes');
        }
        throw Errors.internal(`Failed to remove comment like: ${deleteError.message}`);
      }
    }

    const { error: updateError } = await supabase
      .from('comments')
      .update({ like_count: nextLikeCount })
      .eq('id', params.commentId);

    if (updateError) {
      if (isSupabaseAvailabilityError(updateError)) {
        throw Errors.serviceUnavailable('comments');
      }
      throw Errors.internal(`Failed to update comment like count: ${updateError.message}`);
    }

    return ToggleCommentLikeResponseSchema.parse({
      comment_id: params.commentId,
      liked,
      like_count: nextLikeCount,
    });
  }
}
