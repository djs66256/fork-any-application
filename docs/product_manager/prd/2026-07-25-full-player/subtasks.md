# 子任务拆分：完整观看播放器

> 关联 PRD：[prd.md](prd.md)
> 创建日期：2026-07-25
> 状态：草稿

---

## 工时总览

| 平台 | 子任务数 | 总工时（人日） | 备注 |
|------|---------|--------------|------|
| Backend | 2 | 3 人日 | |
| iOS | 3 | 5 人日 | |
| Android | 3 | 5 人日 | |
| **合计** | **8** | **13 人日** | |

---

## 迭代规划

| 迭代 | 目标 | 包含子任务 | 交付物 |
|------|------|-----------|--------|
| Sprint 1 | 核心播放 + 选集 | ST-01, ST-02, ST-03, ST-05 | 可播放视频、切换集数、调整倍速 |
| Sprint 2 | 互动栏 + 续播 | ST-04, ST-06, ST-07, ST-08 | 互动栏、断点续播、信息展示 |

---

## 子任务详情

### ST-01：Backend 剧集列表 + 播放扩充 API

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-01, US-03 |
| **平台** | Backend |
| **优先级** | P0 |
| **预估工时** | 2 人日 |
| **前置依赖** | 无 |
| **迭代** | Sprint 1 |

#### 工作内容

1. 实现 `GET /api/dramas/:id/episodes`：返回该剧集全部剧集列表（按 episode_number 排序）
2. 扩充 `POST /api/player/start`：返回字段增加 `start_time`（断点续播位置）
3. 扩充 `POST /api/player/stop`：接收 `progress` 字段保存播放进度
4. 种子数据：为已有 Drama 生成 3-5 集 Episode 数据

#### 完成标准

- [ ] `GET /api/dramas/:id/episodes` 正确返回剧集列表
- [ ] `POST /api/player/start` 支持断点续播参数
- [ ] `POST /api/player/stop` 正确保存进度

#### 涉及 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dramas/:id/episodes` | 获取剧集列表 |
| POST | `/api/player/start` | 开始播放（扩充） |
| POST | `/api/player/stop` | 停止播放（扩充） |

---

### ST-02：Backend 播放进度管理

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-04 |
| **平台** | Backend |
| **优先级** | P1 |
| **预估工时** | 1 人日 |
| **前置依赖** | ST-01 |
| **迭代** | Sprint 2 |

#### 工作内容

1. 创建 `playback_history` 表：user_id + drama_id + episode_id + progress + updated_at
2. `GET /api/player/progress?dramaId=xxx&episodeId=xxx`：查询续播位置
3. `POST /api/player/stop` 自动写入 playback_history

#### 完成标准

- [ ] 播放历史正确存储和查询
- [ ] `start` API 返回正确的续播位置

#### 涉及 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/player/progress` | 查询续播位置 |

---

### ST-03：iOS 播放器核心 UI

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-01, US-02, US-03 |
| **平台** | iOS |
| **优先级** | P0 |
| **预估工时** | 2.5 人日 |
| **前置依赖** | ST-01, PRD-01 ST-02（路由框架） |
| **迭代** | Sprint 1 |

#### 工作内容

1. 实现 `PlayerPage`：接收 `videoId`（dramaId），请求剧集列表
2. 集成视频播放器（AVPlayer / AVKit `VideoPlayer`）
3. 顶部栏：← 返回（pop）、集数文本、倍速按钮（弹出 action sheet）、⋯ 更多按钮
4. 倍速面板（`.confirmationDialog` 或自定义 bottom sheet）：0.5x~2.0x 七档
5. 底部固定选集栏：「选集 · 状态 · 全 N 集」，点击弹出选集面板
6. 选集面板（`.sheet`）：`List` 展示所有剧集，当前集高亮，点击切换

#### 完成标准

- [ ] 视频能正常播放/暂停
- [ ] 点击倍速切换播放速度
- [ ] 选集面板弹出，点击切换剧集
- [ ] 返回按钮正常工作

#### 涉及 UI/页面

| 页面/组件 | 说明 | 涉及端 |
|----------|------|--------|
| PlayerPage | 播放页主视图 | iOS |
| SpeedPicker | 倍速选择面板 | iOS |
| EpisodePicker | 选集面板 | iOS |

---

### ST-04：iOS 播放器互动栏 + 续播

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-04, US-05, US-06 |
| **平台** | iOS |
| **优先级** | P1 |
| **预估工时** | 2.5 人日 |
| **前置依赖** | ST-03, ST-02 |
| **迭代** | Sprint 2 |

#### 工作内容

1. 右侧互动栏：`VStack` 纵向排列（❤️点赞+数 / ⭐收藏+数 / 💬评论数 / 📤分享）
2. 底部信息区：剧集标题 + 标签
3. 断点续播：进入播放页时检查 `start_time`，从指定位置开始播放
4. 退出时上报播放进度（`onDisappear` → `POST /api/player/stop`）

#### 完成标准

- [ ] 互动栏正确渲染，点赞/收藏可点击切换
- [ ] 退出再进入同一集从上次位置续播
- [ ] 底部信息区展示正确

---

### ST-05：Android 播放器核心 UI

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-01, US-02, US-03 |
| **平台** | Android |
| **优先级** | P0 |
| **预估工时** | 2.5 人日 |
| **前置依赖** | ST-01, PRD-01 ST-05（路由框架） |
| **迭代** | Sprint 1 |

#### 工作内容

1. 实现 `PlayerScreen`：接收 `videoId`，请求剧集列表
2. 集成 ExoPlayer 或 Media3 `PlayerView`
3. 顶部栏：返回（`popBackStack`）、集数、倍速按钮（`DropdownMenu`）、更多按钮
4. 倍速面板：`ModalBottomSheet` 七档选择
5. 底部固定选集栏：`Row` 展示「选集 · 状态 · 全 N 集」，点击弹出选集面板
6. 选集面板：`LazyColumn` 所有剧集，当前集高亮

#### 完成标准

- [ ] 视频能正常播放/暂停
- [ ] 倍速切换正常
- [ ] 选集面板可切换剧集
- [ ] 返回按钮正常

#### 涉及 UI/页面

| 页面/组件 | 说明 | 涉及端 |
|----------|------|--------|
| PlayerScreen | 播放页主 Composable | Android |
| SpeedPickerSheet | 倍速选择面板 | Android |
| EpisodePickerSheet | 选集面板 | Android |

---

### ST-06：Android 播放器互动栏 + 续播

| 属性 | 值 |
|------|-----|
| **对应 PRD** | US-04, US-05, US-06 |
| **平台** | Android |
| **优先级** | P1 |
| **预估工时** | 2.5 人日 |
| **前置依赖** | ST-05, ST-02 |
| **迭代** | Sprint 2 |

#### 工作内容

1. 右侧互动栏：`Column` 纵向排列（❤️/⭐/💬/📤）
2. 底部信息区
3. 断点续播逻辑
4. 退出上报进度

#### 完成标准

- [ ] 同 ST-04 iOS 验收标准

---

## 子任务依赖图

```mermaid
flowchart TD
    ST-01[ST-01: Backend 剧集+播放 API] --> ST-02[ST-02: Backend 播放进度]
    ST-01 --> ST-03[ST-03: iOS 播放器核心]
    ST-01 --> ST-05[ST-05: Android 播放器核心]
    ST-03 --> ST-04[ST-04: iOS 互动栏+续播]
    ST-05 --> ST-06[ST-06: Android 互动栏+续播]
    ST-02 --> ST-04
    ST-02 --> ST-06
```
> 💡 跨 PRD 依赖：
> - ST-03 依赖 PRD-01: ST-02（iOS NavigationStack 路由框架）
> - ST-05 依赖 PRD-01: ST-05（Android NavHost 路由框架）
```

---

## 变更历史

| 日期 | 变更内容 | 变更原因 |
|------|---------|---------|
| 2026-07-25 | 初始版本 | PRD-03 |
