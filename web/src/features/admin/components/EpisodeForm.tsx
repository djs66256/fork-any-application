'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { adminApi } from '@/features/admin/api/client';
import type { AdminEpisode, AdminEpisodeFormData } from '@/features/admin/api/types';
import styles from './DramaForm.module.css';

interface EpisodeFormProps {
  dramaId: string;
  initialData?: AdminEpisode;
  isEdit?: boolean;
  dramaTitle?: string;
}

export function EpisodeForm({ dramaId, initialData, isEdit, dramaTitle }: EpisodeFormProps) {
  const router = useRouter();

  const [title, setTitle] = useState(initialData?.title ?? '');
  const [episodeNumber, setEpisodeNumber] = useState(
    initialData?.episode_number?.toString() ?? '',
  );
  const [duration, setDuration] = useState(initialData?.duration?.toString() ?? '');
  const [videoUrl, setVideoUrl] = useState(initialData?.video_url ?? '');
  const [thumbnailUrl, setThumbnailUrl] = useState(initialData?.thumbnail_url ?? '');
  const [description, setDescription] = useState(initialData?.description ?? '');

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!title.trim()) {
      errors.title = '请输入剧集标题';
    }
    if (!episodeNumber || parseInt(episodeNumber, 10) < 1) {
      errors.episode_number = '请输入有效的剧集号（>=1）';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError(null);

    const formData: AdminEpisodeFormData = {
      title: title.trim(),
      episode_number: parseInt(episodeNumber, 10),
      duration: duration ? parseInt(duration, 10) : undefined,
      video_url: videoUrl || undefined,
      thumbnail_url: thumbnailUrl || undefined,
      description: description || undefined,
    };

    try {
      if (isEdit && initialData) {
        await adminApi.updateEpisode(initialData.id, formData);
      } else {
        await adminApi.createEpisode(dramaId, formData);
      }
      router.push(`/admin/dramas/${dramaId}/episodes`);
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : '保存失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  const cancelHref = `/admin/dramas/${dramaId}/episodes`;

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      {dramaTitle && (
        <p style={{ fontSize: 'var(--font-size-small)', color: 'var(--color-fg-muted)', margin: 0 }}>
          所属短剧：{dramaTitle}
        </p>
      )}

      <div className={styles.field}>
        <label className={styles.label} htmlFor="title">
          标题<span className={styles.required}>*</span>
        </label>
        <input
          id="title"
          className={styles.input}
          type="text"
          value={title}
          onChange={(e) => {
            setTitle(e.target.value);
            setFieldErrors((prev) => ({ ...prev, title: '' }));
          }}
          placeholder="输入剧集标题"
          maxLength={200}
        />
        {fieldErrors.title && (
          <span className={styles.fieldError}>{fieldErrors.title}</span>
        )}
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="episodeNumber">
          剧集号<span className={styles.required}>*</span>
        </label>
        <input
          id="episodeNumber"
          className={styles.input}
          type="number"
          value={episodeNumber}
          onChange={(e) => {
            setEpisodeNumber(e.target.value);
            setFieldErrors((prev) => ({ ...prev, episode_number: '' }));
          }}
          placeholder="1"
          min={1}
        />
        {fieldErrors.episode_number && (
          <span className={styles.fieldError}>{fieldErrors.episode_number}</span>
        )}
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="duration">
          时长（秒）
        </label>
        <input
          id="duration"
          className={styles.input}
          type="number"
          value={duration}
          onChange={(e) => setDuration(e.target.value)}
          placeholder="300"
          min={0}
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="videoUrl">
          视频 URL
        </label>
        <input
          id="videoUrl"
          className={styles.input}
          type="text"
          value={videoUrl}
          onChange={(e) => setVideoUrl(e.target.value)}
          placeholder="https://example.com/video.mp4"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="thumbnailUrl">
          缩略图 URL
        </label>
        <input
          id="thumbnailUrl"
          className={styles.input}
          type="text"
          value={thumbnailUrl}
          onChange={(e) => setThumbnailUrl(e.target.value)}
          placeholder="https://example.com/thumb.jpg"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="description">
          描述
        </label>
        <textarea
          id="description"
          className={styles.textarea}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="输入剧集描述"
        />
      </div>

      {submitError && <div className={styles.formError}>{submitError}</div>}

      <div className={styles.actions}>
        <button
          className={styles.submitButton}
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting ? '保存中...' : isEdit ? '保存修改' : '新建剧集'}
        </button>
        <Link href={cancelHref} className={styles.cancelLink}>
          取消
        </Link>
      </div>
    </form>
  );
}