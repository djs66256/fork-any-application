# 子任务拆分：赚钱中心

> 关联 PRD：[prd.md](prd.md)
> 创建日期：2026-07-25 | 状态：草稿

---

## 工时总览

| 平台 | 子任务数 | 总工时（人日） |
|------|---------|--------------|
| Backend | 1 | 2.5 人日 |
| iOS | 2 | 5 人日 |
| Android | 2 | 5 人日 |
| **合计** | **5** | **12.5 人日** |

备注：backlog 原估 14 人日含提现等范围外功能，本迭代 12.5 人日。

---

## 子任务详情

### ST-01：Backend 任务与奖励 API

- 工时：2.5 人日 | P0
- `GET /api/earn/overview`（收益总览+任务列表）、`POST /api/earn/complete-task`（标记任务完成）、种子数据预设虚拟金额和新手任务

**GET /api/earn/overview**

Response：
```json
{
  "coins": 0,
  "new_user_task": { "title": "新人7天保底6元", "reward": 600, "status": "available" },
  "daily_rewards": [{ "day": 1, "coins": 10, "claimed": false }],
  "cash_tasks": [{ "id": "uuid", "title": "看剧领现金", "reward_coins": 500, "description": "观看完整短剧获得奖励", "status": "available" }]
}
```

**POST /api/earn/complete-task**

Request：
```json
{
  "task_id": "uuid"
}
```

Response：
```json
{
  "success": true,
  "coins_earned": 500,
  "total_coins": 500
}
```

签到 API（PRD-10 `POST /api/signin/checkin`）与赚钱中心 API 独立，首版不互通。

### ST-02：iOS 赚钱中心页 UI

- 工时：3 人日 | P0
- 收益头图（虚拟金额展示+登录入口）、新手任务卡片、连续看剧福利网格、现金任务列表

### ST-03：Android 赚钱中心页 UI

- 工时：3 人日 | P0
- 收益头图（虚拟金额展示+登录入口）、新手任务卡片（「新人7天保底6元」+「立即领取」按钮）、连续看剧福利网格（每日奖励卡片，LazyVerticalGrid）、现金任务列表（任务卡片+奖励金额）

### ST-04：iOS 奖励跳转播放页

- 工时：2 人日 | P1
- 「立即领取」→ 跳转播放页，携带任务上下文；播放完成回调 → `POST /api/earn/complete-task`

### ST-05：Android 奖励跳转播放页

- 工时：2 人日 | P1
- 同 ST-04，Android 端实现

---

## 变更历史

| 日期 | 变更内容 |
|------|---------|
| 2026-07-25 | 审查修正：ST-03 补充 Android 描述、ST-01 补充 API 规格（complete-task 端点+数据模型）、工时调整 10→12.5 人日 |
| 2026-07-25 | 初始版本 |
