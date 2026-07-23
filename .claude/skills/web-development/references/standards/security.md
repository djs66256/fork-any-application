# 安全 — Web

> 本文档定义 Web 端的安全规范。

---

## 1. XSS 防护

### 1.1 输出编码

React 默认对 JSX 中的所有插值进行 HTML 转义，这是第一道防线。

```typescript
// ✅ 安全：React 自动转义
function UserComment({ text }: { text: string }) {
  return <p>{text}</p>; // <script> 会被转义为 &lt;script&gt;
}
```

**dangerouslySetInnerHTML 使用原则：**

- **默认禁止使用**，ESLint 规则 `react/no-danger: "error"`
- 仅在以下严格场景允许（需代码审查批准）：
  - 渲染服务端返回的富文本 HTML（如剧集简介中的格式化文本）
  - 渲染转义后的 HTML 邮件模板
- 使用前必须通过 DOMPurify 清洗：

```typescript
import DOMPurify from 'dompurify';

interface RichContentProps {
  html: string;
}

export function RichContent({ html }: RichContentProps) {
  // eslint-disable-next-line react/no-danger -- 已通过 DOMPurify 清洗
  const clean = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'a'],
    ALLOWED_ATTR: ['href', 'target'],
  });

  return <div dangerouslySetInnerHTML={{ __html: clean }} />;
}
```

**URL 安全：**

- 用户提供的 URL 用于 `<a href>` 时，确保协议为 `http:` / `https:` / `mailto:`
- `javascript:` 协议的 URL 必须拒绝

```typescript
function sanitizeUrl(url: string): string {
  const trimmed = url.trim();
  if (/^(https?:\/\/|mailto:|\/)/i.test(trimmed)) {
    return trimmed;
  }
  // 拒绝 javascript: data: 等危险协议
  return '#';
}
```

### 1.2 CSP

使用 Content-Security-Policy Header 限制资源来源。在 `next.config.ts` 中配置：

```typescript
// next.config.ts
async headers() {
  return [
    {
      source: '/(.*)',
      headers: [
        {
          key: 'Content-Security-Policy',
          value: [
            "default-src 'self'",
            `script-src 'self' 'unsafe-inline' 'unsafe-eval'`,
            // Next.js 开发模式需要 unsafe-eval（Fast Refresh），生产环境可收紧
            `style-src 'self' 'unsafe-inline'`,
            // 按需放开媒体源
            `media-src 'self' https://cdn.shortdrama.com blob:`,
            `img-src 'self' https: data: blob:`,
            `font-src 'self'`,
            `connect-src 'self' https://api.shortdrama.com`,
            `frame-ancestors 'none'`,
            `form-action 'self'`,
          ].join('; '),
        },
      ],
    },
  ];
},
```

**CSP 策略说明：**

| 指令 | 值 | 说明 |
|------|------|------|
| `default-src` | `'self'` | 默认仅允许同源资源 |
| `script-src` | `'self'` + Next.js 必要值 | 阻止外部脚本注入 |
| `media-src` | CDN + blob | 视频播放需要 blob: |
| `connect-src` | 后端 API 域名 | 限制 API 请求目标 |
| `frame-ancestors` | `'none'` | 禁止被嵌入 iframe |
| `form-action` | `'self'` | 禁止表单提交到外部 |

CSP 部署建议：

1. 先用 `Content-Security-Policy-Report-Only` 模式观察日志，避免误伤
2. 通过 `report-uri /api/csp-report` 收集违规报告
3. 稳定后切换为 `Content-Security-Policy` 强执行

### 1.3 Trusted Types

Trusted Types 是更深层的 DOM XSS 防护机制，通过限制赋值给 `innerHTML` 等 sink 的值的类型来防止注入。

```typescript
// next.config.ts headers 中添加
// "require-trusted-types-for 'script'"

// 创建 Trusted Types Policy
if (typeof window !== 'undefined' && window.trustedTypes) {
  window.trustedTypes.createPolicy('default', {
    createHTML: (input: string) => DOMPurify.sanitize(input),
    createScriptURL: (input: string) => {
      // 仅允许受信任的脚本源
      if (input.startsWith('/') || input.startsWith(process.env.NEXT_PUBLIC_SITE_URL!)) {
        return input;
      }
      throw new TypeError('Untrusted script URL');
    },
  });
}
```

**注意**：Trusted Types 需要浏览器支持（Chrome/Edge > 83），且启用后 `innerHTML` 赋值会强制要求 TrustedHTML 类型。当前阶段建议先配置 CSP，Trusted Types 待团队 infra 成熟后再开启。

---

## 2. CSRF 防护

### 2.1 Token 方案

**基于 Cookie 的 Session 认证方案：**

| Cookie 属性 | 值 | 说明 |
|-------------|------|------|
| `HttpOnly` | `true` | JavaScript 不可读取，防止 XSS 窃取 |
| `Secure` | `true`（生产环境） | 仅 HTTPS 传输 |
| `SameSite` | `Lax` | 阻止跨站 POST 请求携带 Cookie |
| `Path` | `/` | Cookie 作用路径 |

```typescript
// 服务端设置 Cookie 示例（API Route 或 middleware）
import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

export async function setSessionCookie(token: string) {
  const cookieStore = await cookies();
  cookieStore.set('session', token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 7 * 24 * 60 * 60, // 7 天
  });
}
```

**自定义 Header 方案（适用于 Token 认证）：**

如果使用 Bearer Token 认证（Authorization Header），需要额外添加 CSRF Token：

```typescript
// lib/api/client.ts — 发送请求时附带 CSRF Token
async function getCsrfToken(): Promise<string> {
  // 从 Cookie 中读取 CSRF Token（非 HttpOnly）
  const match = document.cookie.match(/(?:^|;\s*)csrf_token=([^;]+)/);
  return match?.[1] ?? '';
}

// 在请求 Header 中附带
headers['X-CSRF-Token'] = getCsrfToken();
```

**每次登录后应刷新 CSRF Token 和 Session Token**，防止 Session Fixation。

### 2.2 Server Actions

Next.js Server Actions 内置 CSRF 保护：

- 服务端自动验证请求来源（Origin / Referer Header）
- Action 请求使用 `Next-Action` Header 标识
- 无需手动添加 CSRF Token（前提是使用 Server Actions 提交表单）

**对于本项目**：当前后端使用独立 RESTful API（`backend/`），前端的表单提交通常通过 `POST /api/...` 调用后端，因此 CSRF 防护主要在 API Route 层面通过 SameSite Cookie + Origin 验证实现。

---

## 3. 认证与授权

### 3.1 Session 管理

**Session Cookie 最佳实践：**

```typescript
// 登录成功后设置 Session
import { SignJWT } from 'jose';

async function createSession(userId: string): Promise<string> {
  const secret = new TextEncoder().encode(process.env.JWT_SECRET);
  const token = await new SignJWT({ sub: userId })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime('7d')
    .sign(secret);

  const cookieStore = await cookies();
  cookieStore.set('session', token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    maxAge: 7 * 24 * 60 * 60,
  });

  return token;
}

// 退出登录时清除 Session
async function destroySession() {
  const cookieStore = await cookies();
  cookieStore.delete('session');
}
```

**Session 验证中间件（middleware.ts）：**

```typescript
// middleware.ts
import { NextRequest, NextResponse } from 'next/server';
import { jwtVerify } from 'jose';

export async function middleware(request: NextRequest) {
  const session = request.cookies.get('session')?.value;
  const { pathname } = request.nextUrl;

  // 保护路由
  const protectedPaths = ['/profile', '/settings', '/history'];
  const isProtected = protectedPaths.some(p => pathname.startsWith(p));

  if (!isProtected) {
    return NextResponse.next();
  }

  if (!session) {
    const loginUrl = new URL('/login', request.url);
    loginUrl.searchParams.set('redirect', pathname);
    return NextResponse.redirect(loginUrl);
  }

  // 验证 JWT
  try {
    const secret = new TextEncoder().encode(process.env.JWT_SECRET);
    await jwtVerify(session, secret);
    return NextResponse.next();
  } catch {
    // Token 无效或过期
    const response = NextResponse.redirect(new URL('/login', request.url));
    response.cookies.delete('session');
    return response;
  }
}

export const config = {
  matcher: ['/((?!api|_next/static|_next/image|favicon.ico).*)'],
};
```

### 3.2 Token 存储

| 存储方式 | 安全性 | 适用场景 |
|---------|--------|---------|
| httpOnly Cookie | 高（JS 不可读） | Session Token、Refresh Token |
| 内存变量（React State） | 高（刷新即丢失） | Access Token 短期缓存 |
| localStorage | 低（XSS 可窃取） | 不推荐存储 Token |
| sessionStorage | 中（标签页关闭即清除） | 仅限非敏感临时数据 |

**本项目推荐方案：Session Token 存 httpOnly Cookie + 服务端验证**

```typescript
// ❌ 禁止：将 Token 存入 localStorage
localStorage.setItem('access_token', token);

// ✅ 正确：Token 由服务端通过 httpOnly Cookie 管理
// 客户端只需调用 API，Cookie 自动携带
```

**如果必须在前端缓存 Token**（如需要减少服务端 Session 查询），将 Access Token 存为闭包变量（而非 localStorage）：

```typescript
// lib/auth/token.ts
let accessToken: string | null = null;

export function setAccessToken(token: string) {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

export function clearAccessToken() {
  accessToken = null;
}
```

### 3.3 路由守卫

**页面级守卫**（Server Component）：

```typescript
// app/profile/page.tsx
import { redirect } from 'next/navigation';
import { cookies } from 'next/headers';
import { jwtVerify } from 'jose';

export default async function ProfilePage() {
  const cookieStore = await cookies();
  const session = cookieStore.get('session')?.value;

  if (!session) {
    redirect('/login?redirect=/profile');
  }

  try {
    const secret = new TextEncoder().encode(process.env.JWT_SECRET);
    const { payload } = await jwtVerify(session, secret);
    const user = await fetchUser(payload.sub!);

    return <ProfileContent user={user} />;
  } catch {
    redirect('/login?redirect=/profile');
  }
}
```

**组件级守卫**（Client Component）：

```typescript
'use client';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { data: session, isLoading } = useSession();

  useEffect(() => {
    if (!isLoading && !session) {
      router.push('/login');
    }
  }, [session, isLoading, router]);

  if (isLoading) return <LoadingSkeleton />;
  if (!session) return null;

  return <>{children}</>;
}
```

**注意**：页面级守卫（Server Component）是最安全的，因为逻辑在服务端执行，无法被客户端绕过。Client Component 守卫仅作为 UI 层的补充。

---

## 4. 敏感信息管理

### 4.1 NEXT_PUBLIC_ 前缀

| 前缀 | 可见范围 | 用途 |
|------|---------|------|
| `NEXT_PUBLIC_*` | 浏览器 bundle 中可见 | 非敏感的配置（站点名、CDN 地址、App ID 等） |
| 无前缀 | 仅服务端可见 | 密钥、Token、数据库连接串、第三方 API Secret |

```bash
# .env.local

# ✅ 客户端安全
NEXT_PUBLIC_SITE_URL=http://localhost:3000
NEXT_PUBLIC_CDN_URL=https://cdn.shortdrama.com
NEXT_PUBLIC_APP_ID=com.djs66256.short_drama

# ✅ 仅服务端（不会出现在浏览器 bundle 中）
JWT_SECRET=your-super-secret-key-min-32-chars
UPLOAD_API_SECRET=xxxxx
DATABASE_URL=postgresql://localhost:5432/shortdrama
```

**危险示例：**

```bash
# ❌ 绝对禁止！Secret Key 暴露到浏览器 bundle
NEXT_PUBLIC_JWT_SECRET=...
NEXT_PUBLIC_API_KEY=...
NEXT_PUBLIC_DATABASE_URL=...
```

### 4.2 禁止硬编码

- API Key、Secret、Token 等统一通过环境变量管理
- 不同环境（dev/staging/prod）使用不同的环境变量文件
- `.env.local` / `.env.production.local` 加入 `.gitignore`，不提交到 Git

```bash
# .gitignore
.env*.local
```

- 模板文件 `.env.example` 可提交到 Git，仅包含变量名（不包含真实值）：

```bash
# .env.example
API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_SITE_URL=http://localhost:3000
NEXT_PUBLIC_APP_NAME=ShortDrama
NEXT_PUBLIC_APP_ID=com.djs66256.short_drama
# JWT_SECRET=<生成随机 32 位字符串>
# UPLOAD_API_SECRET=<第三方上传服务密钥>
```

**CI/CD 中的敏感信息处理：**

- CI 使用 GitHub Secrets 或环境变量注入
- 构建时的环境变量不打印到日志
- 部署平台（Vercel、Docker）通过各自的 Secrets 管理面板配置
