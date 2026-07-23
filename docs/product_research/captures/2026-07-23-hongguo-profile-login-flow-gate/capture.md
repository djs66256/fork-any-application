# 采集：红果 — 我的页登录门槛

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | user-profile |
| 采集范围 | 三级页面以内的我的页登录拦截：首页 → 我的 → 立即登录或登录赚钱入口 → 登录页首屏（不继续实际登录） |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入我的页首屏

- **操作**：冷启动红果并切换到我的页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-profile-ready.png`
- **观察要点**：登录入口位置、登录赚钱浮层状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击“立即登录”按钮

- **操作**：点击顶部“立即登录”
- **命令**：`adb shell input tap 920 280 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-login-page.png`
- **观察要点**：登录页样式、登录方式、是否需要系统权限

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：返回我的页并点击“登录赚钱”浮层

- **操作**：返回我的页后点击右侧登录赚钱浮层
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 941 1275 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-login-earn-gate.png`
- **观察要点**：是否进入同一登录页、是否存在不同拦截形式

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回我的页

- **操作**：返回到我的页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-profile.png`
- **观察要点**：返回路径、我的页状态恢复情况

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-profile-login-flow-gate.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-profile-ready.png` | 截图 | 步骤 1 | 我的页登录入口状态 |
| `assets/2026-07-23-step-02-login-page.png` | 截图 | 步骤 2 | 登录页首屏 |
| `assets/2026-07-23-step-03-login-earn-gate.png` | 截图 | 步骤 3 | 登录赚钱入口承接页 |
| `assets/2026-07-23-step-04-back-profile.png` | 截图 | 步骤 4 | 返回我的页状态 |
| `assets/2026-07-23-profile-login-flow-gate.mp4` | 录屏 | 步骤 2-4 | 我的页登录门槛录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入我的页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击立即登录 | `adb shell input tap 920 280 && sleep 3` | 待执行 |
| HH:MM:SS | 点击登录赚钱浮层 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 941 1275 && sleep 3` | 待执行 |
| HH:MM:SS | 返回我的页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
