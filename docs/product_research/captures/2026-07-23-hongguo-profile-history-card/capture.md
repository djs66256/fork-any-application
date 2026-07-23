# 采集：红果 — 我的页历史卡片承接页

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
| 采集范围 | 三级页面以内的我的页历史续播：首页 → 我的 → 历史卡片 → 承接页/续播页/登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入我的页历史首屏

- **操作**：冷启动红果并切换到我的页，保持默认历史标签
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-history-ready.png`
- **观察要点**：首个历史卡片标题、集数状态与是否可点击

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击首个历史剧集卡片

- **操作**：点击左上首个历史卡片
- **命令**：`adb shell input tap 196 1136 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-history-detail.png`
- **观察要点**：进入详情页/播放页/登录门槛中的哪一种，是否恢复到上次集数

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：在承接页点击首个核心动作

- **操作**：若匿名可达，点击首个核心 CTA 或播放区域
- **命令**：`adb shell input tap 540 1860 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-history-secondary.png`
- **观察要点**：是否进入三级页、是否恢复播放、是否遇到登录拦截

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回我的页历史列表

- **操作**：连续返回至我的页历史标签
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-history.png`
- **观察要点**：返回后历史列表位置、卡片状态和标签保持情况

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-profile-history-card.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-history-ready.png` | 截图 | 步骤 1 | 我的页历史首屏 |
| `assets/2026-07-23-step-02-history-detail.png` | 截图 | 步骤 2 | 历史卡片承接页 |
| `assets/2026-07-23-step-03-history-secondary.png` | 截图 | 步骤 3 | 承接页二级动作结果 |
| `assets/2026-07-23-step-04-back-history.png` | 截图 | 步骤 4 | 返回历史列表状态 |
| `assets/2026-07-23-profile-history-card.mp4` | 录屏 | 步骤 2-4 | 我的页历史卡片录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入我的页历史首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击历史卡片 | `adb shell input tap 196 1136 && sleep 3` | 待执行 |
| HH:MM:SS | 点击承接页核心动作 | `adb shell input tap 540 1860 && sleep 3` | 待执行 |
| HH:MM:SS | 返回历史列表 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
