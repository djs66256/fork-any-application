# 采集：红果 — 赚钱任务入口

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | earn-center |
| 采集范围 | 三级页面以内的赚钱任务入口：首页 → 赚钱页 → 新人任务/连续看剧福利/现金打款入口中的首个匿名可达路径 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入赚钱首屏并关闭签到弹窗

- **操作**：冷启动进入赚钱页后关闭签到弹窗
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 760 2240 && sleep 3 && adb shell input tap 540 1830 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-01-earn-ready.png`
- **观察要点**：记录赚钱首屏可点击任务区域

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击首个任务 CTA

- **操作**：点击首个核心任务按钮（如“立即领取”或“去看剧”）
- **命令**：`adb shell input tap 842 898 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-task-detail.png`
- **观察要点**：进入任务详情、播放承接页或登录门槛中的哪一种

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：在承接页点击首个核心动作

- **操作**：若匿名可达，点击一级 CTA 继续下钻一次
- **命令**：`adb shell input tap 880 2110 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-task-secondary.png`
- **观察要点**：是否进入三级页、是否要求登录

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回赚钱首页

- **操作**：连续返回至赚钱首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-earn.png`
- **观察要点**：返回路径、任务状态变化、赚钱页恢复情况

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-earn-task-entries.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-earn-ready.png` | 截图 | 步骤 1 | 关闭弹窗后的赚钱首屏 |
| `assets/2026-07-23-step-02-task-detail.png` | 截图 | 步骤 2 | 任务入口承接页 |
| `assets/2026-07-23-step-03-task-secondary.png` | 截图 | 步骤 3 | 二级任务动作结果 |
| `assets/2026-07-23-step-04-back-earn.png` | 截图 | 步骤 4 | 返回赚钱首屏状态 |
| `assets/2026-07-23-earn-task-entries.mp4` | 录屏 | 步骤 2-4 | 赚钱任务入口录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入赚钱首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 760 2240 && sleep 3 && adb shell input tap 540 1830 && sleep 2` | 待执行 |
| HH:MM:SS | 点击任务 CTA | `adb shell input tap 842 898 && sleep 3` | 待执行 |
| HH:MM:SS | 点击二级动作 | `adb shell input tap 880 2110 && sleep 3` | 待执行 |
| HH:MM:SS | 返回赚钱首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
