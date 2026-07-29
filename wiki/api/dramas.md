# Dramas API 文档

> 最后更新：2026-07-28

---

## GET /api/dramas

### 功能简介

获取首页信息流短剧列表。当前已从“骨架返回空数组”演进为可分页返回首页卡片数据的列表接口，是 Android / iOS Native 首页 Feed 的唯一数据来源；同时 `tags` 字段也会作为搜索发现与分类承接链路的共享基础字段继续下发（`backend/src/app/api/dramas/route.ts:8-24`、`backend/src/lib/schemas.ts:15-28,68-73`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:32-34`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:421-424`
- Schema：`backend/src/lib/schemas.ts:15-28,61-73`
- 测试：`backend/src/app/api/__tests__/dramas.test.ts:6-92`

### path / method

`GET /api/dramas`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "11111111-1111-1111-1111-000000000001",
      "title": "重生之我在80年代当后妈",
      "description": "穿回八零年代后，她从保姆逆袭成全家团宠。",
      "cover_url": "https://images.example.com/dramas/retro-mom.jpg",
      "category": "年代",
      "episode_count": 68,
      "tags": ["重生", "家庭", "逆袭"],
      "rating": 8.9,
      "created_at": "2026-07-20T10:00:00.000Z",
      "updated_at": "2026-07-25T08:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 12,
    "total_pages": 2
  }
}
```

> 说明：示例响应反映当前代码中的字段形态与分页结构；真实列表由 mock repository 中的预置短剧分页切片得到（`backend/src/repositories/mock/drama.mock.repository.ts:421-424`）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `data` | array | 首页短剧卡片数组 |
| `data[].id` | string | 短剧 UUID |
| `data[].title` | string | 标题 |
| `data[].description` | string | 描述 |
| `data[].cover_url` | string \| null | 封面图 URL |
| `data[].category` | string | 分类 |
| `data[].episode_count` | number | 集数 |
| `data[].tags` | string[] | 标签列表；后续搜索与分类链路会复用该字段 |
| `data[].rating` | number \| null | 评分 |
| `data[].created_at` | string | 创建时间 |
| `data[].updated_at` | string | 更新时间 |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 每页数量（注意响应仍为 snake_case） |
| `pagination.total` | number | 总记录数 |
| `pagination.total_pages` | number | 总页数 |

### 当前行为说明

- 默认请求 `GET /api/dramas` 返回第一页 10 条数据，当前总数为 12，总页数为 2（`backend/src/app/api/__tests__/dramas.test.ts:17-35`）。
- 请求 `GET /api/dramas?page=2&pageSize=10` 返回第 2 页剩余数据（`backend/src/app/api/__tests__/dramas.test.ts:37-59`）。
- 请求超大页码时仍返回 200，但 `data=[]`，分页信息保持正确（`backend/src/app/api/__tests__/dramas.test.ts:61-74`）。
- 当前 Android / iOS 客户端都只消费第一页，未实现首页加载更多；但搜索和排行链路会继续复用同一 `Drama` 基础字段契约。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和大页码空结果） |
| 400 | `VALIDATION_ERROR` | 分页参数非法，例如 `page=0` 或 `pageSize=101` |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/channel

### 功能简介

获取剧场频道 feed 列表。该接口是 Android / iOS 剧场一级频道的唯一数据源，支持 `channel/page/pageSize` 三个 query 参数；当前运行时只有 `channel=all` 返回真实内容，其余 `real / anime / movie / audio / novel / comic / bigscreen` 都返回合法空分页，用于承接剧场子频道空态而不是错误页。响应结构沿用统一的 `{ data, pagination }` 形式，但每个列表项在基础 `Drama` 字段之上额外增加 `heat` 整数字段，供客户端自行格式化显示（`backend/src/app/api/dramas/channel/route.ts:1-17`、`backend/src/lib/schemas.ts:175-194`、`backend/src/repositories/mock/drama.mock.repository.ts`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/channel/route.ts:1-17`
- Service：`backend/src/services/drama/drama.service.ts`
- Repository Contract：`backend/src/repositories/interfaces/drama.repository.interface.ts:14-30,62-66`
- Repository Registry：`backend/src/repositories/repository-registry.ts`
- Mock Repository：`backend/src/repositories/mock/drama.mock.repository.ts`
- Schema：`backend/src/lib/schemas.ts`
- Route 测试：`backend/src/app/api/__tests__/dramas-channel.test.ts:1-139`
- Service 测试：`backend/src/services/drama/drama.service.test.ts:101-133`

### path / method

`GET /api/dramas/channel`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `channel` | enum | 否 | `all` | `all` / `real` / `anime` / `movie` / `audio` / `novel` / `comic` / `bigscreen` |
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 20 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "逆袭归来后我成了豪门团宠",
      "description": "落魄千金重回豪门，在误会与守护中逆风翻盘。",
      "cover_url": "https://example.com/dramas/001.jpg",
      "category": "都市",
      "episode_count": 68,
      "tags": ["逆袭", "豪门"],
      "rating": 8.9,
      "created_at": "2026-07-25T00:00:00Z",
      "updated_at": "2026-07-25T00:00:00Z",
      "heat": 98210
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 1,
    "total_pages": 1
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data[].id` | string | 短剧 UUID |
| `data[].title` | string | 标题 |
| `data[].description` | string | 描述 |
| `data[].cover_url` | string \| null | 封面图 URL |
| `data[].category` | string | 分类 |
| `data[].episode_count` | number | 集数 |
| `data[].tags` | string[] | 标签列表 |
| `data[].rating` | number \| null | 评分 |
| `data[].created_at` | string | 创建时间 |
| `data[].updated_at` | string | 更新时间 |
| `data[].heat` | number | 剧场热度原始整数值；客户端负责格式化显示 |
| `pagination.page` | number | 当前页码 |
| `pagination.page_size` | number | 每页数量 |
| `pagination.total` | number | 总记录数 |
| `pagination.total_pages` | number | 总页数 |

### 当前行为说明

- Route 使用 `TheaterFeedQuerySchema` 解析 query，默认收口为 `channel=all&page=1&pageSize=20`。
- Route 不直接实例化 mock repository，而是通过 `getDramaRepository()` 走 registry 注入，便于测试时替换仓库实现。
- 当前默认仓库中，`channel=all` 会把排行 mock 数据映射为剧场卡片列表，并将 `play_count` 映射为 `heat`。
- `real / anime / movie / audio / novel / comic / bigscreen` 当前都返回 `200 + data=[]` 与 `total_pages=0`，用于承接合法空态。
- 若 repository 返回缺失 `heat` 等关键字段的非法内部数据，Service 会把它包装为 500 `INTERNAL_ERROR`。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含非 `all` 频道空列表） |
| 400 | `VALIDATION_ERROR` | `channel` / `page` / `pageSize` 非法 |
| 500 | `INTERNAL_ERROR` | 服务内部错误或 repository 返回非法结构 |

---

## GET /api/dramas/search

### 功能简介

搜索短剧列表。当前接口继续作为搜索结果页的唯一数据源，同时也是分类标签点击后的结果承接接口。PRD-06 已将命中规则从 `title + category` 扩展为 `title + category + tags`，因此点击分类标签后无需新增独立结果页或独立查询接口（`backend/src/app/api/dramas/search/route.ts:7-19`、`backend/src/services/drama/drama.service.ts:36-45`、`backend/src/repositories/mock/drama.mock.repository.ts:426-439`、`backend/src/repositories/supabase/drama.supabase.repository.ts:259-321`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/search/route.ts:1-20`
- Service：`backend/src/services/drama/drama.service.ts:36-45`
- Repository Contract：`backend/src/repositories/interfaces/drama.repository.interface.ts:17-19,56-59`
- Mock Repository：`backend/src/repositories/mock/drama.mock.repository.ts:426-439`
- Supabase Repository：`backend/src/repositories/supabase/drama.supabase.repository.ts:255-321`
- Schema：`backend/src/lib/schemas.ts:82-88`
- Route 测试：`backend/src/app/api/__tests__/dramas-search.test.ts:34-127`
- Supabase 测试：`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts:98-140`

### path / method

`GET /api/dramas/search`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `q` | string | 是 | — | 搜索词；`trim` 后长度 1~50 |
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440012",
      "title": "天降萌宝总裁爹地别太宠",
      "description": "萌宝助攻下，破镜重圆的爱情再次启动。",
      "cover_url": "https://example.com/dramas/012.jpg",
      "category": "家庭",
      "episode_count": 66,
      "tags": ["萌宝", "破镜重圆"],
      "rating": 8,
      "created_at": "2026-07-24T13:00:00Z",
      "updated_at": "2026-07-24T13:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 1,
    "total_pages": 1
  }
}
```

> 说明：响应结构与 `GET /api/dramas` 完全一致，差异只在于数据集按搜索词过滤（`backend/src/lib/schemas.ts:68-73`、`backend/src/app/api/__tests__/dramas-search.test.ts:39-57`）。

### 当前行为说明

- Route 会先用 `SearchDramaQuerySchema` 对 `q/page/pageSize` 做标准化与校验；例如 `q=%20萌宝%20` 会被清洗为 `萌宝`（`backend/src/app/api/dramas/search/route.ts:8-17`、`backend/src/app/api/__tests__/dramas-search.test.ts:39-57`）。
- Mock repository 当前按标题、分类和 `tags[]` 三路做不区分大小写的包含匹配（`backend/src/repositories/mock/drama.mock.repository.ts:426-435`）。
- Supabase repository 也已将搜索表达式扩展到 `title.ilike`、`category.ilike` 与 `tags.cs` 三路匹配（`backend/src/repositories/supabase/drama.supabase.repository.ts:259-304`，`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts:128-133`）。
- 请求超大页码时返回 200 + 空数组，分页元数据仍保留（`backend/src/app/api/__tests__/dramas-search.test.ts:71-96`）。
- 该接口既服务普通搜索结果页，也服务分类页点击标签后的搜索结果承接；当前没有独立的 `classification/result` API。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和大页码空结果） |
| 400 | `VALIDATION_ERROR` | `q` 为空白、过长，或分页参数非法 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/hot-search

### 功能简介

获取搜索发现页的热搜词列表。当前返回固定不超过 10 条的静态项，用于搜索发现页首屏热词展示；本期未引入真实搜索统计或实时榜单来源（`backend/src/app/api/dramas/hot-search/route.ts:6-11`、`backend/src/services/drama/drama.service.ts:73-81`、`backend/src/lib/schemas.ts:162-174`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/hot-search/route.ts:1-12`
- Service：`backend/src/services/drama/drama.service.ts:73-81`
- Repository：`backend/src/repositories/mock/drama.mock.repository.ts:464-468`
- Schema：`backend/src/lib/schemas.ts:162-174`
- 测试：`backend/src/app/api/__tests__/dramas-hot-search.test.ts:18-44`

### path / method

`GET /api/dramas/hot-search`

### Response

```json
{
  "data": [
    { "rank": 1, "keyword": "逆袭", "score": 9821 },
    { "rank": 2, "keyword": "豪门", "score": 9540 }
  ]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data[].rank` | number | 排名，int，min 1 |
| `data[].keyword` | string | 热搜词，`trim` 后长度 1~50 |
| `data[].score` | number | 热度分值，int，min 0 |

### 当前行为说明

- 当前 Route 不接收 query 参数，直接返回 Service 透传的数据（`backend/src/app/api/dramas/hot-search/route.ts:6-11`）。
- 返回项上限为 10；mock / supabase repository 当前都使用固定静态数据集（`backend/src/lib/schemas.ts:170-171`、`backend/src/repositories/mock/drama.mock.repository.ts:464-468`、`backend/src/repositories/supabase/drama.supabase.repository.ts:398-401`）。
- 当前热门词与分类标签是两套独立来源：热搜用于搜索发现首屏，分类 tags 用于分类页维度浏览。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/tags

### 功能简介

获取分类浏览页的标签矩阵。该接口是 Android / iOS 分类页的唯一数据源，返回固定三维度——`时代背景`、`主题情节`、`角色设定`——并支持 `all / male / female` 三种性别视图。`all` 视图会按 `male` 在前、`female` 在后的顺序去重合并标签；即使某维度当前为空，也不会省略该维度（`backend/src/app/api/dramas/tags/route.ts:7-18`、`backend/src/lib/schemas.ts:99-152`、`backend/src/repositories/mock/drama.mock.repository.ts:249-289,387-446`）。

### 代码文件路径

- Route：`backend/src/app/api/dramas/tags/route.ts:1-18`
- Service：`backend/src/services/drama/drama.service.ts:47-57`
- Repository Contract：`backend/src/repositories/interfaces/drama.repository.interface.ts:26-33,56-60`
- Mock Repository：`backend/src/repositories/mock/drama.mock.repository.ts:249-289,387-446`
- Supabase Repository：`backend/src/repositories/supabase/drama.supabase.repository.ts:71-160,323-328`
- Schema：`backend/src/lib/schemas.ts:99-152`
- Supabase 测试：`backend/src/repositories/supabase/__tests__/drama.supabase.repository.test.ts:169-181`

### path / method

`GET /api/dramas/tags`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `gender` | enum | 否 | `all` | `all` / `male` / `female` |

### Response

```json
{
  "data": {
    "gender": "all",
    "dimensions": [
      {
        "key": "era_background",
        "name": "时代背景",
        "tags": ["都市", "古风", "年代", "校园", "豪门"]
      },
      {
        "key": "theme_plot",
        "name": "主题情节",
        "tags": ["逆袭", "系统", "复仇", "甜宠", "穿书", "重生"]
      },
      {
        "key": "character_setting",
        "name": "角色设定",
        "tags": ["总裁", "萌宝"]
      }
    ]
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.gender` | enum | 当前返回的性别视图 |
| `data.dimensions` | array | 固定 3 个维度，顺序不可变 |
| `data.dimensions[].key` | enum | `era_background` / `theme_plot` / `character_setting` |
| `data.dimensions[].name` | string | 维度展示名 |
| `data.dimensions[].tags` | string[] | 当前维度标签列表，可为空数组 |

### 当前行为说明

- Route 会用 `ClassificationTagsQuerySchema` 解析 query，默认值为 `gender=all`（`backend/src/app/api/dramas/tags/route.ts:8-11`、`backend/src/lib/schemas.ts:107-111`）。
- `ClassificationDimensionsSchema` 强制返回数组长度等于 3，且维度顺序必须和 `CLASSIFICATION_DIMENSION_KEYS` 保持一致（`backend/src/lib/schemas.ts:121-144`）。
- `all` 视图通过 `mergeUniqueTags(primary, secondary)` 先拼 male，再拼 female，按出现顺序去重（`backend/src/repositories/mock/drama.mock.repository.ts:371-397`、`backend/src/repositories/supabase/drama.supabase.repository.ts:121-147`）。
- 当前运行时 Route 仍直接实例化 `DramaMockRepository()`；Supabase repository 已补齐同等 classification 能力，但尚未切换为实际运行时数据源（`backend/src/app/api/dramas/tags/route.ts:13-16`、`backend/src/repositories/supabase/drama.supabase.repository.ts:323-328`）。
- 该接口只提供分类页标签矩阵；点击标签后的结果仍统一走 `GET /api/dramas/search`。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 400 | `VALIDATION_ERROR` | `gender` 非法 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## GET /api/dramas/rankings

### 功能简介

获取排行页榜单列表。当前接口是 Android / iOS 排行页的唯一数据来源，支持内容类型与榜单类型双维度筛选，以及标准分页返回。

### 代码文件路径

- Route：`backend/src/app/api/dramas/rankings/route.ts:8-24`
- Service：`backend/src/services/drama/drama.service.ts:59-71`
- Repository Contract：`backend/src/repositories/interfaces/drama.repository.interface.ts:21-24,59-61`
- Mock Repository：`backend/src/repositories/mock/drama.mock.repository.ts:449-462`
- Schema：`backend/src/lib/schemas.ts:30-44,75-80,90-97`
- 测试：`backend/src/app/api/__tests__/dramas-rankings.test.ts:39-138`

### path / method

`GET /api/dramas/rankings`

### Query 参数

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `type` | enum | 否 | `hot` | 榜单类型：`hot` / `recommend` / `booking` |
| `contentType` | enum | 否 | `all` | 内容类型：`all` / `live_action` / `ai` |
| `page` | number | 否 | 1 | 页码（int，min 1） |
| `pageSize` | number | 否 | 10 | 每页数量（int，min 1，max 100） |

### Response

```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "title": "逆袭归来后我成了豪门团宠",
      "description": "落魄千金重回豪门，在误会与守护中逆风翻盘。",
      "cover_url": "https://example.com/dramas/001.jpg",
      "category": "都市",
      "episode_count": 68,
      "tags": ["逆袭", "豪门"],
      "rating": 8.9,
      "created_at": "2026-07-25T00:00:00Z",
      "updated_at": "2026-07-25T00:00:00Z",
      "content_type": "live_action",
      "play_count": 98210,
      "booking_count": 820,
      "recommendation_score": 58930.6,
      "is_booked": false
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 10,
    "total": 12,
    "total_pages": 2
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `data[].content_type` | enum | 内容类型：`live_action` / `ai` |
| `data[].play_count` | number | 热榜排序与展示的热度代理值 |
| `data[].booking_count` | number | 预约榜排序与展示的预约数 |
| `data[].recommendation_score` | number | 推荐榜排序与展示的推荐值 |
| `data[].is_booked` | boolean | 当前用户是否已预约；匿名请求固定返回 `false` |
| `pagination.*` | object | 与 `GET /api/dramas` 相同的统一分页结构 |

### 当前行为说明

- 默认请求 `GET /api/dramas/rankings` 会被解析为 `type=hot&contentType=all&page=1&pageSize=10`。
- Route 当前已切换为 `DramaSupabaseRepository()`，不再使用早期的 `DramaMockRepository()` 运行时数据源（`backend/src/app/api/dramas/rankings/route.ts:17-22`）。
- Route 会通过 `resolveOptionalAuthContext(request)` 解析 bearer access token；未登录请求仍允许访问排行列表，但 `is_booked` 固定为 `false`（`backend/src/app/api/dramas/rankings/route.ts:19-22`、`backend/src/middleware/auth.ts:65-67`）。
- 当前可选鉴权只接受真实 `Authorization: Bearer <accessToken>` 语义：本地测试 token 会走 local auth session 校验，正式 token 会走 `supabase.auth.getUser(token)` 校验；旧的 `x-user-id` / `Bearer <user-id>` 已不再是当前 contract（`backend/src/middleware/auth.ts:27-63`）。
- 超大页码返回 200 + 空数组，不视为错误。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功（含空列表和大页码空结果） |
| 400 | `VALIDATION_ERROR` | `type` / `contentType` / 分页参数非法 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/dramas/[id]/book

### 功能简介

提交短剧预约。该接口服务于预约榜交互，当前已要求调用方提供真实登录态 bearer access token；成功后返回单向幂等的 `booked: true` 结果和最新预约数。

### 代码文件路径

- Route：`backend/src/app/api/dramas/[id]/book/route.ts:16-28`
- Auth：`backend/src/middleware/auth.ts:69-103,132-138`
- Service：`backend/src/services/drama/drama.service.ts:84-92`
- Repository：`backend/src/repositories/supabase/drama.supabase.repository.ts`
- Schema：`backend/src/lib/schemas.ts:154-160`
- 测试：`backend/src/app/api/__tests__/dramas-book.test.ts:18-133`

### path / method

`POST /api/dramas/:id/book`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### Headers

| 字段 | 必填 | 说明 |
|------|------|------|
| `Authorization` | 是 | `Bearer <accessToken>`；本地测试 token 走 local auth session，正式 token 走 Supabase `getUser(token)` 校验 |

> 说明：本接口当前通过 `requireAuthContext()` 强制要求真实登录态；缺失 token、fake token、过期 token 都会返回 401 `AUTH_UNAUTHORIZED`。

### Response

```json
{
  "drama_id": "550e8400-e29b-41d4-a716-446655440001",
  "booked": true,
  "booking_count": 821
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `drama_id` | string | 被预约的短剧 UUID |
| `booked` | literal `true` | 当前版本只支持“预约成功 / 已预约”语义，不支持取消 |
| `booking_count` | number | 预约后的最新预约数 |

### 当前行为说明

- Route 当前已通过 `requireAuthContext()` 完成鉴权，并从 `getAuth(request)` 提取 `userId`；预约接口不再接受 `x-user-id` 或伪造 bearer userId（`backend/src/app/api/dramas/[id]/book/route.ts:16-25`、`backend/src/middleware/auth.ts:69-103,132-138`）。
- 若同一用户重复预约同一短剧，接口保持幂等 success：返回 `booked: true`，但不会再次增加 `booking_count`。
- 当前运行时数据源已经切换为 `DramaSupabaseRepository()`；预约数与 `is_booked` 由真实 repository 路径负责，而不是旧的 mock 内存状态（`backend/src/app/api/dramas/[id]/book/route.ts:20-27`）。
- 若 `id` 不存在，则返回 404 `NOT_FOUND`。

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 预约成功或重复预约幂等成功 |
| 400 | `VALIDATION_ERROR` | `id` 不是合法 UUID |
| 401 | `AUTH_UNAUTHORIZED` | 未登录、token 非法或 token 已失效 |
| 404 | `NOT_FOUND` | 短剧不存在 |
| 500 | `INTERNAL_ERROR` | 服务内部错误 |

---

## POST /api/dramas

### 功能简介

创建短剧。当前仍为占位接口，返回 501 Not Implemented，不属于当前搜索发现 / 排行 / 分类体系范围。

### 代码文件路径

`backend/src/app/api/dramas/route.ts:26-28`

### path / method

`POST /api/dramas`

### Response (501)

```json
{
  "error": {
    "code": "NOT_IMPLEMENTED",
    "message": "POST /api/dramas not implemented"
  }
}
```

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## GET /api/dramas/[id]

### 功能简介

获取短剧详情。当前仍为 501 占位接口；首页、搜索、排行与分类链路都只复用客户端既有占位路由，不依赖该接口。

### 代码文件路径

`backend/src/app/api/dramas/[id]/route.ts:1-6`

### path / method

`GET /api/dramas/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 短剧 UUID |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-28 | 更新：新增 `GET /api/dramas/channel` 文档，补充剧场 feed query、`heat` 字段、非 `all` 频道合法空态、repository registry 注入与服务端内部数据校验语义 |
| 2026-07-27 | 更新：新增 `GET /api/dramas/search`、`GET /api/dramas/hot-search`、`GET /api/dramas/tags` 文档，补充 `tags` 字段在首页/搜索/分类中的共享事实，并将搜索命中规则明确为 `title + category + tags` |
| 2026-07-29 | 更新：同步 PRD-08 登录闭环后排行与预约接口的真实认证语义，修正 rankings 的可选 bearer 校验、book 的 `requireAuthContext()`、`AUTH_UNAUTHORIZED` 错误码，以及运行时 repository 已切换到 `DramaSupabaseRepository()` |
| 2026-07-27 | 更新：新增 `GET /api/dramas/rankings` 与 `POST /api/dramas/:id/book` 文档，补充排行 query、扩展字段、可选 auth 上下文、预约幂等行为与当前骨架态认证约束 |
| 2026-07-26 | 更新：`GET /api/dramas` 从空骨架修正为首页 Feed 列表接口，补充 canonical query、首页卡片字段、mock 数据分页行为与 `VALIDATION_ERROR` 校验错误码 |
| 2026-07-24 | 初始创建，项目初始化阶段新增 3 个 dramas API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*