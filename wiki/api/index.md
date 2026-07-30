# API 文档索引

- [健康检查 (Health)](health.md) — `GET /api/health`，返回服务状态、版本号及 Supabase DB + Redis 连通性
- [认证 (Auth)](auth.md) — `POST /api/auth/otp-requests`、`POST /api/auth/sessions`、`POST /api/auth/session-refreshes`、`GET /api/users/me`、`DELETE /api/auth/session`；覆盖手机号验证码登录 / 自动注册、会话恢复、刷新与登出契约
- [个人资产 (User Assets)](user-assets.md) — `GET /api/users/me/bookings`；覆盖当前用户预约资产分页列表、`online/upcoming` 状态过滤、snake_case 分页字段与 summary 摘要
- [短剧 (Dramas)](dramas.md) — `GET /api/dramas`、`GET /api/dramas/channel`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book`、`POST /api/dramas`、`GET /api/dramas/[id]`；覆盖首页 Feed、剧场 Feed、搜索发现、排行、分类标签、预约与详情接口契约
- [签到 (Check-Ins)](check-ins.md) — `GET /api/check-ins/status`、`POST /api/check-ins`；覆盖首页签到浮层的状态查询、当日签到提交、`X-Installation-Id` 匿名主体、可选登录与服务端业务日规则
- [消息 (Messages)](messages.md) — `GET /api/messages/preview`、`GET /api/messages/system`、`GET /api/messages/interactions`；覆盖菜单消息预览、系统消息列表、互动消息登录门槛、204 空态与双列表 contract
- [剧集 (Episodes)](episodes.md) — `GET /api/episodes/[id]`，剧集详情接口（骨架）
- [商城 (Mall)](mall.md) — `GET /api/mall/products`；覆盖商城首页双列商品 Feed 的分页契约、校验规则、空态与错误处理
- [赚钱中心 (Earn)](earn.md) — `GET /api/earn/overview`、`POST /api/earn/complete-task`；覆盖赚钱首页聚合数据、可选鉴权首屏、代表性任务 Bearer-only 完成与幂等奖励语义
- [播放器 (Player)](player.md) — `GET /api/player/recently-viewed`、`POST /api/player/start`、`POST /api/player/stop`；覆盖菜单最近在看与播放历史主链路
- [管理平台 (Admin)](admin.md) — 14 个管理 API 端点（`/api/admin/*`），涵盖认证、仪表盘统计、短剧 CRUD、剧集 CRUD、用户管理与角色分配

---
*本文档由 llm-wiki skill 自动维护。*
