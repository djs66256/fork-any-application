# 2026-07-24 — Supabase 作为基础 BaaS 平台

> 状态：已采纳
> 决策人：daniel
> 最后更新：2026-07-24

## 背景

项目从零开始构建多端短剧平台，需要数据库、认证、存储和实时推送等后端基础设施。传统方案需要自建 PostgreSQL + 自建 Auth，运维成本高。

## 决策

使用 Supabase 作为项目的核心后端基础设施（BaaS），替代自建 PostgreSQL + 自建 Auth 的方案。本地开发通过 `supabase start` 一键启动完整栈（PostgreSQL + Auth + Storage + Realtime + Studio）。

## 备选方案

| 方案 | 优点 | 缺点 | 结论 |
|------|------|------|------|
| Supabase BaaS | 开箱即用（DB/Auth/Storage/Realtime）；本地 CLI 一键启动；内置 Row Level Security；Studio 管理界面 | 平台锁定风险；需学习 Supabase 特有 API | 采纳 |
| 自建 PostgreSQL + 自建 Auth | 完全控制；无供应商锁定 | 运维成本高；需手动管理 migration；需自建 Auth 系统 | 拒绝（运维成本过高） |
| Firebase | 与 Supabase 类似的 BaaS | 非开源；NoSQL 不适合关系型数据模型 | 拒绝（关系型数据为主） |

## 影响

- Infrastructure 层使用 `@supabase/supabase-js` SDK，提供双客户端实例（anon key + service role key）
- Repository 层使用 Supabase Client 的 `from().select().insert()` API
- Migration 使用 Supabase CLI（`supabase migration`），原生 SQL 方式
- RLS 策略在后续 PRD 中启用，当前阶段表公开可读
- Auth 集成由后续 PRD 启用 Supabase Auth

### 源文件

- `backend/src/infrastructure/supabase.ts:L1` — Supabase Client 双实例 + 健康检查
- `backend/src/lib/config.ts` — SUPABASE_URL / SUPABASE_ANON_KEY / SUPABASE_SERVICE_ROLE_KEY 配置项
- `backend/supabase/config.toml` — Supabase CLI 本地开发配置
- `backend/supabase/migrations/00000000000001_init_tables.sql` — 初始 schema migration

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 初始创建，记录 Supabase BaaS 选型决策 |

---

*本文档由 llm-wiki skill 自动维护。*
