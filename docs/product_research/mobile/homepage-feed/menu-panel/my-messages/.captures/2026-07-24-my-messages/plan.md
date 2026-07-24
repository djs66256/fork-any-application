# 采集规划：红果 — 我的消息

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/menu-panel/my-messages |
| 采集范围 | 菜单抽屉中“我的消息”模块的入口态、点击“更多”后的消息页，以及返回后的上下文 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录“我的消息”模块入口态

- **操作**：打开首页左上菜单抽屉，记录“我的消息”模块在抽屉中的位置与内容摘要
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-01-entry-context.png`
- **截图**：保存为 `assets/2026-07-24-step-01-entry-context.png`
- **观察要点**：确认模块标题、预览条目、时间信息与“更多”入口位置

*采集阶段回填：*
- **观察**：菜单抽屉顶部登录 CTA 下方展示“我的消息”模块，右上角提供“更多”文字与箭头。模块内当前只预览 1 条消息，左侧为铃铛图标，中间文案为“春节主会场上线！王牌短剧演员...”，右侧显示相对时间“547天前”。该模块位于“最近在看”之上，属于抽屉上部的重要个人通知入口。
- **截图文件**：`assets/2026-07-24-step-01-entry-context.png`

### 步骤 2：点击“更多”进入消息页

- **操作**：点击“我的消息”模块右上角“更多”入口，记录目标页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb shell input tap 748 338 && sleep 4 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-02-more-result.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-02-more-result.xml`
- **截图**：保存为 `assets/2026-07-24-step-02-messages-page.png`
- **XML**：保存为 `assets/2026-07-24-step-02-messages-page.xml`
- **观察要点**：确认“更多”是否进入独立消息页、消息列表结构，以及匿名态是否存在额外登录门槛

*采集阶段回填：*
- **观察**：点击“更多”后进入独立的“我的消息”页，顶部标题为“我的消息”，左上角提供返回箭头。页面首条消息以“系统通知”分组卡片呈现，右侧显示绝对日期“2025-01-22”，摘要文案仍为“春节主会场上线！王牌短剧演员携100余部贺...”，并以橙点标识未读。页面下半区出现胶片插画和“登录查看互动消息”提示，底部提供橙色“登录”按钮，说明系统通知可直接浏览，但互动类消息需要登录后查看。XML 中可见系统通知摘要的 content-desc，但未输出更多结构化文本节点，推测页面主体为自绘或 Compose 混合实现。
- **截图文件**：`assets/2026-07-24-step-02-messages-page.png`
- **XML 文件**：`assets/2026-07-24-step-02-messages-page.xml`

### 步骤 3：从消息页返回并确认回落上下文

- **操作**：在“我的消息”页返回
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-03-back-context.png`
- **截图**：保存为 `assets/2026-07-24-step-03-back-context.png`
- **观察要点**：确认返回后是否重新回到菜单抽屉，而不是首页首屏

*采集阶段回填：*
- **观察**：从“我的消息”页返回后，界面回到原菜单抽屉上下文，“我的消息”模块重新可见，右侧仍保留首页 Feed 背景。说明消息页是从抽屉上下文发起的全屏承接页，返回后不会直接关闭抽屉。
- **截图文件**：`assets/2026-07-24-step-03-back-context.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-entry-context.png` | 截图 | 步骤 1 | 菜单抽屉中的“我的消息”模块入口态 |
| `assets/2026-07-24-step-02-messages-page.png` | 截图 | 步骤 2 | 点击“更多”后进入的独立消息页 |
| `assets/2026-07-24-step-02-messages-page.xml` | XML | 步骤 2 | 独立消息页 UI 层级 |
| `assets/2026-07-24-step-03-back-context.png` | 截图 | 步骤 3 | 从消息页返回后的菜单抽屉上下文 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:45:00 | 打开菜单抽屉并记录“我的消息”模块 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-01-entry-context.png` | 成功，记录“我的消息”模块入口态 |
| 16:45:10 | 点击“更多”并抓取消息页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4 && adb shell input tap 72 116 && sleep 2 && adb shell input tap 748 338 && sleep 4 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-02-more-result.png && adb exec-out uiautomator dump /dev/tty | perl -pe 's/\x0D\x0A/\n/g' > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-02-more-result.xml` | 成功，进入独立“我的消息”页并抓取截图与 XML |
| 16:45:20 | 从消息页返回并记录上下文 | `adb shell input keyevent KEYCODE_BACK && sleep 2 && adb exec-out screencap -p > docs/product_research/mobile/homepage-feed/menu-panel/my-messages-step-03-back-context.png` | 成功，确认返回后回到菜单抽屉 |
| 16:54:00 | 归档 my-messages 产物 | `cp my-messages-step-* homepage-feed/menu-panel/my-messages/.captures/2026-07-24-my-messages/assets/` | 成功，完成产物归档 |

## 异常记录

- 当前样本只看到“系统通知”分组下的 1 条消息，未继续点入单条消息详情。
- 页面下半区的“互动消息”明确受登录限制，但本轮未点击底部“登录”按钮继续验证登录链路；该登录能力应复用 `login-cta` 的统一登录体系。
- XML 对消息页主体的可读结构暴露较少，更多判断依赖截图内容。

## 执行状态

- [x] 步骤 1：记录“我的消息”模块入口态
- [x] 步骤 2：点击“更多”进入消息页
- [x] 步骤 3：从消息页返回并确认回落上下文
