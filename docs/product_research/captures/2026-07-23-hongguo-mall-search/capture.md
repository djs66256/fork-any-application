# 采集：红果 — 商城搜索页

## 采集信息

| 项目 | 内容 |
|------|------|
| 采集日期 | 2026-07-23 |
| 目标竞品 | 红果 |
| 竞品版本 | 7.2.4.32 |
| 包名/标识 | com.phoenix.read |
| 频道 | mobile |
| 采集方案 | ADB |
| 功能模块 | mall |
| 采集范围 | 三级页面以内的商城搜索：首页 → 商城 → 搜索页 → 搜索结果或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入商城首屏

- **操作**：冷启动红果并切换到商城页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-mall-ready.png`
- **观察要点**：确认搜索框和搜索按钮可见

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击搜索框进入搜索页

- **操作**：点击商城搜索框
- **命令**：`adb shell input tap 260 120 && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-02-search-page.png`
- **观察要点**：搜索页结构、搜索历史、推荐词或热搜可见性

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：执行关键词搜索

- **操作**：输入“鞋”并提交搜索
- **命令**：`adb shell input tap 260 120 && sleep 1 && adb shell input text xie && adb shell input keyevent KEYCODE_ENTER && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-search-result.png`
- **观察要点**：结果页类型、商品卡样式、筛选/排序结构、是否登录门槛

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回商城首屏

- **操作**：连续返回至商城
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-mall.png`
- **观察要点**：返回路径、商城页面恢复状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-mall-search.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-mall-ready.png` | 截图 | 步骤 1 | 商城搜索入口状态 |
| `assets/2026-07-23-step-02-search-page.png` | 截图 | 步骤 2 | 商城搜索页 |
| `assets/2026-07-23-step-03-search-result.png` | 截图 | 步骤 3 | 商城搜索结果页 |
| `assets/2026-07-23-step-04-back-mall.png` | 截图 | 步骤 4 | 返回商城状态 |
| `assets/2026-07-23-mall-search.mp4` | 录屏 | 步骤 2-4 | 商城搜索链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入商城首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 打开商城搜索页 | `adb shell input tap 260 120 && sleep 2` | 待执行 |
| HH:MM:SS | 搜索关键词 | `adb shell input tap 260 120 && sleep 1 && adb shell input text xie && adb shell input keyevent KEYCODE_ENTER && sleep 3` | 待执行 |
| HH:MM:SS | 返回商城首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
