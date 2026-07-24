# 采集规划：红果 — 冷启动签到弹窗

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/signin-popup |
| 采集范围 | 冷启动后偶发出现的 7 日签到金币奖励弹窗，以及关闭后返回首页的状态 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：冷启动命中签到弹窗

- **操作**：冷启动红果并记录偶发弹出的签到浮层
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 4`
- **截图**：保存为 `assets/2026-07-24-step-01-signin-popup.png`
- **观察要点**：弹窗文案、奖励结构、CTA 与关闭方式

*采集阶段回填：*
- **观察**：冷启动后偶发出现橙色渐变签到浮层，主标题为“7天签到必得6万金币”，中部以 7 宫格方式展示每日金币奖励，底部有“立即领取”主 CTA 和关闭按钮。弹窗覆盖在首页内容之上，背景仍能看见首页视频和右侧互动列。
- **截图文件**：`assets/2026-07-24-step-01-signin-popup.png`

### 步骤 2：关闭弹窗后返回首页

- **操作**：关闭签到弹窗，确认是否回到首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-24-step-02-home-after-popup.png`
- **观察要点**：关闭路径、是否恢复首页上下文、是否有残留状态

*采集阶段回填：*
- **观察**：关闭浮层后直接回到首页 Feed，不进入其他奖励页。首页原有菜单、搜索、互动列与底部导航立即恢复可操作状态，说明签到浮层是冷启动叠加层，而非独立奖励页。
- **截图文件**：`assets/2026-07-24-step-02-home-after-popup.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-signin-popup.png` | 截图 | 步骤 1 | 冷启动签到金币浮层 |
| `assets/2026-07-24-step-02-home-after-popup.png` | 截图 | 步骤 2 | 关闭弹窗后的首页恢复状态 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 11:57:12 | 记录签到弹窗样本 | `adb exec-out screencap -p > assets/2026-07-24-step-01-signin-popup.png` | 成功，记录到冷启动签到弹窗 |
| 11:57:00 | 关闭弹窗后回到首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 成功，返回首页 Feed |

## 异常记录

- 弹窗为偶发出现，非每次冷启动必现。

## 执行状态

- [x] 步骤 1：冷启动命中签到弹窗
- [x] 步骤 2：关闭弹窗后返回首页
