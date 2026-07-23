# Backend 开源库选型

> 本文件列出 Backend 端将使用或可能使用到的开源库，按功能领域分组。
> 标记说明：
> - ✅ 已选定 / 强烈推荐
> - 🔶 备选 / 待评估
> - ⚠️ 需用户确认后才能引入

---

## 框架与工具链

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Next.js | 全栈框架（API Routes） | ✅ | 已确定技术栈 |
| TypeScript | 类型系统 | ✅ | |
| Zod | 数据校验与类型推导 | ✅ | 已确定 |
| ESLint + Prettier | 代码规范 | ✅ | |
| tsx | TypeScript 执行 | 🔶 | 开发期运行 TS 文件 |

---

## 数据库与 ORM

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Supabase JS Client | 数据库访问 + Auth | ✅ | 已确定技术栈 |
| Prisma | ORM | 🔶 | 类型安全、Schema 管理、Migration |

---

## 认证与授权

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Supabase Auth | 认证服务 | ✅ | 已确定技术栈 |

---

## 文件存储

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Supabase Storage | 文件存储 | ✅ | S3 兼容 |

---

## 缓存

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| ioredis | Redis 客户端 | 🔶 | 功能全面 |
| upstash/redis | Redis 客户端 | 🔶 | Serverless-friendly |
| lru-cache | 内存 LRU 缓存 | 🔶 | 进程内缓存 |

---

## 任务队列

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| BullMQ | Redis 任务队列 | 🔶 | 可靠、功能丰富 |

---

## 日志与监控

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| pino | 日志框架 | 🔶 | Node.js 最快日志 |
| winston | 日志框架 | 🔶 | 功能全面 |
| Sentry Node | 错误追踪 | 🔶 | |

---

## API 文档

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| OpenAPI / Swagger | API 文档规范 | 🔶 | |
| next-swagger-doc | Next.js OpenAPI 文档 | 🔶 | |
| Scalar | API 文档 UI | 🔶 | 比 Swagger UI 更现代 |

---

## 测试

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Vitest | 测试框架 | ✅ | 速度快 |
| Supertest | HTTP 测试 | 🔶 | API 集成测试 |
| Testcontainers | Docker 容器测试 | 🔶 | 数据库集成测试 |
| Faker.js | 测试数据生成 | 🔶 | |
| MSW | API Mock | 🔶 | 拦截 outgoing 请求 |

---

## 安全

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| helmet | HTTP 安全 Headers | 🔶 | |
| rate-limiter-flexible | 限流 | 🔶 | 多种存储后端 |
| express-rate-limit | Express 限流 | 🔶 | Next.js 可适配 |
| zod | 输入校验 | ✅ | 防护注入 |

---

## 邮件与通知

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Resend | 邮件发送 | 🔶 | 现代 API |
| Nodemailer | 邮件发送 | 🔶 | 经典方案 |
| Firebase Admin SDK | 推送通知 | 🔶 | |

---

## 视频处理

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| FFmpeg (CLI) | 视频转码/处理 | 🔶 | 旋召唤进程 |
| fluent-ffmpeg | FFmpeg Node 封装 | 🔶 | |
| Mux Node SDK | 视频托管/处理 | 🔶 | 需 Mux 服务 |

---

## DevOps / CI

| 库 | 用途 | 状态 | 备注 |
|---|------|------|------|
| Supabase CLI | 数据库管理、Migration | ✅ | 已确定技术栈 |
| Docker | 容器化 | 🔶 | |
| GitHub Actions | CI/CD | 🔶 | |
