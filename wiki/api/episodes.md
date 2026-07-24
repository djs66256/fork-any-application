# Episodes API 文档

> 最后更新：2026-07-24

---

## GET /api/episodes/[id]

### 功能简介

获取剧集详情。当前骨架阶段返回 501 Not Implemented。

### 代码文件路径

`backend/src/app/api/episodes/[id]/route.ts:L1`

### path / method

`GET /api/episodes/:id`

### Path Parameters

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | string | 是 | 剧集 UUID |

### Error Code

| 状态码 | 错误码 | 说明 |
|--------|--------|------|
| 200 | — | 成功 |
| 404 | `NOT_FOUND` | 剧集不存在 |
| 501 | `NOT_IMPLEMENTED` | 端点尚未实现 |

---

## 修订历史

| 日期 | 变更摘要 |
|------|---------|
| 2026-07-24 | 初始创建，项目初始化阶段新增 episodes API 端点 |

---

*本文档由 llm-wiki skill 自动维护。*
