# 子任务拆分：用户登录与注册

> 关联 PRD：[prd.md](prd.md)
> 创建日期：2026-07-25 | 状态：草稿

---

## 工时总览

| 平台 | 子任务数 | 总工时（人日） |
|------|---------|--------------|
| Backend | 1 | 2.5 人日 |
| iOS | 2 | 3 人日 |
| Android | 2 | 3 人日 |
| **合计** | **5** | **8.5 人日** |

---

## 子任务详情

### ST-01：Backend Supabase Auth 集成

- 工时：2.5 人日 | P0
- 配置 Supabase Auth、SMS provider 配置
- `POST /api/auth/send-code`（发送验证码）
- `POST /api/auth/verify-code`（验证登录）
- `POST /api/auth/logout`（登出，调用 `supabase.auth.signOut()` 使当前 session 失效）
- `GET /api/auth/me`（获取当前用户）
- 升级 `backend/src/middleware/auth.ts` 从骨架实现为真实 Supabase JWT 校验（`supabase.auth.getUser(jwt)`）

### ST-02：iOS 登录页 UI + Token 管理

- 工时：2 人日 | P0
- 登录页（手机号+验证码+协议勾选）、Token 存储（Keychain）、自动登录恢复

### ST-03：Android 登录页 UI + Token 管理

- 工时：2 人日 | P0
- 登录页（手机号+验证码+协议勾选）、Token 存储（EncryptedSharedPreferences / DataStore）、自动登录恢复

### ST-04：iOS 登录拦截集成

- 工时：1 人日 | P1
- 统一登录拦截中间件：评论/预约等操作触发的登录检查

### ST-05：Android 登录拦截集成

- 工时：1 人日 | P1

---

## 变更历史

| 日期 | 变更内容 |
|------|---------|
| 2026-07-25 | 初始版本 |
