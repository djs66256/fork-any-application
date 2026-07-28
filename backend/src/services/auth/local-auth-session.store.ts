import { randomUUID } from 'node:crypto';

const ACCESS_TOKEN_PREFIX = 'local_at_';
const REFRESH_TOKEN_PREFIX = 'local_rt_';

export type LocalAuthSessionRecord = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  phone: string;
  role: 'admin' | 'editor' | 'viewer';
  expiresAt: number;
  refreshExpiresAt: number;
};

export type LocalAuthUserRecord = {
  id: string;
  phone: string;
  role: 'admin' | 'editor' | 'viewer';
};

const accessSessions = new Map<string, LocalAuthSessionRecord>();
const refreshSessions = new Map<string, LocalAuthSessionRecord>();
const localUsers = new Map<string, LocalAuthUserRecord>();

function nowSeconds(): number {
  return Math.floor(Date.now() / 1000);
}

function cloneRecord(record: LocalAuthSessionRecord): LocalAuthSessionRecord {
  return {
    accessToken: record.accessToken,
    refreshToken: record.refreshToken,
    userId: record.userId,
    phone: record.phone,
    role: record.role,
    expiresAt: record.expiresAt,
    refreshExpiresAt: record.refreshExpiresAt,
  };
}

function persistRecord(record: LocalAuthSessionRecord) {
  accessSessions.set(record.accessToken, record);
  refreshSessions.set(record.refreshToken, record);
}

function removeRecord(record: LocalAuthSessionRecord) {
  accessSessions.delete(record.accessToken);
  refreshSessions.delete(record.refreshToken);
}

export function upsertLocalAuthUser(user: LocalAuthUserRecord): void {
  localUsers.set(user.id, user);
}

export function getLocalAuthUser(userId: string): LocalAuthUserRecord | null {
  return localUsers.get(userId) ?? null;
}

function isExpired(record: LocalAuthSessionRecord): boolean {
  const now = nowSeconds();
  return record.expiresAt <= now || record.refreshExpiresAt <= now;
}

export function isLocalAccessToken(token?: string | null): boolean {
  return typeof token === 'string' && token.startsWith(ACCESS_TOKEN_PREFIX);
}

export function isLocalRefreshToken(token?: string | null): boolean {
  return typeof token === 'string' && token.startsWith(REFRESH_TOKEN_PREFIX);
}

export function createLocalAuthSession(input: {
  userId: string;
  phone: string;
  role: 'admin' | 'editor' | 'viewer';
  accessTokenTtlSeconds: number;
  refreshTokenTtlSeconds: number;
}): LocalAuthSessionRecord {
  const now = nowSeconds();
  const record: LocalAuthSessionRecord = {
    accessToken: `${ACCESS_TOKEN_PREFIX}${randomUUID()}`,
    refreshToken: `${REFRESH_TOKEN_PREFIX}${randomUUID()}`,
    userId: input.userId,
    phone: input.phone,
    role: input.role,
    expiresAt: now + input.accessTokenTtlSeconds,
    refreshExpiresAt: now + input.refreshTokenTtlSeconds,
  };

  persistRecord(record);
  return cloneRecord(record);
}

export function getLocalAuthSessionByAccessToken(accessToken: string): LocalAuthSessionRecord | null {
  const record = accessSessions.get(accessToken);
  if (!record) {
    return null;
  }

  if (isExpired(record)) {
    removeRecord(record);
    return null;
  }

  return cloneRecord(record);
}

export function refreshLocalAuthSession(input: {
  refreshToken: string;
  accessTokenTtlSeconds: number;
  refreshTokenTtlSeconds: number;
}): LocalAuthSessionRecord | null {
  const record = refreshSessions.get(input.refreshToken);
  if (!record) {
    return null;
  }

  if (record.refreshExpiresAt <= nowSeconds()) {
    removeRecord(record);
    return null;
  }

  removeRecord(record);
  return createLocalAuthSession({
    userId: record.userId,
    phone: record.phone,
    role: record.role,
    accessTokenTtlSeconds: input.accessTokenTtlSeconds,
    refreshTokenTtlSeconds: input.refreshTokenTtlSeconds,
  });
}

export function revokeLocalAuthSessionByAccessToken(accessToken: string): void {
  const record = accessSessions.get(accessToken);
  if (!record) {
    return;
  }

  removeRecord(record);
}

export function clearLocalAuthSessions(): void {
  accessSessions.clear();
  refreshSessions.clear();
  localUsers.clear();
}
