# AI 操作与自动化 — iOS

> 本文档定义 iOS 端 AI agent 可执行的设备操作与自动化能力。
> 所有命令假设已安装 Xcode 27+ 和 Simulator。

---

## 0. Xcode MCP（推荐方式）

Xcode 27+ 内置了 MCP（Model Context Protocol）Server，AI agent 通过 MCP 协议直接与 Xcode 交互，无需手动执行 CLI 命令。
这是 **最推荐** 的操作方式——语义化调用、自动获取结果、支持丰富的 Xcode 原生能力。

### 0.1 启用 Xcode MCP

在 Xcode 27 中，MCP Server 默认开启。AI agent 通过 MCP 工具调用即可操作 Xcode。

Xcode MCP 提供的核心能力（摘要，以实际 tool list 为准）：

| 能力域 | 说明 | 替代的传统方式 |
|--------|------|---------------|
| **项目管理** | 打开/关闭 project、列出 targets、获取 scheme 列表 | 手动 Xcode 操作 |
| **构建** | 触发 build、clean、test、archive，获取构建结果与错误 | `xcodebuild` CLI |
| **运行** | 启动/停止模拟器、选择设备、安装 app、启动 app | `simctl` + `xcodebuild` |
| **调试** | 设置断点、单步执行、读取变量值、查看调用栈 | LLDB CLI / Xcode GUI |
| **UI 检查** | 获取 UI 层级树、查询元素属性（frame、label、identifier）、查找匹配元素 | Accessibility Inspector / `po` |
| **UI 交互** | 模拟点击、滑动、文本输入、手势操作 | XCUITest / `simctl` 有限命令 |
| **截图** | 截取模拟器/设备屏幕（完整截图或指定元素截图） | `simctl io screenshot` |
| **代码编辑** | 打开文件、定位到行、获取代码补全建议 | Xcode Source Editor |
| **日志** | 获取运行时日志、过滤 subsystem/category/级别 | `log stream` |
| **模拟器管理** | 创建/删除/启动/关闭模拟器、管理运行时、擦除数据 | `simctl` CLI |
| **设备管理** | 列出真机、安装应用到真机、获取设备信息 | `xctrace` / `cfgutil` |

### 0.2 MCP 操作示例

以下为通过 MCP 执行典型任务的示意流程：

**构建并运行 App：**

```
1. mcp: build_project(scheme: "ShortDrama", destination: "iPhone 15 Pro")
   → 返回构建日志、错误（如有）
2. mcp: boot_simulator(device: "iPhone 15 Pro")  → 启动模拟器
3. mcp: install_app(bundleId: "com.djs66256.short_drama")  → 安装 app
4. mcp: launch_app(bundleId: "com.djs66256.short_drama")  → 启动 app
```

**UI 交互式截图：**

```
1. mcp: launch_app(bundleId: "com.djs66256.short_drama")  → 启动 app
2. mcp: screenshot()  → 截取首页
3. mcp: tap(x: 160, y: 300)  → 点击短剧卡片
4. mcp: wait_for_element(identifier: "player_view", timeout: 5)  → 等待播放器
5. mcp: screenshot()  → 截取播放页
```

**UI 树检查：**

```
1. mcp: get_ui_tree()  → 获取完整 UI 层级 JSON
2. mcp: find_element(identifier: "drama_card_0")  → 定位特定元素
3. mcp: get_element_attributes(identifier: "drama_card_0")
   → 返回 frame、label、isEnabled 等属性
```

**调试与日志：**

```
1. mcp: set_breakpoint(file: "HomeViewModel.swift", line: 42)  → 设断点
2. mcp: launch_app(bundleId: "...")  → 启动触发断点
3. mcp: get_variable("viewModel.dramas")  → 读取变量
4. mcp: stream_logs(subsystem: "com.djs66256.short_drama", level: "debug")  → 实时日志
```

### 0.3 MCP 优先原则

- **能用 MCP 就不用 CLI**。MCP 操作语义化、结果结构化、错误处理完善。
- 当 MCP 不支持某个操作时（如复杂的 simctl 隐私权限重置），再回退到 CLI。
- MCP 不可用时（Xcode 版本 < 27、MCP Server 未启动），使用下方 CLI 替代方案。

---

## 1. 模拟器控制 (simctl) —— CLI 备用方案

以下 CLI 命令作为 MCP 不可用时的备用方案。

`xcrun simctl` 是 Xcode 自带的模拟器控制工具，所有操作通过它完成。

### 1.1 设备管理

```bash
# 列出所有可用设备（含状态）
xcrun simctl list devices

# 仅列出已启动的设备
xcrun simctl list devices | grep Booted

# 列出所有可用的运行时（iOS 版本）
xcrun simctl list runtimes

# 列出所有设备类型
xcrun simctl list devicetypes

# 创建新模拟器
# 格式：xcrun simctl create "<名称>" "<设备类型>" "<运行时>"
xcrun simctl create "ShortDrama-17.4" "iPhone 15 Pro" "com.apple.CoreSimulator.SimRuntime.iOS-17-4"

# 查看创建结果（返回 UDID）
xcrun simctl create "test-device" "iPhone 15 Pro" "com.apple.CoreSimulator.SimRuntime.iOS-17-4"
# → 0F5D1A9C-8B2D-4E3F-9A1B-2C3D4E5F6A7B

# 启动模拟器
xcrun simctl boot "ShortDrama-17.4"
# 或通过 UDID 启动
xcrun simctl boot 0F5D1A9C-8B2D-4E3F-9A1B-2C3D4E5F6A7B

# 启动后打开 Simulator.app 界面
open -a Simulator

# 关机
xcrun simctl shutdown "ShortDrama-17.4"

# 关机所有模拟器
xcrun simctl shutdown all

# 删除模拟器
xcrun simctl delete "ShortDrama-17.4"
xcrun simctl delete 0F5D1A9C-8B2D-4E3F-9A1B-2C3D4E5F6A7B

# 擦除模拟器所有数据（重置为出厂状态）
xcrun simctl erase "ShortDrama-17.4"

# 获取已启动模拟器的 UDID
xcrun simctl list devices | grep Booted | head -1 | awk -F'[()]' '{print $2}'
```

### 1.2 应用管理

```bash
# 安装 .app 到模拟器
xcrun simctl install booted /path/to/ShortDrama.app

# 安装 IPA 到模拟器
xcrun simctl install booted /path/to/ShortDrama.ipa

# 卸载应用（使用 bundle identifier）
xcrun simctl uninstall booted com.djs66256.short_drama

# 启动应用
xcrun simctl launch booted com.djs66256.short_drama

# 启动应用并传入启动参数
xcrun simctl launch booted com.djs66256.short_drama -debug -resetData

# 启动应用并传入 URL（模拟 Deep Link）
xcrun simctl launch booted com.djs66256.short_drama djsdrama://drama/12345

# 终止应用
xcrun simctl terminate booted com.djs66256.short_drama

# 获取应用容器路径
xcrun simctl get_app_container booted com.djs66256.short_drama
# → /Users/xxx/Library/Developer/CoreSimulator/Devices/.../data/Containers/Bundle/.../ShortDrama.app

# 获取数据目录路径
xcrun simctl get_app_container booted com.djs66256.short_drama data
# → /Users/xxx/Library/Developer/CoreSimulator/Devices/.../data/Containers/Data/Application/.../
```

### 1.3 截图与录屏

```bash
# 截图（保存到指定路径）
xcrun simctl io booted screenshot /tmp/screenshot.png

# 指定截图格式（png / tiff / bmp / gif / jpeg）
xcrun simctl io booted screenshot /tmp/screenshot.jpg

# 截图并指定显示方式（可附带 mask）
xcrun simctl io booted screenshot --type=png --mask=black /tmp/masked.png

# 录制视频（按 Ctrl+C 停止）
xcrun simctl io booted recordVideo /tmp/screen_record.mp4

# 录制视频指定格式（h264 / hevc）
xcrun simctl io booted recordVideo --codec=h264 /tmp/record.mp4

# 录制视频到文件，在后台运行，通过 kill 停止
xcrun simctl io booted recordVideo /tmp/record.mp4 &
RECORD_PID=$!
sleep 10
kill $RECORD_PID
```

### 1.4 系统操作

```bash
# —— 状态栏 ——
# 覆盖状态栏显示（时间、电池、信号）
xcrun simctl status_bar booted override \
  --time "9:41" \
  --dataNetwork "wifi" \
  --wifiMode "active" \
  --cellularMode "active" \
  --batteryState "charged" \
  --batteryLevel 100

# 清除状态栏覆盖
xcrun simctl status_bar booted clear

# —— 隐私权限 ——
# 授予权限
xcrun simctl privacy booted grant photos com.djs66256.short_drama
xcrun simctl privacy booted grant camera com.djs66256.short_drama
xcrun simctl privacy booted grant microphone com.djs66256.short_drama
xcrun simctl privacy booted grant location-always com.djs66256.short_drama

# 撤销权限
xcrun simctl privacy booted revoke photos com.djs66256.short_drama

# 重置所有权限
xcrun simctl privacy booted reset all com.djs66256.short_drama

# 查看权限状态
xcrun simctl privacy booted list com.djs66256.short_drama

# —— Deep Link / URL 打开 ——
# 通过 URL Scheme 打开应用
xcrun simctl openurl booted "djsdrama://drama/12345"
xcrun simctl openurl booted "djsdrama://play/67890?episode=3"
xcrun simctl openurl booted "https://shortdrama.example.com/app"

# —— 推送通知 ——
# 发送静默推送（payload 为 JSON 文件路径）
xcrun simctl push booted com.djs66256.short_drama /path/to/payload.json

# payload.json 示例内容：
# { "aps": { "alert": { "title": "新短剧上线", "body": "第5集已更新" }, "badge": 1 } }

# —— 通知中心 ——
# 发送本地通知（测试用）
xcrun simctl spawn booted notifyutil -p "com.djs66256.short_drama" -s "TestNotification"

# —— 系统设置 ——
# 模拟设备旋转
xcrun simctl status_bar booted override --orientation "landscape-left"

# 设置语言和地区
xcrun simctl boot <UDID>
# 注意：语言和地区需要在模拟器创建时通过 plist 设置，或在 Settings.app 中手动操作

# 模拟 iCloud 登录状态
# 在 Simulator → Features → iCloud 中操作，无 CLI 等效命令
```

### 1.5 键盘与输入

```bash
# 切换软件键盘显示/隐藏
# 快捷键：Cmd+K（在模拟器获得焦点时）

# 写入文本到当前焦点控件
xcrun simctl spawn booted xcrun simctl io booted input "Hello ShortDrama"

# 模拟硬件键盘语言切换
# 在 Simulator → I/O → Keyboard → 切换 "Connect Hardware Keyboard"

# 粘贴剪贴板到模拟器
xcrun simctl pbcopy booted  # 复制模拟器剪贴板到 Mac
echo "test content" | xcrun simctl pbpaste booted  # Mac 剪贴板内容不直接粘贴到模拟器
# 实际做法：用 UI 自动化 XCUITest 的 typeText
```

---

## 2. UI 自动化

> **优先使用 MCP 的 `tap`、`swipe`、`type_text`、`get_ui_tree` 等工具进行交互式 UI 操作。**XCUITest 适用于需要断言和自动化的回归测试场景。

### 2.1 XCUITest（自动化回归测试）

- 使用 Xcode 内置的 XCUITest 框架编写 UI 自动化测试。
- 测试文件放在 `ios/ShortDrama/Tests/UITests/`。
- 基本流程：

```swift
import XCTest

final class HomePageUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = ["-UITesting"]
        app.launch()
    }

    func testTapDramaCardOpensPlayer() {
        let firstCard = app.scrollViews.otherElements.buttons.firstMatch
        XCTAssertTrue(firstCard.waitForExistence(timeout: 5))
        firstCard.tap()
        let playerView = app.otherElements["player_view"]
        XCTAssertTrue(playerView.waitForExistence(timeout: 3))
    }
}
```

- 在 View 中添加 `accessibilityIdentifier` 以便定位：
  ```swift
  DramaCard(drama: drama)
      .accessibilityIdentifier("drama_card_\(drama.id)")
  ```

### 2.2 UI 树获取

```bash
# 通过 XCUITest 导出当前 UI 树（在测试代码中）
# 运行时打印 UI 层级
po app.debugDescription
po app.staticTexts.allElementsBoundByIndex.map { $0.label }

# 通过 simctl 导出 Accessibility 树
xcrun simctl io booted screenshot /tmp/screenshot.png
# 辅助方式：通过 Accessibility Inspector 查看 UI 树
# Xcode → Open Developer Tool → Accessibility Inspector
# 选择 Simulator 为目标，查看元素树
```

```swift
// 在测试中递归打印 UI 树
func printUITree(_ element: XCUIElement, indent: Int = 0) {
    let prefix = String(repeating: "  ", count: indent)
    let id = element.identifier.isEmpty ? "-" : element.identifier
    let label = element.label.isEmpty ? "-" : element.label
    print("\(prefix)\(element.elementType): id=\(id), label=\(label)")
    for child in element.children(matching: .any).allElementsBoundByIndex {
        printUITree(child, indent: indent + 1)
    }
}
```

---

## 3. 截图与视觉比对

> **优先使用 MCP 的 `screenshot` 工具**——支持全屏截图和指定元素截图，结果直接返回。

### 3.1 截图采集（CLI 备用方案）

- 自动化截图采集的完整流程：

```bash
# 1. 确保模拟器已启动
BOOTED=$(xcrun simctl list devices | grep Booted | head -1 | awk -F'[()]' '{print $2}')
if [ -z "$BOOTED" ]; then
  echo "未找到已启动的模拟器"
  exit 1
fi

# 2. 安装并启动 App
xcrun simctl install "$BOOTED" ios/.build/ShortDrama.app
xcrun simctl launch "$BOOTED" com.djs66256.short_drama

# 3. 等待 App 加载（简单 sleep，或用 UI 测试等待特定元素）
sleep 3

# 4. 截图
xcrun simctl io "$BOOTED" screenshot /tmp/screenshot_home.png

# 5. 导航到特定页面（通过 URL Scheme）
xcrun simctl openurl "$BOOTED" "djsdrama://drama/12345"
sleep 2
xcrun simctl io "$BOOTED" screenshot /tmp/screenshot_detail.png
```

- 批量截图脚本放在 `ios/Scripts/capture_screenshots.sh`。
- 截图命名规范：`{功能模块}_{页面}_{状态}.png`，如 `home_main_loaded.png`、`player_episode_playing.png`。

### 3.2 设计稿比对

- 使用 **SwiftSnapshotTesting** 库（open-source-libs.md 🔶）进行自动视觉回归测试。
- 参考截图存放路径：`ios/ShortDrama/Tests/Snapshots/`。
- 比对流程：
  1. 在 XCUITest 或 ViewInspector 中渲染目标页面。
  2. 使用 `assertSnapshot(of:view, as: .image)` 生成快照。
  3. 与参考图片逐像素比对，差异在可接受阈值内（默认 1% 像素差异）。
- 与 Figma 设计稿手动比对流程：
  1. 从 Figma 导出设计稿为 PNG（2x 或 3x 分辨率）。
  2. 截图模拟器中的实际页面。
  3. 使用图片比对工具（如 `compare` from ImageMagick）叠加对比：
     ```bash
     compare -compose src design.png screenshot.png diff.png
     ```
- CI 中集成快照测试：在 Fastlane `test` lane 中设置 `SIMULATOR_DEVICE_NAME=iPhone 15 Pro` 确保一致性。

---

## 4. 日志采集

### 4.1 系统日志

```bash
# 查看系统日志（启动时至今的所有日志，含模拟器内核日志）
xcrun simctl spawn booted log show --last 1h

# 实时流式日志（tail -f 等效）
xcrun simctl spawn booted log stream

# 过滤特定进程的日志
xcrun simctl spawn booted log stream --predicate 'process == "ShortDrama"'

# 过滤特定子系统的日志
xcrun simctl spawn booted log stream --predicate 'subsystem == "com.djs66256.short_drama"'

# 组合过滤：进程 + 级别
xcrun simctl spawn booted log stream \
  --predicate 'process == "ShortDrama" AND messageType >= error'

# 过滤多个子系统的日志
xcrun simctl spawn booted log stream \
  --predicate 'subsystem IN {"com.djs66256.short_drama.network", "com.djs66256.short_drama.player"}'

# 导出日志到文件
xcrun simctl spawn booted log show --last 30m > /tmp/simulator_log.txt

# 按时间范围导出
xcrun simctl spawn booted log show --start "2026-07-23 10:00:00" --end "2026-07-23 11:00:00" > /tmp/log.txt
```

### 4.2 OSLog

```swift
import OSLog

// 定义日志子系统
extension OSLog {
    static let network = OSLog(subsystem: "com.djs66256.short_drama", category: "network")
    static let player = OSLog(subsystem: "com.djs66256.short_drama", category: "player")
    static let auth = OSLog(subsystem: "com.djs66256.short_drama", category: "auth")
    static let ui = OSLog(subsystem: "com.djs66256.short_drama", category: "ui")
}

// 使用示例
os_log(.debug, log: .network, "请求发起：%{public}@", url.absoluteString)
os_log(.error, log: .player, "播放失败：%{public}@", error.localizedDescription)
os_log(.info, log: .auth, "用户登录成功，ID：%{private}@", userId)  // %{private} 在非 debug 下隐藏
```

- `log stream` 的 `--predicate` 基于 `subsystem` 和 `category` 过滤。
- `%{public}@` 在 Release 构建中可见，`%{private}@` 仅在 debug 构建中可见。
- `Logger`（swift-log）与 `OSLog` 并存时，统一以 `OSLog` 为底层输出后端，`swift-log` 做 API 抽象。

---

## 5. 真机调试

### 5.1 设备连接

```bash
# 列出所有连接的真机（含 USB 和 WiFi）
xcrun xctrace list devices
# 或（旧命令）
instruments -s devices

# 列出已连接真机的基本信息
cfgutil list

# 通过 USB 连接：
# 1. 用数据线连接 iPhone 和 Mac
# 2. 在 iPhone 上信任此电脑（弹出信任对话框时点击"信任"）
# 3. Xcode → Window → Devices and Simulators 中确认设备出现

# 配置 WiFi 连接：
# 1. 先用 USB 连接
# 2. Xcode → Window → Devices and Simulators → 选择设备
# 3. 勾选 "Connect via network"
# 4. WiFi 调试生效后，可断开 USB
```

### 5.2 设备管理

```bash
# 获取设备 UDID
xcrun xctrace list devices | grep -v Simulator | head -1

# cfgutil 安装应用到真机（需先安装 Apple Configurator）
cfgutil install-app /path/to/ShortDrama.ipa

# 通过 ios-deploy 安装和启动
ios-deploy --bundle /path/to/ShortDrama.app

# 通过 libimobiledevice 查看设备信息
ideviceinfo
idevice_id -l  # 仅列出 UDID

# 查看真机日志（通过 idevicesyslog）
idevicesyslog

# 查看真机截图
idevicescreenshot /tmp/device_screenshot.png
```

### 5.3 注意事项

- 真机调试需要有效 Apple Developer 账号且设备已加入 Provisioning Profile。
- 无线调试要求 iOS 和 macOS 在同一 WiFi 网络，且路由器支持 Bonjour。
- 真机上的 OSLog 可通过 Xcode → Devices → 选择设备 → "Open Console" 查看。
