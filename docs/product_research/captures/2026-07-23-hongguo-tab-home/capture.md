# 采集：红果 — 一级页面-首页首屏

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | homepage-feed |
| 采集范围 | Android 模拟器中红果应用的首页一级页面首屏静态布局，包括启动后默认进入页面、可能出现的签到弹窗处理、顶部栏、主内容区与底部导航的首屏状态 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

> 以下步骤在规划阶段填入。采集 subagent 逐步骤执行，每步完成后填写「观察」栏和截图路径。

### 步骤 1：冷启动进入红果

- **操作**：停止红果应用后重新启动，等待默认一级页面加载完成
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4`
- **截图**：保存为 `assets/2026-07-23-step-01-launch-home.png`
- **观察要点**：记录默认落点是否为首页、是否有开屏广告/活动弹窗、是否需要登录

*执行后由 subagent 填写：*
- **观察**：冷启动后直接落在首页 Feed 首屏，未出现签到或活动弹窗；顶部为左侧菜单和右侧搜索入口，底部导航中“首页”高亮。主内容是竖屏短剧播放流，右侧可见金币、收藏、评论、点赞、分享等操作位。
- **截图文件**：`assets/2026-07-23-step-01-launch-home.png`

### 步骤 2：处理首屏遮挡层

- **操作**：如出现签到、活动、领奖等弹窗，先关闭弹窗，回到一级页面可见状态
- **命令**：`adb shell input tap 540 1620 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-dismiss-overlay.png`
- **观察要点**：确认弹窗类型、关闭方式、关闭后是否仍停留在首页首屏

*执行后由 subagent 填写：*
- **观察**：本次冷启动首页没有出现遮挡层；按预估坐标点击后页面无明显变化，仍停留在首页首屏，可继续采集。
- **截图文件**：`assets/2026-07-23-step-02-dismiss-overlay.png`

### 步骤 3：首页首屏截图

- **操作**：在无遮挡状态下采集首页一级页面首屏截图
- **命令**：`adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-home/assets/2026-07-23-step-03-home.png`
- **截图**：保存为 `assets/2026-07-23-step-03-home.png`
- **观察要点**：关注顶部导航/搜索入口、主内容首卡、推荐流形态、底部导航高亮态、是否存在浮层按钮

*执行后由 subagent 填写：*
- **观察**：首页首屏无弹窗遮挡。首卡为短剧《赠物得长生，老头修仙记！》，可见“爆剧”标签、题材标签、热评文案与“观看完整漫剧·全114集”入口；底部导航保留“首页/剧场/商城/赚钱/我的”五个一级入口，其中“首页”高亮。
- **截图文件**：`assets/2026-07-23-step-03-home.png`

## 录屏

本次采集无需录屏。

*执行后由 subagent 填写：*
- **录屏文件**：无

## 产物清单

> 采集 subagent 执行完成后回填此表。

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-launch-home.png` | 截图 | 步骤 1 | 冷启动后的默认落点页面 |
| `assets/2026-07-23-step-02-dismiss-overlay.png` | 截图 | 步骤 2 | 无弹窗时的首页可操作状态 |
| `assets/2026-07-23-step-03-home.png` | 截图 | 步骤 3 | 首页一级页面首屏 |

## 采集日志

> 采集 subagent 执行过程中逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:57:00 | 冷启动进入红果 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4` | 成功，默认落在首页 Feed |
| 16:57:07 | 处理首屏遮挡层 | `adb shell input tap 540 1620 && sleep 2` | 成功，未检测到弹窗，页面保持首页 |
| 16:57:10 | 首页首屏截图 | `adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-home/assets/2026-07-23-step-03-home.png` | 成功，首页首屏截图已保存 |

## 异常记录

> 采集 subagent 回填。如有操作失败、页面异常、定位偏差等在此记录。

无异常

---

> 本文件在规划阶段预制采集信息和操作步骤，由采集 subagent 执行后回填观察、日志、产物清单和异常。
> 不作为分析推断，分析内容见同目录 `analysis.md`。
