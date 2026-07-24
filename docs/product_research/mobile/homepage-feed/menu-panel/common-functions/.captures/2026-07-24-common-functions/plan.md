# 采集规划：红果 — 菜单抽屉常用功能区

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-24 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 目标页面 | mobile/homepage-feed/menu-panel/common-functions |
| 采集范围 | 菜单抽屉中的“常用功能”区入口结构、功能项语义与模块定位 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：记录菜单抽屉中的常用功能区

- **操作**：打开首页左侧菜单抽屉，聚焦“常用功能”模块
- **命令**：复用 `homepage-feed/menu-panel/.captures/2026-07-24-drawer-snapshot` 已验证截图中的同屏区域
- **截图**：保存为 `assets/2026-07-24-step-01-common-functions.png`
- **观察要点**：记录功能项数量、图标、命名以及该模块与上方续播区的层级关系

*采集阶段回填：*
- **观察**：“常用功能”位于菜单抽屉下部，以列表方式展示两个入口：“我的预约”“我的下载”，左侧均配有线性图标，右侧有进入箭头。该模块与上方“最近在看”“游戏中心”区隔明显，更偏用户工具与资产管理入口，而非内容分发入口。
- **截图文件**：`assets/2026-07-24-step-01-common-functions.png`

## 录屏

本次采集无需录屏。

*采集阶段回填：*
- **录屏文件**：无

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-24-step-01-common-functions.png` | 截图 | 步骤 1 | 菜单抽屉中的常用功能区 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| 15:46:55 | 归档常用功能区样本 | `cp homepage-feed/menu-panel/.captures/2026-07-24-drawer-snapshot/assets/2026-07-24-step-04-menu.png homepage-feed/menu-panel/common-functions/.captures/2026-07-24-common-functions/assets/2026-07-24-step-01-common-functions.png` | 成功，归档菜单抽屉首屏中常用功能区截图，用于拆分模块级文档 |

## 异常记录

- 本轮未单独重新采集 common-functions，因为父路径 `menu-panel` 的已验证样本已完整覆盖该模块的可见状态；因此直接复用同屏截图拆分模块级文档。

## 执行状态

- [x] 步骤 1：记录菜单抽屉中的常用功能区
