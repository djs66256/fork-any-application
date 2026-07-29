# API 文档索引

- [健康检查 (Health)](health.md) — `GET /api/health`，返回服务状态、版本号及 Supabase DB + Redis 连通性
- [认证 (Auth)](auth.md) — `POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session`；覆盖手机号验证码登录 / 自动注册、会话恢复、刷新与登出契约
- [短剧 (Dramas)](dramas.md) — `GET /api/dramas`、`GET /api/dramas/channel`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book`、`POST /api/dramas`、`GET /api/dramas/[id]`；其中首页 Feed、剧场 Feed、搜索发现、排行、分类标签、预约与详情接口均已形成可消费契约
- [商城 (Mall)](mall.md) — `GET /api/mall/products`；覆盖商城首页双列商品 Feed 的分页契约、校验规则、空态与错误处理
- [剧集 (Episodes)](episodes.md) — `GET /api/episodes/[id]`，剧集详情接口（骨架）
- [赚钱中心 (Earn)](earn.md) — `GET /api/earn/overview`、`POST /api/earn/complete-task`；覆盖赚钱首页聚合数据、可选鉴权首屏、代表性任务 Bearer-only 完成与幂等奖励语义
- [播放器 (Player)](player.md) — `POST /api/player/start`、`POST /api/player/stop`，播放控制接口（骨架）
- [管理平台 (Admin)](admin.md) — 14 个管理 API 端点（`/api/admin/*`），涵盖认证、仪表盘统计、短剧 CRUD、剧集 CRUD、用户管理与角色分配

---
*本文档由 llm-wiki skill 自动维护。*
