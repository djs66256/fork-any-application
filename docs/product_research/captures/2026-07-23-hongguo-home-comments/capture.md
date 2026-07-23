# 采集：红果 — 首页评论面板

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
| 采集范围 | 三级页面以内的首页评论入口：首页首卡 → 评论面板/评论页 → 匿名可见交互与登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入首页首屏

- **操作**：冷启动红果并等待首页稳定
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5`
- **截图**：保存为 `assets/2026-07-23-step-01-home-ready.png`
- **观察要点**：记录首页评论按钮位置和评论数可见状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击评论入口

- **操作**：点击首页右侧评论按钮
- **命令**：`adb shell input tap 1005 1490 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-comment-panel.png`
- **观察要点**：评论以底部抽屉/全页/半屏出现，是否可匿名浏览

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：尝试点击评论输入或互动项

- **操作**：点击评论输入框或首条评论互动按钮
- **命令**：`adb shell input tap 540 2070 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-comment-gate.png`
- **观察要点**：记录是否要求登录、是否出现发布输入态、是否进入二级评论页

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：关闭评论面板返回首页

- **操作**：返回或点击空白处关闭评论面板
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：关闭方式、首页恢复位置、评论状态是否保留

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-home-comments.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-home-ready.png` | 截图 | 步骤 1 | 首页评论入口状态 |
| `assets/2026-07-23-step-02-comment-panel.png` | 截图 | 步骤 2 | 评论面板结构 |
| `assets/2026-07-23-step-03-comment-gate.png` | 截图 | 步骤 3 | 评论输入或登录门槛 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 评论关闭后首页状态 |
| `assets/2026-07-23-home-comments.mp4` | 录屏 | 步骤 2-4 | 评论链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入首页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5` | 待执行 |
| HH:MM:SS | 打开评论面板 | `adb shell input tap 1005 1490 && sleep 2` | 待执行 |
| HH:MM:SS | 尝试评论互动 | `adb shell input tap 540 2070 && sleep 2` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
