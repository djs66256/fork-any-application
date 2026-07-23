# 采集：红果 — 一级页面-商城首屏

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | mall |
| 采集范围 | Android 模拟器中红果应用“商城”一级页面首屏静态布局，包括切换到商城 Tab 后的默认首屏内容、商品入口、福利元素与底部导航状态 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

> 以下步骤在规划阶段填入。采集 subagent 逐步骤执行，每步完成后填写「观察」栏和截图路径。

### 步骤 1：重置到一级页面起点

- **操作**：停止红果应用后重新启动，等待默认页面加载完成
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4`
- **截图**：保存为 `assets/2026-07-23-step-01-launch-mall.png`
- **观察要点**：确认默认落点与首页加载状态，记录是否有弹窗遮挡

*执行后由 subagent 填写：*
- **观察**：重新启动后默认落在首页 Feed，未出现签到或活动弹窗，底部“首页”高亮，页面可直接继续切换 Tab。
- **截图文件**：`assets/2026-07-23-step-01-launch-mall.png`

### 步骤 2：处理首屏遮挡层

- **操作**：如出现签到、活动、领奖等弹窗，先关闭弹窗，确保可点击底部导航
- **命令**：`adb shell input tap 540 1620 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-dismiss-overlay.png`
- **观察要点**：确认弹窗关闭方式是否有效，关闭后页面是否稳定

*执行后由 subagent 填写：*
- **观察**：未检测到遮挡层；预估点击未引起页面变化，仍处于首页可操作状态。
- **截图文件**：`assets/2026-07-23-step-02-dismiss-overlay.png`

### 步骤 3：切换到商城一级页面并截图

- **操作**：点击底部“商城”Tab，等待页面稳定后采集首屏截图
- **命令**：`adb shell input tap 540 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-mall/assets/2026-07-23-step-03-mall.png`
- **截图**：保存为 `assets/2026-07-23-step-03-mall.png`
- **观察要点**：关注商品宫格/卡片、价格与金币标识、顶部筛选区域、Tab 高亮状态

*执行后由 subagent 填写：*
- **观察**：已切换到“商城”一级页。顶部为搜索框、橙色“搜索”按钮与购物车入口，下方有“我的订单/卡券红包/我的钱包/短剧同款/国家补贴”等快捷入口；主体内容为双列商品流，底部浮层提示“登录可查看商品价格”，“商城”Tab 高亮。
- **截图文件**：`assets/2026-07-23-step-03-mall.png`

## 录屏

本次采集无需录屏。

*执行后由 subagent 填写：*
- **录屏文件**：无

## 产物清单

> 采集 subagent 执行完成后回填此表。

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-launch-mall.png` | 截图 | 步骤 1 | 冷启动后的首页起始页面 |
| `assets/2026-07-23-step-02-dismiss-overlay.png` | 截图 | 步骤 2 | 无弹窗时的可操作状态 |
| `assets/2026-07-23-step-03-mall.png` | 截图 | 步骤 3 | 商城一级页面首屏 |

## 采集日志

> 采集 subagent 执行过程中逐条记录。

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 16:59:00 | 冷启动进入红果 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4` | 成功，默认落在首页 Feed |
| 16:59:06 | 处理首屏遮挡层 | `adb shell input tap 540 1620 && sleep 2` | 成功，未检测到弹窗 |
| 16:59:09 | 切换商城并截图 | `adb shell input tap 540 2240 && sleep 2 && adb exec-out screencap -p > docs/product_research/captures/2026-07-23-hongguo-tab-mall/assets/2026-07-23-step-03-mall.png` | 成功，商城首屏截图已保存 |

## 异常记录

> 采集 subagent 回填。如有操作失败、页面异常、定位偏差等在此记录。

无异常

---

> 本文件在规划阶段预制采集信息和操作步骤，由采集 subagent 执行后回填观察、日志、产物清单和异常。
> 不作为分析推断，分析内容见同目录 `analysis.md`。
