# 采集：红果 — 首页菜单面板

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
| 采集范围 | 三级页面以内的首页左上菜单面板：首页首屏 → 打开菜单 → 记录菜单结构与匿名可达的首个承接层 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入首页首屏

- **操作**：冷启动红果并等待首页稳定
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5`
- **截图**：保存为 `assets/2026-07-23-step-01-home-ready.png`
- **观察要点**：记录首页可操作状态和菜单按钮可见性

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击左上菜单按钮

- **操作**：点击首页左上角菜单按钮
- **命令**：`adb shell input tap 72 116 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-menu-panel.png`
- **观察要点**：菜单是否侧滑/弹层，包含哪些分组和入口

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：选取首个匿名可达入口进入二级页

- **操作**：若菜单中存在无需登录入口，点击首个核心入口进入其承接页
- **命令**：`adb shell input tap 320 460 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-menu-entry-detail.png`
- **观察要点**：记录进入页类型、是否属于二级或三级页、是否被登录拦截

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回首页

- **操作**：返回上一级，确认菜单是否关闭且回到首页
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-home.png`
- **观察要点**：返回行为、菜单关闭方式、首页恢复状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-home-menu-panel.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-home-ready.png` | 截图 | 步骤 1 | 首页稳定态 |
| `assets/2026-07-23-step-02-menu-panel.png` | 截图 | 步骤 2 | 菜单面板结构 |
| `assets/2026-07-23-step-03-menu-entry-detail.png` | 截图 | 步骤 3 | 菜单入口承接页 |
| `assets/2026-07-23-step-04-back-home.png` | 截图 | 步骤 4 | 返回首页状态 |
| `assets/2026-07-23-home-menu-panel.mp4` | 录屏 | 步骤 2-4 | 首页菜单面板录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入首页首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5` | 待执行 |
| HH:MM:SS | 打开菜单 | `adb shell input tap 72 116 && sleep 2` | 待执行 |
| HH:MM:SS | 点击菜单入口 | `adb shell input tap 320 460 && sleep 3` | 待执行 |
| HH:MM:SS | 返回首页 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
