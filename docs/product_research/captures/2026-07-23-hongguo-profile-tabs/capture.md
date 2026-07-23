# 采集：红果 — 我的页标签体系

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
| 采集范围 | 三级页面以内的我的页标签体系：首页 → 我的 → 收藏/点赞/预约/动态标签首屏与过滤行为 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入我的页首屏

- **操作**：冷启动红果并切换到“我的”页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-profile-ready.png`
- **观察要点**：默认选中标签、内容区数据状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：切换到“收藏”标签

- **操作**：点击“收藏”标签
- **命令**：`adb shell input tap 262 696 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-favorite-tab.png`
- **观察要点**：是否有数据、空状态样式、顶部按钮变化

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：切换到“点赞”或“预约”标签

- **操作**：点击“点赞”标签；如无明显变化可继续点“预约”
- **命令**：`adb shell input tap 410 696 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-03-like-or-booking.png`
- **观察要点**：记录不同标签的数据形态与是否登录限制

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：切回历史并记录过滤项

- **操作**：点击“历史”，再点击“已看完”过滤项
- **命令**：`adb shell input tap 114 696 && sleep 1 && adb shell input tap 292 810 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-history-filter.png`
- **观察要点**：过滤器生效方式、内容刷新模式、是否保留标签状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-profile-tabs.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-profile-ready.png` | 截图 | 步骤 1 | 我的页默认首屏 |
| `assets/2026-07-23-step-02-favorite-tab.png` | 截图 | 步骤 2 | 收藏标签状态 |
| `assets/2026-07-23-step-03-like-or-booking.png` | 截图 | 步骤 3 | 点赞或预约标签状态 |
| `assets/2026-07-23-step-04-history-filter.png` | 截图 | 步骤 4 | 历史标签过滤状态 |
| `assets/2026-07-23-profile-tabs.mp4` | 录屏 | 步骤 2-4 | 我的页标签切换录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入我的页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 970 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 切换收藏标签 | `adb shell input tap 262 696 && sleep 2` | 待执行 |
| HH:MM:SS | 切换点赞/预约标签 | `adb shell input tap 410 696 && sleep 2` | 待执行 |
| HH:MM:SS | 记录历史过滤项 | `adb shell input tap 114 696 && sleep 1 && adb shell input tap 292 810 && sleep 2` | 待执行 |

## 异常记录

待执行
