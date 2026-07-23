# 采集：红果 — 剧场卡片详情承接页

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | theater |
| 采集范围 | 三级页面以内的剧场卡片点击路径：首页 → 剧场 → 首个剧集卡片 → 详情/播放承接页 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动并切换到剧场页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：记录首卡可点击状态和标题信息

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击左上首个剧集卡片

- **操作**：点击剧场页左上首个海报卡片
- **命令**：`adb shell input tap 276 734 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-card-target.png`
- **观察要点**：进入详情页/播放页/登录门槛中的哪一种，记录页面主结构

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：在承接页点击首个核心操作

- **操作**：若承接页匿名可操作，点击首个核心 CTA（如播放、选集、简介展开）
- **命令**：`adb shell input tap 540 1860 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-card-secondary.png`
- **观察要点**：确认是否进入三级页、是否被登录拦截

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回剧场首屏

- **操作**：连续返回至剧场首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回路径、剧场列表是否恢复原位

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-card-detail.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏与目标卡片 |
| `assets/2026-07-23-step-02-card-target.png` | 截图 | 步骤 2 | 卡片承接页 |
| `assets/2026-07-23-step-03-card-secondary.png` | 截图 | 步骤 3 | 承接页核心操作后的状态 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场首屏 |
| `assets/2026-07-23-theater-card-detail.mp4` | 录屏 | 步骤 2-4 | 剧场卡片承接链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击剧集卡片 | `adb shell input tap 276 734 && sleep 3` | 待执行 |
| HH:MM:SS | 点击承接页核心操作 | `adb shell input tap 540 1860 && sleep 3` | 待执行 |
| HH:MM:SS | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
