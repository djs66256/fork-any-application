# QA 黑盒测试文档

> 需求：PRD-03 完整观看播放器
> 版本：2026-07-26（当前主仓工作区）
> 撰写日期：2026-07-26
> 撰写人：QA subagent (AI)

---

## 一、测试范围

### 1.1 测试目标

验证 PRD-03「完整观看播放器」在 Backend / iOS / Android 三端的首版能力是否满足 shared spec/design 约束，重点覆盖以下黑盒验收链路：

- 从 `play/:id` / `player/:id` 进入 Native 播放器
- 固定 bootstrap 顺序：`GET /api/player/progress` → `GET /api/dramas/:id/episodes` → 目标集解析 → `POST /api/player/start`
- 匿名续播 header `X-Playback-Session-Id` 的透传范围与恢复语义
- 倍速切换、切集、切后台 / 退出时 `POST /api/player/stop` 的 best-effort 上报
- 无资源、错误态、路由兼容、沉浸式导航隐藏等边界行为

> 说明：当前会话未提供 device/simulator 测试 skill，因此本次仅产出黑盒测试文档并记录“设备执行跳过”结果；跳过原因符合 `feature-workflow` 的 `qa-blackbox-testing` 阶段规则。

### 1.2 涉及平台

| 平台 | 是否涉及 | 说明 |
|------|---------|------|
| Backend | ✅ | 提供 `GET /api/player/progress`、`GET /api/dramas/:id/episodes`、`POST /api/player/start`、`POST /api/player/stop` |
| iOS | ✅ | Native 播放页、倍速、选集、断点续播、沉浸式承载 |
| Android | ✅ | Native 播放页、倍速、选集、断点续播、`play/player` 路由兼容 |
| Web | ❌ | 本期不做 Web 播放器 |

### 1.3 不测试的内容

- Web 播放器：本期范围外。
- `mall` / `earn` H5 页面：本期范围外。
- 评论 / 分享 / 收藏后端持久化：首版只要求 UI 承载与本地反馈。
- 真机 / 模拟器实际点击执行：当前会话无可用 device/simulator skill，按 workflow 规则跳过设备执行。
- 外部真实内容分发、DRM、广告、投屏、下载、字幕、画质切换：本期范围外。

---

## 二、测试环境

| 项目 | 要求 |
|------|------|
| 设备型号 | N/A（当前会话无 device/simulator skill） |
| OS 版本 | N/A（当前会话无 device/simulator skill） |
| App 版本 | 当前主仓工作区 `master` 分支上的 PRD-03 实现快照 |
| 网络环境 | 本地开发 / 自动化验证环境 |
| 账号权限 | 匿名续播场景，无登录前置 |
| 其他依赖 | 参考已完成自动化验证：Backend 路由/服务测试、Android `assembleDebug` + `detekt` + `:app:testDebugUnitTest`、iOS `xcodebuild build/test` + `swiftlint lint` |

---

## 三、测试用例

每个用例包含测试定义和测试执行两部分。当前会话因缺少 device/simulator skill，**执行结果统一记录为跳过**，并在备注中附上已完成的自动化验证映射，供后续真机 / 模拟器黑盒执行复用。

### 3.1 功能测试

#### QA-F-001：播放器主链路可从播放路由进入并完成 bootstrap

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **前置条件** | 首页 Feed 或 deeplink 可进入 `play/:id`；目标 drama 存在至少 1 条可播放 Episode |
| **测试步骤** | 1. 从首页 Feed 或 deeplink 进入 `play/:id`。2. 观察播放器页面进入 loading。3. 校验客户端先请求 `GET /api/player/progress?dramaId=...`。4. 再请求 `GET /api/dramas/:id/episodes`。5. 客户端确定目标集后请求 `POST /api/player/start`。6. 页面进入可播放态并展示顶部栏、互动栏、底部信息区、选集栏。 |
| **预期结果** | 播放器按固定 bootstrap 顺序初始化；可播放态下页面不再停留在占位页；播放页隐藏底部导航 / Tab Bar。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实黑盒点击链路。 |
| **备注** | 已有自动化佐证：Backend `player.progress/player.start/drama-episodes` 路由测试，Android `PlayerViewModelTest` / `NavGraphTest`，iOS `PlayerViewModelTests` 与 `PlayerView` 沉浸式视图实现。 |

---

#### QA-F-002：倍速面板支持 7 档倍速且切集后沿用当前会话倍速

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | iOS / Android |
| **前置条件** | 当前集已进入可播放态 |
| **测试步骤** | 1. 进入播放器。2. 点击顶部“倍速”。3. 依次观察 `0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 1.75x / 2.0x` 选项。4. 选择 `1.5x`。5. 打开选集面板并切到下一集。6. 返回播放页确认当前倍速。 |
| **预期结果** | 倍速面板展示 7 档选项；当前值有高亮反馈；切集后仍沿用当前会话倍速，不重置为默认值。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实 UI 黑盒操作。 |
| **备注** | 已有自动化佐证：Android `PlayerViewModelTest` 覆盖 speed keep；iOS `PlayerViewModelTests` 覆盖倍速切换与切集保留。 |

---

#### QA-F-003：选集面板支持高亮当前集并在切集时执行 stop → start（新集从 0 秒开始）

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **前置条件** | drama 至少有 2 集可播放内容；当前已在第 1 集播放一段时间 |
| **测试步骤** | 1. 打开选集面板。2. 观察当前集高亮。3. 点击另一条可播放 Episode。4. 观察旧集停止并上报 `POST /api/player/stop`。5. 观察新集调用 `POST /api/player/start(progress=0)`。 |
| **预期结果** | 选集面板按 `episode_number` 正序展示；当前集高亮；切集时先 stop 后 start；新集从 0 秒开始，不额外恢复历史进度。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实黑盒切集。 |
| **备注** | 已有自动化佐证：Android `PlayerViewModelTest`、iOS `PlayerViewModelTests` 明确断言切集调用顺序与 `progress=0`。 |

---

#### QA-F-004：断点续播通过匿名会话在同一 drama 维度恢复最近一次 `episode_id + start_time`

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Backend / iOS / Android |
| **前置条件** | 已在某 drama 某一集播放一段时间并成功上报过 `POST /api/player/stop` |
| **测试步骤** | 1. 播放第 N 集并停在非 0 秒位置。2. 退出播放器。3. 再次进入同一 drama。4. 观察先请求 `GET /api/player/progress?dramaId=...` 且携带 `X-Playback-Session-Id`。5. 客户端解析恢复集并调用 `POST /api/player/start(progress=start_time)`。 |
| **预期结果** | 同一匿名会话可恢复最近一次观看的 `episode_id + start_time`；若恢复集无效则回退默认可播集。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实黑盒续播恢复。 |
| **备注** | 已有自动化佐证：Backend `player.progress/player.stop` 路由/服务测试；Android / iOS `loadIfNeeded` 恢复场景测试。 |

---

### 3.2 边界测试

#### QA-B-001：无历史、空剧集、全无资源时进入可理解的默认集或 no-resource 状态

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **边界类型** | 数据边界 / 状态边界 |
| **前置条件** | 分别准备 3 类数据：无历史；`episodes=[]`；所有 `video_url` 不可播放 |
| **测试步骤** | 1. 进入无历史 drama。2. 进入无剧集 drama。3. 进入全无资源 drama。4. 分别观察页面状态与默认集决策。 |
| **预期结果** | 无历史时回退第一条可播放 Episode；空剧集或全无资源时进入 `no-resource`，不出现白屏或卡死。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实黑盒边界操作。 |
| **备注** | 已有自动化佐证：Backend `drama-episodes` / `player.progress` 测试，Android / iOS `noResource` 与 fallback 场景测试。 |

---

#### QA-B-002：canonical `play/:id` 与 Android legacy `player/:id` 路由兼容一致

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Android / iOS |
| **边界类型** | 输入边界 / 兼容边界 |
| **前置条件** | Android 保留 `player/:id` alias；iOS 使用 `play` 语义路由 |
| **测试步骤** | 1. 使用 canonical `play/:id` 进入播放器。2. 在 Android 使用 legacy `player/:id` 进入。3. 对比两条路径的页面表现与后续请求顺序。 |
| **预期结果** | Android alias 会被统一映射到 canonical 播放流程；两条路径的播放器行为一致；iOS 保持 `play` 语义入口。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实路由黑盒。 |
| **备注** | 已有自动化佐证：Android `NavGraph` alias forward 逻辑与 ViewModel 参数兼容实现；iOS `NavigationRouter` 保持 `.player(videoId:)`。 |

---

#### QA-B-003：播放页隐藏底部导航 / Tab Bar，并在切后台或页面消失时 best-effort 上报 stop

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | iOS / Android |
| **边界类型** | UI 边界 / 状态边界 |
| **前置条件** | 当前集正在播放，页面已进入沉浸式态 |
| **测试步骤** | 1. 进入播放器。2. 观察底部导航 / Tab Bar 是否隐藏。3. 切到后台再返回。4. 触发返回或页面消失。5. 观察是否发起 `POST /api/player/stop`。 |
| **预期结果** | 播放页沉浸式隐藏底部导航；切后台进入可理解状态；页面退出或消失时 best-effort 上报 stop。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实前后台黑盒操作。 |
| **备注** | 已有自动化佐证：Android `PlayerScreen` 生命周期监听；iOS `PlayerView` `.toolbar(.hidden, for: .tabBar/.navigationBar)`、`onDisappear` 与 `PlayerViewModelTests`。 |

---

### 3.3 异常测试

#### QA-E-001：`X-Playback-Session-Id` 缺失或非法时 progress/start/stop 返回明确错误

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **异常类型** | 服务端错误 |
| **模拟方式** | 构造缺失 header、非法 UUID header 请求 |
| **前置条件** | Backend 已部署当前播放器接口实现 |
| **测试步骤** | 1. 请求 `GET /api/player/progress?dramaId=...` 且不带 header。2. 请求 `POST /api/player/start` 且带非法 header。3. 请求 `POST /api/player/stop` 且带非法 header。 |
| **预期结果** | 三个接口均返回 `400`，错误码为 `INVALID_PLAYBACK_SESSION`；客户端不会误进入成功播放态。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无设备黑盒执行；未从客户端侧发起真实异常请求。 |
| **备注** | 已有自动化佐证：Backend `player.progress/player.start/player.stop` 路由测试覆盖 header 校验；iOS/Android 网络层已限定 header 透传范围。 |

---

#### QA-E-002：服务端错误、资源不可用或网络失败时页面进入 error / no-resource，并提供重试或回退路径

| 属性 | 值 |
|------|-----|
| **优先级** | P1 |
| **涉及平台** | Backend / iOS / Android |
| **异常类型** | 网络异常 / 服务端错误 / 资源不足 |
| **模拟方式** | 模拟 `DRAMA_NOT_FOUND`、`EPISODE_NOT_PLAYABLE`、超时、资源 404 |
| **前置条件** | 播放页可进入 bootstrap |
| **测试步骤** | 1. 进入不存在的 drama。2. 进入资源不可播放的 drama。3. 模拟网络超时后点击重试。4. 观察页面状态与恢复路径。 |
| **预期结果** | 内容不存在时进入错误态；无资源时进入 `no-resource`；网络失败时出现重试入口；不出现白屏与不可恢复假死。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无 device/simulator skill，未执行真实异常黑盒。 |
| **备注** | 已有自动化佐证：Backend 服务/路由错误码测试；Android / iOS ViewModel 错误与 `noResource` 状态建模。 |

---

### 3.4 兼容性测试

#### QA-C-001：三端遵守同一 shared contract（route 语义、bootstrap 顺序、header 范围、切集规则）

| 属性 | 值 |
|------|-----|
| **优先级** | P0 |
| **涉及平台** | Backend / iOS / Android |
| **兼容场景** | 跨端一致性 |
| **前置条件** | 三端均使用当前 PRD-03 主仓实现 |
| **测试步骤** | 1. 对照 shared design 检查 route 语义是否统一为 `play`，Android 是否兼容 `player`。2. 检查 bootstrap 是否固定为 `progress -> episodes -> start`。3. 检查 `X-Playback-Session-Id` 是否仅用于 progress/start/stop。4. 检查切集是否固定从 0 秒开始。 |
| **预期结果** | 三端 shared contract 一致，无平台间语义漂移；Web 保持不涉及。 |

**执行结果**：

| 属性 | 值 |
|------|-----|
| **状态** | ⏭️ 跳过 |
| **实际结果** | 当前会话无设备黑盒执行；未做真机交叉验证。 |
| **备注** | 已通过文档与主仓实现核对：Backend 路由、Android `PlayerViewModel/NavGraph`、iOS `PlayerViewModel/PlayerView` 已收口 shared contract。 |

---

## 四、测试结果汇总

| 统计项 | 数量 |
|--------|------|
| 总用例数 | 9 |
| ✅ 通过 | 0 |
| ❌ 失败 | 0 |
| ⏭️ 跳过 | 9 |
| ⚠️ 阻塞 | 0 |

> 结论：本次 QA 黑盒阶段**未发现失败或阻塞用例**，但由于当前会话无 device/simulator skill，9 个黑盒用例均按 workflow 规则跳过执行。已存在的自动化验证结果可作为后续真机 / 模拟器黑盒回归前的前置可信度支撑，但**不等价于真实设备黑盒通过**。

---

## 五、未通过用例详细记录

| 用例编号 | 标题 | 优先级 | 状态 | 失败原因 | 影响评估 |
|---------|------|--------|------|---------|---------|
| 无 | 本次无失败 / 阻塞用例 | — | — | 当前阶段仅因缺少 device/simulator skill 跳过黑盒执行 | 不构成功能失败结论，但意味着缺少真机 / 模拟器黑盒证据 |

---

## 六、用户决策

> 本次 QA 黑盒阶段按 `feature-workflow` 规则处理为：**已产出测试文档，设备执行步骤因缺少 device/simulator skill 被跳过**。
>
> 当前不存在需要用户在“继续推进 / 驳回修复 / 记录并推进”之间做出的失败用例决策；后续可按 workflow 进入 `worktree-merge`，但若需要补齐真机 / 模拟器黑盒证据，应在具备相应 skill 或设备能力后补测。 |
