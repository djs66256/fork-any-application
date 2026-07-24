# 采集规划：红果 — 推荐榜全部内容类型标签

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/search/ranking/recommend-list/all-tab |
| 采集范围 | 进入推荐榜默认“全部”内容类型标签，记录列表结构、混合内容策略与返回链路 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入推荐榜默认“全部”内容类型标签

- **操作**：从首页进入搜索页，点击“排行”，再切换到“推荐榜”，保持顶部默认“全部”高亮
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 133 555 && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-01-all-tab.png`
- **观察要点**：确认顶部“全部”和二级“推荐榜”同时高亮，并记录列表条目是否为混合内容池

*采集阶段回填：*
- **观察**：进入推荐榜后，顶部内容类型默认保持“全部”高亮，二级标签“推荐榜”高亮。标题仍为《红果推荐榜》，列表并未限定在某一种内容形态：前排条目多为玄幻、脑洞、奇幻等高概念题材，也能看到带演员名的真人剧条目，说明“全部”态承担推荐榜的混合内容总览角色。
- **截图文件**：`assets/2026-07-24-step-01-all-tab.png`

### 步骤 2：返回搜索页上文

- **操作**：从推荐榜默认“全部”态执行返回，确认回到上一层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-back-context.png`
- **观察要点**：记录返回落点，确认 all-tab 仍属于排行承接层内部状态

*采集阶段回填：*
- **观察**：从推荐榜默认“全部”态返回后，直接回到搜索承接页，而不是停留在排行页。这说明 all-tab 只是推荐榜内部的默认内容池状态，而非独立页面。
- **截图文件**：`assets/2026-07-24-step-02-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-all-tab.png` | 截图 | 步骤 1 | 推荐榜默认全部态 |
| `assets/2026-07-24-step-01-all-tab.xml` | XML | 步骤 1 | 推荐榜默认全部态界面树 |
| `assets/2026-07-24-step-02-back-context.png` | 截图 | 步骤 2 | 返回后的搜索页 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 13:53:06 | 进入推荐榜默认全部态 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 1000 110 && sleep 2 && adb shell input tap 322 264 && sleep 2 && adb shell input tap 133 555 && sleep 2` | 成功，顶部“全部”和“推荐榜”高亮，展示混合内容推荐列表 |
| 13:53:11 | 返回搜索页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，直接返回搜索承接页 |

## 异常记录

- 15:30 单独补采 all-tab 时误落到搜索结果页，未形成有效样本；最终复用已验证的推荐榜默认“全部”态快照作为本路径采集源，因为该状态本身就是 all-tab 的默认入口态。

## 执行状态

- [x] 步骤 1：进入推荐榜默认“全部”内容类型标签
- [x] 步骤 2：返回搜索页上文
