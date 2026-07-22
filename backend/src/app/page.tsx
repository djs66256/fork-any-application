import { config } from '@/lib/config';

export default function Home() {
  return (
    <main>
      <h1>{config.app.name}</h1>
      <p>Version: {config.app.version}</p>
      <p>Environment: {config.app.env}</p>
      <p>API Health: <a href="/api/health">/api/health</a></p>
    </main>
  );
}
