# 采集：红果 — 剧场排行榜入口

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
| 采集范围 | 三级页面以内的剧场排行榜：首页 → 剧场 → 排行榜 → 榜单首屏与返回 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入剧场首屏

- **操作**：冷启动并切换至剧场页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-theater-ready.png`
- **观察要点**：确认排行榜入口可见

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：打开排行榜

- **操作**：点击“排行榜”入口
- **命令**：`adb shell input tap 420 334 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-ranking-page.png`
- **观察要点**：记录榜单页结构、榜单分类、默认排序方式

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：点击榜单首项进入承接页

- **操作**：点击榜单首项或首个榜单标签
- **命令**：`adb shell input tap 260 760 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-ranking-detail.png`
- **观察要点**：进入详情页/剧集页/榜单分组页中的哪一种，是否属于三级页内

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回剧场首屏

- **操作**：连续返回到剧场首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-theater.png`
- **观察要点**：返回层级、榜单页是否保留滚动状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-theater-ranking.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-theater-ready.png` | 截图 | 步骤 1 | 剧场首屏 |
| `assets/2026-07-23-step-02-ranking-page.png` | 截图 | 步骤 2 | 排行榜页首屏 |
| `assets/2026-07-23-step-03-ranking-detail.png` | 截图 | 步骤 3 | 榜单承接页 |
| `assets/2026-07-23-step-04-back-theater.png` | 截图 | 步骤 4 | 返回剧场状态 |
| `assets/2026-07-23-theater-ranking.mp4` | 录屏 | 步骤 2-4 | 剧场排行榜链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入剧场首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 314 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 打开排行榜 | `adb shell input tap 420 334 && sleep 3` | 待执行 |
| HH:MM:SS | 点击榜单内容 | `adb shell input tap 260 760 && sleep 3` | 待执行 |
| HH:MM:SS | 返回剧场首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
