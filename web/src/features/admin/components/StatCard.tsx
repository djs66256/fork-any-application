import styles from './StatCard.module.css';

interface StatCardProps {
  label: string;
  value: number;
  isLoading?: boolean;
}

export function StatCard({ label, value, isLoading }: StatCardProps) {
  if (isLoading) {
    return (
      <div className={styles.skeleton}>
        <div className={styles.skeletonLabel} />
        <div className={styles.skeletonValue} />
      </div>
    );
  }

  return (
    <div className={styles.card}>
      <div className={styles.label}>{label}</div>
      <div className={styles.value}>{value}</div>
    </div>
  );
}