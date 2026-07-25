# 子任务拆分：剧场频道

> 关联 PRD：[prd.md](prd.md)
> 创建日期：2026-07-25 | 状态：草稿

---

## 工时总览

| 平台 | 子任务数 | 总工时（人日） |
|------|---------|--------------|
| Backend | 1 | 2 人日 |
| iOS | 2 | 5 人日 |
| Android | 2 | 5 人日 |
| **合计** | **5** | **12 人日** |

---

## 子任务详情

### ST-01：Backend 剧场频道 API

- 工时：2 人日 | P0
- `GET /api/dramas/channel?channel=xxx&page=1`：按频道类型返回双列 Feed 数据
- channel 枚举值：`all / real / anime / movie / audio / novel / comic / bigscreen`
- Response 格式：
  ```json
  {
    "data": [
      { "id": "drama_id", "title": "剧名", "cover_url": "https://...", "heat": "2.3万", "tags": ["爆剧", "都市"], "category": "real" }
    ],
    "pagination": { "page": 1, "page_size": 20, "total": 150, "total_pages": 8 }
  }
  ```

### ST-02：iOS 剧场频道页 UI

- 工时：3.5 人日 | P0
- 频道页：顶部搜索+识图占位、横向滚动子频道Tab（8 个 Tab 全部展示，仅找剧 Tab 有数据，其它 Tab 展示空态）、4个快捷入口、双列瀑布流卡片（`LazyVGrid` 2列）
- 搜索框点击 → 跳转 `/search` 搜索页

### ST-03：Android 剧场频道页 UI

- 工时：3.5 人日 | P0
- 频道页：顶部搜索+识图占位、横向滚动子频道Tab（使用 `TabRow` + `HorizontalPager`，8 个 Tab 全部展示，仅找剧 Tab 有数据，其它 Tab 展示空态）、4个快捷入口、双列瀑布流卡片（`LazyVerticalStaggeredGrid` 2列）
- 搜索框点击 → 跳转 `/search` 搜索页

### ST-04：iOS 快捷入口跳转

- 工时：1.5 人日 | P1
- 快捷入口跳转：新剧 → Toast "功能开发中"，筛选 → 跳转分类页，排行 → 排行页，预约 → 排行页预约榜 Tab

### ST-05：Android 快捷入口跳转

- 工时：1.5 人日 | P1
- 快捷入口跳转：新剧 → Toast "功能开发中"，筛选 → 跳转分类页，排行 → 排行页，预约 → 排行页预约榜 Tab

---

## 变更历史

| 日期 | 变更内容 |
|------|---------|
| 2026-07-25 | 初始版本 |
| 2026-07-25 | 审查修正：工时调整 10.5→12，ST-01 补充 API 规格，ST-03 补充描述，ST-04/ST-05 补充新剧/筛选入口 | PRD-12 审查反馈 |
