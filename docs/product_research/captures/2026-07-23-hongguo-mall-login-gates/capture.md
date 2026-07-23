# 采集：红果 — 商城登录门槛

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
| 采集范围 | 三级页面以内的商城匿名与登录边界：首页 → 商城 → 触发“立即登录”或价格/交易门槛 → 记录拦截形式 |

## 命令参考

采集过程中的命令参考 `references/mobile-adb.md`。

## 操作序列

### 步骤 1：进入商城首屏

- **操作**：冷启动后切换到商城页
- **命令**：`adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-01-mall-ready.png`
- **观察要点**：记录底部“登录可查看商品价格”浮层是否出现

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 2：点击“立即登录”按钮

- **操作**：点击底部登录提示中的“立即登录”按钮
- **命令**：`adb shell input tap 832 2050 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-login-gate.png`
- **观察要点**：记录登录拦截页样式、登录方式、是否为全屏或弹层

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 3：返回商城并点击价格相关区

- **操作**：返回商城后点击商品价格区域或购买相关入口
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 702 1116 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-price-gate.png`
- **观察要点**：再次触发的拦截是否一致，是否存在不同类型登录门槛

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

### 步骤 4：返回商城首屏

- **操作**：返回商城首屏稳定态
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-mall.png`
- **观察要点**：拦截关闭方式、商城恢复状态

*执行后由 subagent 填写：*
- **观察**：
- **截图文件**：

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-mall-login-gates.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：

## 产物清单

| 文件 | 类型 | 对应步骤 | 说明 |
|------|------|---------|------|
| `assets/2026-07-23-step-01-mall-ready.png` | 截图 | 步骤 1 | 商城登录提示状态 |
| `assets/2026-07-23-step-02-login-gate.png` | 截图 | 步骤 2 | 立即登录拦截页 |
| `assets/2026-07-23-step-03-price-gate.png` | 截图 | 步骤 3 | 价格相关拦截 |
| `assets/2026-07-23-step-04-back-mall.png` | 截图 | 步骤 4 | 返回商城状态 |
| `assets/2026-07-23-mall-login-gates.mp4` | 录屏 | 步骤 2-4 | 商城登录门槛录屏 |

## 采集日志

| 时间 | 操作 | 命令 | 结果 |
|------|------|------|------|
| HH:MM:SS | 进入商城首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3` | 待执行 |
| HH:MM:SS | 点击立即登录 | `adb shell input tap 832 2050 && sleep 3` | 待执行 |
| HH:MM:SS | 点击价格相关区 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 702 1116 && sleep 3` | 待执行 |
| HH:MM:SS | 返回商城首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 待执行 |

## 异常记录

待执行
