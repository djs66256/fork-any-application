# API 文档索引

- [健康检查 (Health)](health.md) — `GET /api/health`，返回服务状态、版本号及 Supabase DB + Redis 连通性
- [短剧 (Dramas)](dramas.md) — `GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags`、`GET /api/dramas/rankings`、`POST /api/dramas/:id/book`、`POST /api/dramas`、`GET /api/dramas/[id]`；其中首页 Feed、搜索发现、排行、分类标签与预约接口均已形成可消费契约
- [剧集 (Episodes)](episodes.md) — `GET /api/episodes/[id]`，剧集详情接口（骨架）
- [播放器 (Player)](player.md) — `POST /api/player/start`、`POST /api/player/stop`，播放控制接口（骨架）
