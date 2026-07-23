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
- **观察**：首页默认进入短剧首卡播放态，右侧评论按钮及评论数可见；当前样本卡片为《幸福的家》，评论数显示为 239。视频区域仍有明显花屏，但评论入口识别不受影响。
- **截图文件**：`assets/2026-07-23-step-01-home-ready.png`

### 步骤 2：点击评论入口

- **操作**：点击首页右侧评论按钮
- **命令**：`adb shell input tap 1005 1490 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-comment-panel.png`
- **观察要点**：评论以底部抽屉/全页/半屏出现，是否可匿名浏览

*执行后由 subagent 填写：*
- **观察**：点击后以底部半屏抽屉形式拉起评论面板，标题为“239条评论”。匿名态可直接浏览评论列表，首屏包含活动引导“来评论区聊两句·解锁千元奖励”、多条用户评论、点赞数与“展开6条回复”入口。
- **截图文件**：`assets/2026-07-23-step-02-comment-panel.png`

### 步骤 3：尝试点击评论输入或互动项

- **操作**：点击评论输入框或首条评论互动按钮
- **命令**：`adb shell input tap 540 2070 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-comment-gate.png`
- **观察要点**：记录是否要求登录、是否出现发布输入态、是否进入二级评论页

*执行后由 subagent 填写：*
- **观察**：点击底部评论输入区后，未进入可直接发布的输入态，而是跳转到手机号验证码登录页。登录页包含手机号输入框、“获取验证码”、用户协议勾选以及抖音快捷登录，说明匿名用户可浏览评论，但评论发布/互动前需要登录。
- **截图文件**：`assets/2026-07-23-step-03-comment-gate.png`

### 步骤 4：关闭评论面板返回首页

- **操作**：返回或点击空白处关闭评论面板
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：关闭方式、首页恢复位置、评论状态是否保留

*执行后由 subagent 填写：*
- **观察**：从登录页返回会先回到评论面板；再次返回后关闭评论层并回到首页推荐流。关闭后首页仍停留在当前短剧卡片，评论数 239 与右侧互动栏保持可见，说明返回链路为“登录页 → 评论面板 → 首页”。
- **截图文件**：`assets/2026-07-23-step-04-back-home.png`

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
