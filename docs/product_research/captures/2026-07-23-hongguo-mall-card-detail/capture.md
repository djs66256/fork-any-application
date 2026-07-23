# 采集：红果 — 商城商品详情承接页

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
| 采集范围 | 三级页面以内的商城商品链路：首页 → 商城 → 首个商品卡片 → 商品详情或登录门槛 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入商城首屏

- **操作**：冷启动红果并切换到商城页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-mall-ready.png`
- **观察要点**：确认商城首屏稳定、商品卡片可点击

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击首个商品卡片

- **操作**：点击左侧首个商品卡片
- **命令**：`adb shell input tap 260 930 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-product-detail.png`
- **观察要点**：进入商品详情页、直播页或登录门槛中的哪一种

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：在承接页点击首个核心 CTA

- **操作**：若匿名可达，点击首个核心 CTA（如购买、规格、详情展开）
- **命令**：`adb shell input tap 880 2110 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-product-secondary.png`
- **观察要点**：是否进入三级页、是否要求登录或授权

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回商城首屏

- **操作**：连续返回至商城首屏
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-mall.png`
- **观察要点**：返回路径和商城恢复状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-mall-card-detail.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-mall-ready.png` | 截图 | 步骤 1 | 商城首屏 |
| `assets/2026-07-23-step-02-product-detail.png` | 截图 | 步骤 2 | 商品承接页 |
| `assets/2026-07-23-step-03-product-secondary.png` | 截图 | 步骤 3 | 商品详情二级动作结果 |
| `assets/2026-07-23-step-04-back-mall.png` | 截图 | 步骤 4 | 返回商城状态 |
| `assets/2026-07-23-mall-card-detail.mp4` | 录屏 | 步骤 2-4 | 商城商品链路录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入商城首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击商品卡片 | `adb shell input tap 260 930 && sleep 3` | 待执行 |
| HH:MM:SS | 点击核心 CTA | `adb shell input tap 880 2110 && sleep 3` | 待执行 |
| HH:MM:SS | 返回商城首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
