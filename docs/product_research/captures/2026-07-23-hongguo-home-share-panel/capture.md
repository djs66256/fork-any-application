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
- **观察**：首页首卡为《我要当咸鱼，你慌什么》，右侧分享按钮可见，页面仍存在视频花屏/彩条现象；同时可见红包浮层“立即领取”，说明首页在内容流中叠加了赚钱导流悬浮层。
- **截图文件**：`assets/2026-07-23-step-01-home-ready.png`

### 步骤 2：点击分享按钮

- **操作**：点击首页右侧分享按钮
- **命令**：`adb shell input tap 1005 1945 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-share-panel.png`
- **观察要点**：分享以系统面板/应用内面板出现，记录分享渠道与操作项

*执行后由 subagent 填写：*
- **观察**：点击后先弹出应用内底部分享面板，标题为“分享至”，可见渠道包括“微信”“朋友圈”“抖音好友”“微博”“复制链接”。这说明首页分享并非直接调用系统 chooser，而是先经过应用自定义分享层。
- **截图文件**：`assets/2026-07-23-step-02-share-panel.png`

### 步骤 3：尝试点击首个分享渠道或复制链接

- **操作**：点击首个可见分享动作，不真正发出外部分享，以记录承接层为主
- **命令**：`adb shell input tap 944 2118 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-share-action.png`
- **观察要点**：记录是否弹出系统 chooser、复制成功提示或登录门槛

*执行后由 subagent 填写：*
- **观察**：本次实际命中“复制链接”区域后，没有出现简单 toast，而是进入更深一层分享承接界面：先出现系统全屏教学提示 `Viewing full screen`，随后进入带海报/二维码的全屏分享页，可继续选择“保存海报、微信、朋友圈、抖音好友、微博”等渠道。说明“复制链接”所在能力链路会触发全屏分享海报态，而非仅复制文本链接。
- **截图文件**：`assets/2026-07-23-step-03-share-action.png`

### 步骤 4：关闭分享面板返回首页

- **操作**：返回至首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回层级、首页恢复状态、是否仍停留首卡

*执行后由 subagent 填写：*
- **观察**：该链路发生异常：在全屏分享页和系统提示之间多次返回后，未能稳定恢复到首页，而是停留在全屏播放页（顶部显示“第3集”，底部有“选集·已完结·全71集”抽屉）。因此本次仅确认了分享入口会进入更深的全屏分享/外部承接链路，但未能在单次采集中完整闭环回到首页。
- **截图文件**：`assets/2026-07-23-step-04-back-home.png`

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
