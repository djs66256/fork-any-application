# 采集：红果 — 首页分享面板

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
| 采集范围 | 三级页面以内的首页分享入口：首页首卡 → 分享面板 → 首层分享能力与关闭返回 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入首页首屏

- **操作**：冷启动红果并等待首页稳定
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5`
- **截图**：保存为 `assets/2026-07-23-step-01-home-ready.png`
- **观察要点**：记录分享按钮位置与首页状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击分享按钮

- **操作**：点击首页右侧分享按钮
- **命令**：`adb shell input tap 1004 1818 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-share-panel.png`
- **观察要点**：分享以系统面板/应用内面板出现，记录分享渠道与操作项

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：尝试点击首个分享渠道或复制链接

- **操作**：点击首个可见分享动作，不真正发出外部分享，以记录承接层为主
- **命令**：`adb shell input tap 200 1940 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-share-action.png`
- **观察要点**：记录是否弹出系统 chooser、复制成功提示或登录门槛

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：关闭分享面板返回首页

- **操作**：返回至首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回层级、首页恢复状态、是否仍停留首卡

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-home-share-panel.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-home-ready.png` | 截图 | 步骤 1 | 首页分享入口状态 |
| `assets/2026-07-23-step-02-share-panel.png` | 截图 | 步骤 2 | 分享面板结构 |
| `assets/2026-07-23-step-03-share-action.png` | 截图 | 步骤 3 | 分享动作承接层 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 返回首页状态 |
| `assets/2026-07-23-home-share-panel.mp4` | 录屏 | 步骤 2-4 | 分享链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入首页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5` | 待执行 |
| HH:MM:SS | 打开分享面板 | `adb shell input tap 1004 1818 && sleep 2` | 待执行 |
| HH:MM:SS | 触发分享动作 | `adb shell input tap 200 1940 && sleep 2` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
