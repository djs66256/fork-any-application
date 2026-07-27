# API 文档索引

- [健康检查 (Health)](health.md) — `GET /api/health`，返回服务状态、版本号及 Supabase DB + Redis 连通性
- [短剧 (Dramas)](dramas.md) — `GET /api/dramas`、`GET /api/dramas/search`、`GET /api/dramas/hot-search`、`POST /api/dramas`、`GET /api/dramas/[id]`；其中搜索结果与首页列表共用同一 `Drama` 分页契约，热搜榜为搜索发现页提供 Top 10 关键词数据
- [剧集 (Episodes)](episodes.md) — `GET /api/episodes/[id]`，剧集详情接口（骨架）
- [播放器 (Player)](player.md) — `POST /api/player/start`、`POST /api/player/stop`，播放控制接口（骨架）
