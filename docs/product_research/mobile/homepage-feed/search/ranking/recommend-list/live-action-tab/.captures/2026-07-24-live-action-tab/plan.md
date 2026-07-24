# 采集规划：红果 — 推荐榜真人剧 Tab

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking/recommend-list/live-action-tab |
| 采集范围 | 从推荐榜切换到顶部“真人剧”内容类型，记录真人剧内容池首屏结构与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入推荐榜页面

- **操作**：从首页进入搜索页，再点击“排行”，随后切换到“推荐榜”
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 133 555 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-recommend-list.png`
- **观察要点**：确认已处于推荐榜默认“全部”内容类型

*采集阶段回填：*
- **观察**：从排行页切到推荐榜后，顶部一级标签为“全部 / 真人剧 / 漫剧 / AI剧 / 演员”，默认停留在“全部”。这说明“真人剧”属于推荐榜内的内容类型切换，而非独立页面入口。
- **截图文件**：`assets/2026-07-24-step-01-recommend-list.png`

### 步骤 2：点击“真人剧”Tab

- **操作**：在推荐榜页点击顶部“真人剧”内容类型标签
- **命令**：`adb shell input tap 251 319 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-live-action-tab.png`
- **观察要点**：确认榜单标题、列表内容与内容类型是否切换为真人剧池

*采集阶段回填：*
- **观察**：点击“真人剧”后，页面标题切换为《真人剧推荐榜》，顶部一级标签高亮变为“真人剧”，榜单仍沿用推荐榜的“推荐”主指标，但内容列表明显转为真人短剧题材，条目更集中在都市情感、家庭关系、成长、重生改命等真人演员出演内容。
- **截图文件**：`assets/2026-07-24-step-02-live-action-tab.png`

### 步骤 3：返回排行/搜索页上文

- **操作**：从真人剧推荐榜状态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **观察要点**：记录返回落点和推荐榜状态是否保留

*采集阶段回填：*
- **观察**：从真人剧推荐榜返回后，直接回到搜索承接页，而不是停留在推荐榜默认态。这说明推荐榜中的内容类型切换仍属于排行承接层内部状态，返回操作会退出整个排行体系。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-recommend-list.png` | 截图 | 步骤 1 | 推荐榜默认全部态 |
| `assets/2026-07-24-step-02-live-action-tab.png` | 截图 | 步骤 2 | 真人剧推荐榜首屏 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 14:13:00 | 进入推荐榜 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 133 555 && sleep 2` | 成功，进入推荐榜默认全部态 |
| 14:13:08 | 切换真人剧 Tab | `adb shell input tap 251 319 && sleep 2` | 成功，进入真人剧推荐榜 |
| 14:13:13 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

无异常

## 执行状态

- [x] 步骤 1：进入推荐榜页面
- [x] 步骤 2：点击“真人剧”Tab
- [x] 步骤 3：返回排行/搜索页上文
