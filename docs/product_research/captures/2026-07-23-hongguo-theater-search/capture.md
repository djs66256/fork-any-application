# 采集：红果 — 剧场搜索与识图入口

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
| 采集范围 | 三级页面以内的剧场搜索：首页 → 剧场 → 搜索/截图识别短剧入口 → 搜索页或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动并切换到剧场页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：搜索框和识图入口可见状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击剧场搜索框

- **操作**：点击剧场搜索框进入搜索页
- **命令**：`adb shell input tap 260 116 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-search-page.png`
- **观察要点**：搜索页结构、推荐词、历史词或热搜区域

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：返回后点击“截图识别短剧”入口

- **操作**：返回剧场首屏，再点击右侧识图入口
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 872 116 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-image-recognition.png`
- **观察要点**：是否请求系统权限、上传截图、登录门槛或功能介绍页

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回剧场首屏

- **操作**：返回到剧场页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回路径、搜索与识图状态恢复情况

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-search.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏搜索入口状态 |
| `assets/2026-07-23-step-02-search-page.png` | 截图 | 步骤 2 | 剧场搜索页 |
| `assets/2026-07-23-step-03-image-recognition.png` | 截图 | 步骤 3 | 识图找剧承接层 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场首屏 |
| `assets/2026-07-23-theater-search.mp4` | 录屏 | 步骤 2-4 | 剧场搜索链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 打开剧场搜索页 | `adb shell input tap 260 116 && sleep 2` | 待执行 |
| HH:MM:SS | 打开识图入口 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 872 116 && sleep 3` | 待执行 |
| HH:MM:SS | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
