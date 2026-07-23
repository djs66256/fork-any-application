# 基础库与基础能力 — Backend

> 本文档定义 Backend 端的基础库选型、集成方案与基础能力接入规范。

---

## 1. HTTP 服务

<!-- TODO: 补充 Next.js 服务端能力 -->

### 1.1 Route Handler

<!-- TODO: route.ts 规范、请求/响应模型 -->

### 1.2 中间件

<!-- TODO: middleware.ts（Auth、Logging、CORS、Rate Limiting）-->

### 1.3 Server Actions

<!-- TODO: 表单提交、数据变更 -->

---

## 2. 数据校验

<!-- TODO: 补充 Zod 使用规范 -->

### 2.1 输入校验

<!-- TODO: Request Body/Query/Params 校验 -->

### 2.2 业务校验

<!-- TODO: refine、superRefine、自定义错误 -->

### 2.3 类型生成

<!-- TODO: z.infer → TypeScript 类型 -->

---

## 3. 数据库访问

<!-- TODO: 补充数据库访问规范 -->

### 3.1 Supabase Client

<!-- TODO: supabase-js 服务端使用、Service Role -->

### 3.2 ORM (Prisma / Drizzle)

<!-- TODO: Schema 定义、查询、事务 -->

### 3.3 查询优化

<!-- TODO: N+1 问题、连接池、批量操作 -->

---

## 4. 文件存储

<!-- TODO: 补充文件存储方案 -->

### 4.1 Supabase Storage

<!-- TODO: Bucket 管理、上传/下载/删除 -->

### 4.2 图片处理

<!-- TODO: sharp 缩略图、格式转换 -->

### 4.3 访问控制

<!-- TODO: 私有/公开文件、临时 URL -->

---

## 5. 日志系统

<!-- TODO: 补充日志方案 -->

### 5.1 日志框架

<!-- TODO: pino / winston 配置 -->

### 5.2 结构化日志

<!-- TODO: JSON 格式、统一字段（timestamp、level、message、traceId）-->

### 5.3 日志级别

<!-- TODO: trace/debug/info/warn/error/fatal -->

### 5.4 日志上报

<!-- TODO: 集中式日志、查询 -->

---

## 6. 任务队列

<!-- TODO: 补充异步任务方案 -->

### 6.1 异步任务

<!-- TODO: BullMQ / Inngest 集成 -->

### 6.2 定时任务

<!-- TODO: Cron 任务定义、调度 -->

### 6.3 重试策略

<!-- TODO: 指数退避、死信队列 -->

---

## 7. 国际化 (i18n)

<!-- TODO: 补充服务端多语言方案 -->

### 7.1 错误信息

<!-- TODO: 错误码 + 多语言文案映射 -->

### 7.2 内容国际化

<!-- TODO: 数据库多语言字段设计 -->
