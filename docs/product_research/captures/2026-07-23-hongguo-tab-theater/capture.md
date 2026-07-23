# 采集：红果 — 一级页面-剧场首屏

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | theater |
| 采集范围 | Android 模拟器中红果应用“剧场”一级页面首屏静态布局，包括切换到剧场 Tab 后的默认首屏内容、顶部区域、首屏卡片结构与底部导航高亮状态 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

> 以下步骤在规划阶段填入。采集 subagent 逐步骤执行，每步完成后填写「观察」栏和截图路径。

### 步骤 1：重置到一级页面起点

- **操作**：停止红果应用后重新启动，等待默认页面加载完成
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4`
- **截图**：保存为 `assets/2026-07-23-step-01-launch-theater.png`
- **观察要点**：确认默认落点与首页加载状态，记录是否有弹窗遮挡

*执行后由 subagent 填写：*
- **观察**：重新启动后默认仍落在首页 Feed，未出现额外签到弹窗；首页视频流在采集期间存在花屏/乱码现象，按用户说明视为可能的防爬策略，不作为异常处理。
- **截图文件**：`assets/2026-07-23-step-01-launch-theater.png`

### 步骤 2：处理首屏遮挡层

- **操作**：如出现签到、活动、领奖等弹窗，先关闭弹窗，确保可点击底部导航
- **命令**：`adb shell input tap 540 1620 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-dismiss-overlay.png`
- **观察要点**：确认弹窗关闭方式是否有效，关闭后页面是否稳定

*执行后由 subagent 填写：*
- **观察**：本轮启动未出现遮挡层；预估点击未引发页面跳转，底部导航可正常操作。
- **截图文件**：`assets/2026-07-23-step-02-dismiss-overlay.png`

### 步骤 3：切换到剧场一级页面并截图

- **操作**：点击底部“剧场”Tab，等待页面稳定后采集首屏截图
- **命令**：`adb shell input tap 314 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-theater/assets/2026-07-23-step-03-theater.png`
- **截图**：保存为 `assets/2026-07-23-step-03-theater.png`
- **观察要点**：关注 Tab 高亮、顶部标题/筛选、首屏内容组织方式、首屏是否为剧集宫格或推荐列表

*执行后由 subagent 填写：*
- **观察**：已切换到“剧场”一级页。顶部为搜索栏与“截图识别短剧”入口，下方有“找剧/真人剧/漫剧/电影/听书/小说/漫画”等一级分类，以及“筛选/排行榜/新剧/预约”快捷入口；首屏内容为双列剧集海报流，“剧场”Tab 高亮。
- **截图文件**：`assets/2026-07-23-step-03-theater.png`

## 录屏

本次采集无需录屏。

*执行后由 subagent 填写：*
- **录屏文件**：无

## 产物清单

> 采集 subagent 执行完成后回填此表。

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-launch-theater.png` | 截图 | 步骤 1 | 冷启动后的首页起始页面 |
| `assets/2026-07-23-step-02-dismiss-overlay.png` | 截图 | 步骤 2 | 无弹窗时的可操作状态 |
| `assets/2026-07-23-step-03-theater.png` | 截图 | 步骤 3 | 剧场一级页面首屏 |

## 采集日志

> 采集 subagent 执行过程中逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:58:00 | 冷启动进入红果 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4` | 成功，默认落在首页 Feed |
| 16:58:07 | 处理首屏遮挡层 | `adb shell input tap 540 1620 && sleep 2` | 成功，未检测到弹窗，导航可继续操作 |
| 16:58:10 | 切换剧场并截图 | `adb shell input tap 314 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-theater/assets/2026-07-23-step-03-theater.png` | 成功，剧场首屏截图已保存 |

## 异常记录

> 采集 subagent 回填。如有操作失败、页面异常、定位偏差等在此记录。

无异常（冷启动首页视频区域出现花屏/乱码，用户已说明可忽略）

---

> 本文件在规划阶段预制采集信息和操作步骤，由采集 subagent 执行后回填观察、日志、产物清单和异常。
> 不作为分析推断，分析内容见同目录 `analysis.md`。
