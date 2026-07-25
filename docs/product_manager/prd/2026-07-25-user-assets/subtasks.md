# 子任务拆分：个人资产管理

> 关联 PRD：[prd.md](prd.md)
> 创建日期：2026-07-25 | 状态：草稿

---

## 工时总览

| 平台 | 子任务数 | 总工时（人日） |
|------|---------|--------------|
| Backend | 1 | 1 人日 |
| iOS | 1 | 1.5 人日 |
| Android | 1 | 1.5 人日 |
| **合计** | **3** | **4 人日** |

---

## 子任务详情

### ST-01：Backend 预约列表 API

- 工时：1 人日 | P0
- `GET /api/user/bookings`：返回用户预约列表，按状态分组（已上线/待上线）

**API 规格**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| status | string | 否 | — | 过滤：online / upcoming，不传返回全部 |
| page | number | 否 | 1 | 页码 |
| pageSize | number | 否 | 20 | 每页数量 |

**Response**：
```json
{
  "data": [
    {
      "id": "uuid",
      "drama_id": "uuid",
      "title": "短剧标题",
      "cover_url": "https://...",
      "status": "online | upcoming",
      "booked_at": "ISO 8601",
      "episode_count": 80
    }
  ],
  "pagination": {
    "page": 1,
    "page_size": 20,
    "total": 0,
    "total_pages": 0
  }
}
```

需登录（未登录返回 401）。

### ST-02：iOS 我的预约页

- 工时：1.5 人日 | P0
- 预约管理页：顶部 Tab（已上线/待上线）+ 列表（封面+标题+状态），空态展示，编辑按钮
- 实现「我的下载」入口占位（列表底部或独立行，点击 Toast "功能开发中"）

### ST-03：Android 我的预约页

- 工时：1.5 人日 | P0
- 预约管理页：顶部 Tab（已上线/待上线）+ 列表（封面+标题+状态）
- 使用 `TabLayout` + `ViewPager2` 或 `TabRow` + `HorizontalPager`
- 列表使用 `LazyColumn`（封面图 Coil + 标题 + 状态标签）
- 空态展示（插画 + "暂无预约" 文案）
- 编辑按钮（右上角，点击进入编辑模式可取消预约）
- 实现「我的下载」入口占位（列表底部或独立行，点击 Toast "功能开发中"）

---

## 变更历史

| 日期 | 变更内容 |
|------|---------|
| 2026-07-25 | 初始版本 |
