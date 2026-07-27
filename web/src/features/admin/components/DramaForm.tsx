'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { adminApi } from '@/features/admin/api/client';
import type { AdminDrama, AdminDramaFormData } from '@/features/admin/api/types';
import styles from './DramaForm.module.css';

interface DramaFormProps {
  initialData?: AdminDrama;
  isEdit?: boolean;
}

export function DramaForm({ initialData, isEdit }: DramaFormProps) {
  const router = useRouter();

  const [title, setTitle] = useState(initialData?.title ?? '');
  const [description, setDescription] = useState(initialData?.description ?? '');
  const [coverUrl, setCoverUrl] = useState(initialData?.cover_url ?? '');
  const [category, setCategory] = useState(initialData?.category ?? '');
  const [episodeCount, setEpisodeCount] = useState(
    initialData?.episode_count?.toString() ?? '0',
  );
  const [tags, setTags] = useState(initialData?.tags?.join(', ') ?? '');
  const [rating, setRating] = useState(initialData?.rating?.toString() ?? '');

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!title.trim()) {
      errors.title = '请输入标题';
    } else if (title.length > 200) {
      errors.title = '标题不能超过200个字符';
    }
    if (rating && (isNaN(Number(rating)) || Number(rating) < 0 || Number(rating) > 10)) {
      errors.rating = '评分范围为 0-10';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setSubmitError(null);

    const formData: AdminDramaFormData = {
      title: title.trim(),
      description: description || undefined,
      cover_url: coverUrl || null,
      category: category || undefined,
      episode_count: parseInt(episodeCount, 10) || 0,
      tags: tags
        ? tags.split(',').map((t) => t.trim()).filter(Boolean)
        : [],
      rating: rating ? parseFloat(rating) : null,
    };

    try {
      if (isEdit && initialData) {
        await adminApi.updateDrama(initialData.id, formData);
      } else {
        await adminApi.createDrama(formData);
      }
      router.push('/admin/dramas');
    } catch (err) {
      setSubmitError(err instanceof Error ? err.message : '保存失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  const cancelHref = isEdit && initialData
    ? `/admin/dramas`
    : '/admin/dramas';

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
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
          placeholder="输入短剧标题"
          maxLength={200}
        />
        {fieldErrors.title && (
          <span className={styles.fieldError}>{fieldErrors.title}</span>
        )}
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
          placeholder="输入短剧描述"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="coverUrl">
          封面 URL
        </label>
        <input
          id="coverUrl"
          className={styles.input}
          type="text"
          value={coverUrl}
          onChange={(e) => setCoverUrl(e.target.value)}
          placeholder="https://example.com/cover.jpg"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="category">
          分类
        </label>
        <input
          id="category"
          className={styles.input}
          type="text"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
          placeholder="输入分类"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="episodeCount">
          集数
        </label>
        <input
          id="episodeCount"
          className={styles.input}
          type="number"
          value={episodeCount}
          onChange={(e) => setEpisodeCount(e.target.value)}
          min={0}
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="tags">
          标签
        </label>
        <input
          id="tags"
          className={styles.input}
          type="text"
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder="标签1, 标签2, 标签3"
        />
      </div>

      <div className={styles.field}>
        <label className={styles.label} htmlFor="rating">
          评分
        </label>
        <input
          id="rating"
          className={styles.input}
          type="number"
          value={rating}
          onChange={(e) => {
            setRating(e.target.value);
            setFieldErrors((prev) => ({ ...prev, rating: '' }));
          }}
          placeholder="0-10"
          min={0}
          max={10}
          step={0.1}
        />
        {fieldErrors.rating && (
          <span className={styles.fieldError}>{fieldErrors.rating}</span>
        )}
      </div>

      {submitError && <div className={styles.formError}>{submitError}</div>}

      <div className={styles.actions}>
        <button
          className={styles.submitButton}
          type="submit"
          disabled={isSubmitting}
        >
          {isSubmitting ? '保存中...' : isEdit ? '保存修改' : '新建短剧'}
        </button>
        <Link href={cancelHref} className={styles.cancelLink}>
          取消
        </Link>
      </div>
    </form>
  );
}