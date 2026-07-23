# 采集：红果 — 首页核心观看链路

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
| 采集范围 | 三级页面以内的首页核心观看链路：冷启动首页 → 首卡播放态/暂停态 → “观看完整漫剧”入口或剧集详情承接页（如匿名可达） |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：冷启动进入首页

- **操作**：停止红果并重新启动，等待首页首屏稳定
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5`
- **截图**：保存为 `assets/2026-07-23-step-01-home-launch.png`
- **观察要点**：记录首页默认落点、是否有花屏/弹窗、视频默认是否自动播放

*执行后由 subagent 填写：*
- **观察**：首页默认停留在首卡《亲疏之间》，视频区域出现明显花屏/彩条乱码，但标题、标签、互动数与底部“观看完整短剧·全39集”入口可正常识别；未见额外弹窗，首页首卡默认处于自动播放态。
- **截图文件**：`assets/2026-07-23-step-01-home-launch.png`

### 步骤 2：点击视频区域确认播放交互

- **操作**：点击首卡视频中部，观察播放/暂停或控件变化
- **命令**：`adb shell input tap 540 980 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-home-video-tap.png`
- **观察要点**：是否出现播放按钮、暂停状态、控制栏或跳转行为

*执行后由 subagent 填写：*
- **观察**：点击视频中部后，首卡中央出现播放三角图标，说明点击行为将首页首卡视频切换为暂停态；页面未发生跳转，右侧互动栏与底部完整短剧入口保持原位。
- **截图文件**：`assets/2026-07-23-step-02-home-video-tap.png`

### 步骤 3：点击“观看完整漫剧”入口

- **操作**：点击底部“观看完整漫剧”条，观察进入的二级/三级页
- **命令**：`adb shell input tap 537 1880 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-watch-full-entry.png`
- **观察要点**：确认进入详情页/播放页/登录门槛中的哪一种，记录页面主结构

*执行后由 subagent 填写：*
- **观察**：点击底部完整短剧入口后，进入剧集承接/播放页，顶部出现返回、集数“第1集”、倍速与更多操作；底部出现“选集·已完结·全39集”抽屉栏，说明该入口直接承接到可继续追更的剧集播放页，而非先进入单独详情页或登录门槛。
- **截图文件**：`assets/2026-07-23-step-03-watch-full-entry.png`

### 步骤 4：如进入二级页则记录返回链路

- **操作**：若已进入新页面，执行返回并确认是否回到首页首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回目标页、状态恢复情况、是否回到原卡片位置

*执行后由 subagent 填写：*
- **观察**：返回后重新回到首页首卡《亲疏之间》，页面位置保持在原卡片；中央仍保留播放三角图标，说明从承接页返回后首卡暂停态被保留，未重置到新的推荐卡。
- **截图文件**：`assets/2026-07-23-step-04-back-home.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-home-core-chain.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-home-launch.png` | 截图 | 步骤 1 | 首页冷启动稳定态 |
| `assets/2026-07-23-step-02-home-video-tap.png` | 截图 | 步骤 2 | 首页视频点击后的状态 |
| `assets/2026-07-23-step-03-watch-full-entry.png` | 截图 | 步骤 3 | 观看完整漫剧入口承接页 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 返回后的首页状态 |
| `assets/2026-07-23-home-core-chain.mp4` | 录屏 | 步骤 2-4 | 首页核心观看链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 冷启动进入首页 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5` | 待执行 |
| HH:MM:SS | 点击视频区域 | `adb shell input tap 540 980 && sleep 2` | 待执行 |
| HH:MM:SS | 点击观看完整漫剧入口 | `adb shell input tap 537 1880 && sleep 3` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
