# 采集：红果 — 剧场筛选器

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
| 采集范围 | 三级页面以内的剧场筛选能力：首页 → 剧场 → 筛选面板 → 应用一次筛选并观察结果页 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动后切换到底部剧场 Tab
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：确认剧场首屏已稳定，“筛选”入口可见

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：打开筛选面板

- **操作**：点击剧场页“筛选”按钮
- **命令**：`adb shell input tap 146 334 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-filter-panel.png`
- **观察要点**：筛选以弹层/页形式出现，包含哪些维度

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：应用首个匿名可用筛选条件

- **操作**：点击一个可见筛选项并确认/应用
- **命令**：`adb shell input tap 220 640 && sleep 1 && adb shell input tap 890 2110 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-filter-result.png`
- **观察要点**：结果列表是否刷新、筛选条件是否回显、是否进入二级结果页

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回剧场首屏

- **操作**：返回原剧场页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回层级、筛选状态保留与否

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-filter.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏 |
| `assets/2026-07-23-step-02-filter-panel.png` | 截图 | 步骤 2 | 筛选面板 |
| `assets/2026-07-23-step-03-filter-result.png` | 截图 | 步骤 3 | 应用筛选后的结果 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场首屏状态 |
| `assets/2026-07-23-theater-filter.mp4` | 录屏 | 步骤 2-4 | 剧场筛选链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 打开筛选面板 | `adb shell input tap 146 334 && sleep 2` | 待执行 |
| HH:MM:SS | 应用筛选条件 | `adb shell input tap 220 640 && sleep 1 && adb shell input tap 890 2110 && sleep 3` | 待执行 |
| HH:MM:SS | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
