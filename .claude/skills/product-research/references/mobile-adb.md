# 通用采集能力指南

通过 ADB 控制 Android 模拟器，执行竞品应用的操控、截图和录屏。

## 连接与状态

```bash
adb devices                                    # 列出设备
adb shell wm size                              # 屏幕分辨率（用于坐标计算）
adb shell wm density                           # 屏幕密度
adb shell dumpsys window | grep mCurrentFocus  # 当前页面
```

## 应用控制

```bash
adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1  # 启动
adb shell am force-stop <package>                                      # 停止
adb shell pm clear <package>                                           # 清除数据（恢复初始状态）
adb shell pm list packages -3                                          # 第三方应用
adb shell pm list packages | grep <keyword>                            # 搜索包名
```

## 触摸操作

```bash
adb shell input tap <x> <y>                                    # 点击
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>       # 滑动
adb shell input swipe <x> <y> <x> <y> <duration_ms>           # 长按
```

### 常见滑动（1080×2400 基准）

| 操作 | 命令 |
|------|------|
| 向上浏览 | `swipe 540 1600 540 400 300` |
| 向下返回 | `swipe 540 400 540 1600 300` |
| 左滑 | `swipe 900 1200 200 1200 300` |
| 右滑 | `swipe 200 1200 900 1200 300` |
| 慢速（观察动画） | `swipe 540 1600 540 800 800` |

## 文字输入

```bash
adb shell input text "hello"                    # ASCII
adb shell cmd clipboard set "中文文本"           # 中文（Android 12+）
adb shell input keyevent 279                    # KEYCODE_PASTE
```

## 按键

```bash
adb shell input keyevent KEYCODE_BACK       # 返回
adb shell input keyevent KEYCODE_HOME       # Home
adb shell input keyevent KEYCODE_ENTER      # 回车
adb shell input keyevent KEYCODE_DEL        # 删除
```

## 截图

```bash
adb exec-out screencap -p > <path>.png
```

## 录屏

```bash
# 开始（最长 180s，Android screenrecord 硬限制）
adb shell screenrecord --time-limit 180 --bit-rate 4000000 /sdcard/video.mp4
# 结束后拉取
adb pull /sdcard/video.mp4 <path>.mp4
adb shell rm /sdcard/video.mp4
```

> 超过 180 秒需分段录制。screenrecord 不支持音频，如需音频用 scrcpy 代替。

## 元素定位

```bash
# 导出 UI 层级
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml /tmp/ui.xml
# 搜索 bounds="[left,top][right,bottom]"
# 点击中心 = ((left+right)/2, (top+bottom)/2)
```

### 坐标估算（1080×2400）

| 区域 | 比例 | 坐标 |
|------|------|------|
| 状态栏 | y: 0–5% | y: 0–120 |
| 顶部导航 | y: 5–12% | y: 120–290 |
| 主内容区 | y: 12–85% | y: 290–2040 |
| 底部导航 | y: 85–100% | y: 2040–2400 |
| 返回按钮 | x: 0–8%, y: 5–12% | (40, 170) |
| 屏幕中心 | x: 50%, y: 50% | (540, 1200) |
| Feed 首卡中心 | x: 50%, y: 35% | (540, 840) |

## 常见操作组合

### 启动并截图首页

```bash
adb shell am force-stop <package>
adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1
sleep 3
adb exec-out screencap -p > assets/<日期>-step-01-home.png
```

### 浏览 Feed（逐屏截图）

```bash
for i in $(seq 1 5); do
  adb shell input swipe 540 1600 540 400 300
  sleep 1.5
  adb exec-out screencap -p > "assets/<日期>-step-$(printf '%02d' $((i+1)))-scroll.png"
done
```

## 排查

| 问题 | 原因 | 解决 |
|------|------|------|
| `no devices/emulators found` | 模拟器未连接 | `adb kill-server && adb start-server` |
| `device unauthorized` | 未授权 | 模拟器上确认 USB 调试弹窗 |
| 截图全黑/白 | 过渡动画中 | 等待 2-3s |
| `screenrecord` 报错 | 进程冲突 | `adb shell killall screenrecord` |
| 中文乱码 | ADB 不支持 Unicode | 用剪贴板粘贴方式 |
| 坐标失效 | 非标准布局 | `uiautomator dump` 重新定位 |
