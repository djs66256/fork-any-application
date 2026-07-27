---
name: admin-panel-development
description: >
  管理平台（Admin Panel）全流程开发 skill。
  覆盖从 PRD 到开发落地的完整流程：RBAC 权限设计、Admin API 规范、
  扁平 UI 实现、Supabase 管理端集成。
  触发场景：用户提到"管理平台"、"管理后台"、"admin panel"、"后台管理"；
  开发 PRD-15 或类似的管理端功能；需要实现用户权限管理。
  管理平台仅涉及 Backend（`/api/admin/*`）+ Web（`/admin/*`），不涉及移动端。
---

# Admin Panel Development

## 定位

管理平台是内部运营工具，用于管理 ShortDrama 项目的所有资源（短剧、剧集、用户权限）。
与 consumer-facing 功能（PRD-01~14）完全隔离：

- **路由隔离**：管理平台路由为 `/admin/*`，不干扰现有 consumer 路由
- **API 隔离**：管理 API 为 `/api/admin/*`，已有 consumer API 保持不变
- **端限定**：仅 Backend + Web，不涉及 iOS/Android

## 前置知识

| 知识 | 来源 | 说明 |
|------|------|------|
| 产品信息 | `PRODUCT.md` | 产品名称、技术标识 |
| Web 端工程规范 | `web/CLAUDE.md` | 五层架构、CSS Modules、命令约定 |
| Backend 端工程规范 | `backend/CLAUDE.md` | 四层架构、Supabase、RLS、Migration |
| 数据模型 | `wiki/features/data-models/index.md` | Drama、Episode、UserProfile Schema |

## 能力线

| 能力线 | 职责 | 触发时机 | 执行者 |
|--------|------|---------|--------|
| **RBAC 设计** | 设计 admin/editor/viewer 角色权限矩阵 | 管理平台需求分析阶段 | 主 agent |
| **Admin API 开发** | 实现管理端 CRUD API + Auth 中间件 | 后端开发阶段 | 主 agent（加载 backend-development） |
| **Admin UI 开发** | 实现管理端页面、路由、权限控制 | 前端开发阶段 | 主 agent（加载 web-development） |
| **RLS 配置** | Supabase 行级安全策略 | Admin API 开发后 | 主 agent |

## 规范索引

| 规范 | 文件 | 说明 |
|------|------|------|
| **开发流程** | [references/admin-panel-workflow.md](references/admin-panel-workflow.md) | 从 PRD 到开发落地的完整阶段 |
| **RBAC 设计** | [references/rbac-design.md](references/rbac-design.md) | 角色定义、权限矩阵、JWT role 同步 |
| **Admin API 规范** | [references/admin-api-standards.md](references/admin-api-standards.md) | 路由前缀、响应格式、中间件 |
| **扁平 UI 规范** | [references/flat-ui-standards.md](references/flat-ui-standards.md) | 配色、布局、组件风格约束 |

## PRD 文档

管理平台当前 PRD 为 PRD-15，详见 `docs/product_manager/prd/2026-07-27-admin-panel/`：

| 文档 | 内容 |
|------|------|
| `prd.md` | 功能需求（登录、仪表盘、短剧/剧集/用户管理、RBAC） |
| `subtasks.md` | 7 个子任务（ST-01~ST-07），18.5 人日 |
| `prd-review.md` | 2 轮审查记录 |

## 开发约束

### 必须隔离

- 管理平台路由 `/admin/*` 不与 consumer 路由 `/`、`/play/:id`、`/detail/:id` 交叉
- Admin API 前缀 `/api/admin/*` 不与 consumer API `/api/dramas`、`/api/player/*` 交叉
- Web 端 admin 相关代码放在 `web/src/app/admin/` 和 `web/src/features/admin/` 下

### 技术栈

| 层 | 技术 |
|----|------|
| Backend 认证 | Supabase Auth JWT + 中间件 `requireRole(...)` |
| Backend API | Next.js Route Handlers + Zod Schema |
| Backend 权限 | RLS 策略 + API 中间件双重校验 |
| Web 样式 | CSS Modules + `tokens.css` 自定义属性（不使用 Tailwind CSS） |
| Web 认证 | `@supabase/supabase-js` / `@supabase/ssr`（需征得用户同意后安装） |

### 依赖声明

Web 端新增依赖（需征得用户同意）：
- `@supabase/supabase-js` — Supabase 客户端 SDK
- `@supabase/ssr` — Next.js SSR 环境下的 session 管理

## 子任务执行顺序

按 subtasks.md 依赖图执行：

```
ST-01 (Auth 中间件 + RLS) → ST-02 (CRUD API)
ST-03 (Shell + 登录) → ST-04 (短剧管理) → ST-05 (剧集管理)
                      → ST-06 (用户管理)
ST-04 → ST-07 (仪表盘)
```

- 实线 = 必须在前置完成后开始
- 虚线 = 可 Mock API 先行开发，联调阶段对接

## 参考

- `web/CLAUDE.md` — Web 端架构、分层、命令惯例
- `backend/CLAUDE.md` — Backend 端架构、分层、Supabase 规范
- `PRODUCT.md` — 产品信息
- `docs/product_manager/prd/2026-07-27-admin-panel/` — PRD-15 完整文档
