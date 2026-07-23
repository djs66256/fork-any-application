# 采集：红果 — 一级页面-赚钱首屏

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | earn-center |
| 采集范围 | Android 模拟器中红果应用“赚钱”一级页面首屏静态布局，包括切换到赚钱 Tab 后的任务入口、金币激励信息、活动模块与底部导航状态 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

> 以下步骤在规划阶段填入。采集 subagent 逐步骤执行，每步完成后填写「观察」栏和截图路径。

### 步骤 1：重置到一级页面起点

- **操作**：停止红果应用后重新启动，等待默认页面加载完成
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4`
- **截图**：保存为 `assets/2026-07-23-step-01-launch-earn.png`
- **观察要点**：确认默认落点与首页加载状态，记录是否有弹窗遮挡

*执行后由 subagent 填写：*
- **观察**：重新启动后默认落在首页 Feed，未出现单独弹窗；页面可继续通过底部导航切换到其他一级页。
- **截图文件**：`assets/2026-07-23-step-01-launch-earn.png`

### 步骤 2：处理首屏遮挡层

- **操作**：如出现签到、活动、领奖等弹窗，先关闭弹窗，确保可点击底部导航
- **命令**：`adb shell input tap 540 1620 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-dismiss-overlay.png`
- **观察要点**：确认弹窗关闭方式是否有效，关闭后页面是否稳定

*执行后由 subagent 填写：*
- **观察**：首页状态下未出现遮挡层；预估点击未造成页面变化，继续执行切换赚钱页操作。
- **截图文件**：`assets/2026-07-23-step-02-dismiss-overlay.png`

### 步骤 3：切换到赚钱一级页面并截图

- **操作**：点击底部“赚钱”Tab，等待页面稳定后采集首屏截图
- **命令**：`adb shell input tap 760 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-earn/assets/2026-07-23-step-03-earn.png`
- **截图**：保存为 `assets/2026-07-23-step-03-earn.png`
- **观察要点**：关注金币余额、任务列表、激励 banner、现金/金币兑换入口、Tab 高亮状态

*执行后由 subagent 填写：*
- **观察**：首次进入“赚钱”页时出现签到弹窗遮挡首屏，随后补充点击弹窗外区域关闭，获得无遮罩首屏。页面顶部为登录收益头图和现金收益气泡，下方可见“新人7天 必得6元”“连续看短剧福利”“新人看剧5分钟得 1.56 元”等任务模块，底部“赚钱”Tab 高亮。
- **截图文件**：`assets/2026-07-23-step-03-earn.png`

## 录屏

本次采集无需录屏。

*执行后由 subagent 填写：*
- **录屏文件**：无

## 产物清单

> 采集 subagent 执行完成后回填此表。

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-launch-earn.png` | 截图 | 步骤 1 | 冷启动后的首页起始页面 |
| `assets/2026-07-23-step-02-dismiss-overlay.png` | 截图 | 步骤 2 | 无弹窗时的可操作状态 |
| `assets/2026-07-23-step-03-earn.png` | 截图 | 步骤 3 | 关闭签到弹窗后的赚钱一级页面首屏 |

## 采集日志

> 采集 subagent 执行过程中逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 17:00:00 | 冷启动进入红果 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4` | 成功，默认落在首页 Feed |
| 17:00:06 | 处理首屏遮挡层 | `adb shell input tap 540 1620 && sleep 2` | 成功，未检测到弹窗 |
| 17:00:09 | 切换赚钱并截图 | `adb shell input tap 760 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-earn/assets/2026-07-23-step-03-earn.png` | 首次截图被签到弹窗遮挡，补充关闭弹窗后已重新保存最终首屏截图 |

## 异常记录

> 采集 subagent 回填。如有操作失败、页面异常、定位偏差等在此记录。

步骤 3 首次进入“赚钱”页时弹出签到弹窗，原始截图被遮挡；补充在弹窗外区域点击关闭后，重新保存了最终首屏截图 `assets/2026-07-23-step-03-earn.png`。

---

> 本文件在规划阶段预制采集信息和操作步骤，由采集 subagent 执行后回填观察、日志、产物清单和异常。
> 不作为分析推断，分析内容见同目录 `analysis.md`。
