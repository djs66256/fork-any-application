# 功能域索引

- [应用壳 (App Shell)](app-shell/index.md) — 各端应用启动入口、移动端 5 Tab 容器，以及首页频道承载 Feed、搜索发现、排行、分类等 Native 子页面的关系
- [首页信息流 (Homepage Feed)](homepage-feed/index.md) — Android / iOS 首页首屏 Feed、Backend `GET /api/dramas` 契约与卡片到播放/详情页的主链路
- [搜索发现 (Search Discovery)](search-discovery/index.md) — 首页搜索入口延伸出的搜索发现页、搜索结果页、热搜/历史、快捷入口与搜索 API 主链路
- [排行体系 (Ranking)](ranking/index.md) — 搜索发现页排行入口、双层 Tab 榜单浏览、分页、预约拦截与 `play` 路由复用
- [分类浏览 (Classification)](classification/index.md) — 搜索发现页分类入口、固定三维度标签矩阵、`GET /api/dramas/tags`、标签点击复用搜索结果页与 Native / Web 范围边界
- [健康检查 (Health Check)](health-check/index.md) — 后端服务运行状态监控端点
- [数据模型 (Data Models)](data-models/index.md) — 核心数据实体定义、首页卡片字段、排行扩展字段与分类标签契约
- [深链 (Deeplink)](deeplink/index.md) — 自定义 URL Scheme 唤起应用，含 `ranking` / `classification` 等发现链路入口与 Android `player` 兼容
- [播放器](video-player/index.md) — 跨端播放页占位实现、首页 Feed / 排行卡片入口与后端占位接口现状
- [管理平台 (Admin Panel)](admin-panel/index.md) — Web 端内部管理后台，提供短剧/剧集内容管理、用户角色分配、仪表盘概览与 RBAC 三级权限控制
