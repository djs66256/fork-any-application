# 功能域索引

- [应用壳 (App Shell)](app-shell/index.md) — 各端应用启动入口、移动端 5 Tab 容器，以及首页频道承载 Feed 与排行等 Native 子页面的关系
- [首页信息流 (Homepage Feed)](homepage-feed/index.md) — Android / iOS 首页首屏 Feed、Backend `GET /api/dramas` 契约与卡片到播放/详情页的主链路
- [排行体系 (Ranking)](ranking/index.md) — 搜索发现页排行入口、双层 Tab 榜单浏览、分页、预约拦截与 `play` 路由复用
- [健康检查 (Health Check)](health-check/index.md) — 后端服务运行状态监控端点
- [数据模型 (Data Models)](data-models/index.md) — 核心数据实体定义、首页卡片字段约束与排行扩展字段
- [深链 (Deeplink)](deeplink/index.md) — 自定义 URL Scheme 唤起应用，含 `ranking` 入口与 Android `player` 兼容
- [播放器](video-player/index.md) — 跨端播放页占位实现、首页 Feed / 排行卡片入口与后端占位接口现状
