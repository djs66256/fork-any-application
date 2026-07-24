import { config } from '@/lib/config';
import Link from 'next/link';

export default function Home() {
  return (
    <main>
      <h1>{config.app.name}</h1>
      <p>Version: {config.app.version}</p>
      <p>Environment: {config.app.env}</p>
      <p>API Health: <Link href="/api/health">/api/health</Link></p>
      <p>API Dramas: <Link href="/api/dramas">/api/dramas</Link></p>
    </main>
  );
}
