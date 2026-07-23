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
- **观察**：商城首页匿名可达，顶部有搜索框、购物车和“我的订单 / 卡券红包 / 我的钱包 / 短剧同款 / 国家补贴”快捷入口，中部为直播与商品混排双列瀑布流。页面底部固定出现“登录可查看商品价格”浮层，右侧提供“立即登录”按钮，说明价格查看能力被明确绑定到登录状态。
- **截图文件**：`assets/2026-07-23-step-01-mall-ready.png`

### 步骤 2：点击“立即登录”按钮

- **操作**：点击底部登录提示中的“立即登录”按钮
- **命令**：`adb shell input tap 832 2050 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-02-login-gate.png`
- **观察要点**：记录登录拦截页样式、登录方式、是否为全屏或弹层

*执行后由 subagent 填写：*
- **观察**：点击“立即登录”后，没有进入独立账号体系页，而是从底部上拉出半屏登录底板。底板标题为“完成抖音登录抢购超值好物”，包含手机号输入框、“获取验证码”按钮、协议勾选区与底部抖音快捷登录按钮；背景层仍能看到商城首页商品流，说明这是半屏拦截层而非全屏跳转页。
- **截图文件**：`assets/2026-07-23-step-02-login-gate.png`

### 步骤 3：返回商城并点击价格相关区

- **操作**：关闭登录底板后点击右侧商品卡的价格/购买相关区域
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 702 1116 && sleep 3`
- **截图**：保存为 `assets/2026-07-23-step-03-price-gate.png`
- **观察要点**：再次触发的拦截是否一致，是否存在不同类型登录门槛

*执行后由 subagent 填写：*
- **观察**：关闭底板后点击右侧商品卡的价格相关区域，再次触发与步骤 2 相同的半屏登录底板，文案、输入项与抖音快捷登录方式保持一致。说明“立即登录”按钮和价格相关点击区域命中的都是同一种登录门槛，并未分化出第二种价格专属拦截样式。
- **截图文件**：`assets/2026-07-23-step-03-price-gate.png`

### 步骤 4：返回商城首屏

- **操作**：返回商城首屏稳定态
- **命令**：`adb shell input keyevent KEYCODE_BACK && sleep 2`
- **截图**：保存为 `assets/2026-07-23-step-04-back-mall.png`
- **观察要点**：拦截关闭方式、商城恢复状态

*执行后由 subagent 填写：*
- **观察**：再次按返回键后，半屏登录底板被直接关闭，回到商城首页稳定态。页面恢复为匿名商品流浏览状态，底部“登录可查看商品价格 / 立即登录”浮层仍然存在，说明拦截可 dismiss，但匿名限制不会消失。
- **截图文件**：`assets/2026-07-23-step-04-back-mall.png`

## 录屏

- **录屏范围**：步骤 2 到步骤 4
- **保存为**：`assets/2026-07-23-mall-login-gates.mp4`

*执行后由 subagent 填写：*
- **录屏文件**：`assets/2026-07-23-mall-login-gates.mp4`

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
| 19:36:00 | 进入商城首屏 | `adb shell am force-stop com.phoenix.read && adb shell monkey -p com.phoenix.read -c android.intent.category.LAUNCHER 1 && sleep 5 && adb shell input tap 540 2240 && sleep 3` | 成功进入商城首页，底部显示“登录可查看商品价格”浮层 |
| 19:36:08 | 点击立即登录 | `adb shell input tap 832 2050 && sleep 3` | 触发半屏手机号验证码登录底板 |
| 19:36:14 | 点击价格相关区 | `adb shell input keyevent KEYCODE_BACK && sleep 1 && adb shell input tap 702 1116 && sleep 3` | 再次触发同一登录底板，拦截样式一致 |
| 19:36:20 | 返回商城首屏 | `adb shell input keyevent KEYCODE_BACK && sleep 2` | 关闭登录底板并回到商城首页稳定态 |

## 异常记录

- 商城首页的价格查看限制通过底部固定浮层显式提示，核心文案为“登录可查看商品价格”。
- “立即登录”按钮与商品价格相关点击区域都会触发同一套半屏登录底板，未发现第二种独立拦截样式。
- 登录拦截可通过返回键直接关闭，但关闭后匿名态限制仍保留，无法继续查看真实价格或下单能力。
