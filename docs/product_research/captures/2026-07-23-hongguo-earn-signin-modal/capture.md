# 采集：红果 — 赚钱签到弹窗

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
| 采集范围 | 三级页面以内的赚钱签到弹窗：首页 → 赚钱页 → 签到弹窗 → 关闭 / 尝试领取 / 返回 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入赚钱页并保留签到弹窗

- **操作**：冷启动后切换到赚钱页，不主动关闭签到弹窗
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 760 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-signin-modal.png`
- **观察要点**：记录签到弹窗布局、关闭控件、CTA 和奖励说明

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击签到 CTA

- **操作**：点击弹窗主按钮进行签到尝试
- **命令**：`adb shell input tap 540 1545 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-signin-action.png`
- **观察要点**：签到成功提示、登录门槛或下一层弹窗

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：如仍在弹窗则关闭弹窗

- **操作**：点击弹窗外区域或关闭区域
- **命令**：`adb shell input tap 540 1830 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-modal-closed.png`
- **观察要点**：关闭方式、是否回到赚钱首屏、签到状态是否变化

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回首页

- **操作**：返回首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回层级、赚钱页状态保留情况

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 1 到步骤 4
- **保存为**：`assets/2026-07-23-earn-signin-modal.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-signin-modal.png` | 截图 | 步骤 1 | 赚钱签到弹窗 |
| `assets/2026-07-23-step-02-signin-action.png` | 截图 | 步骤 2 | 点击签到 CTA 后状态 |
| `assets/2026-07-23-step-03-modal-closed.png` | 截图 | 步骤 3 | 关闭弹窗后的赚钱首屏 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 返回首页状态 |
| `assets/2026-07-23-earn-signin-modal.mp4` | 录屏 | 步骤 1-4 | 赚钱签到弹窗录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入赚钱页并保留弹窗 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 760 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击签到 CTA | `adb shell input tap 540 1545 && sleep 3` | 待执行 |
| HH:MM:SS | 关闭签到弹窗 | `adb shell input tap 540 1830 && sleep 2` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
