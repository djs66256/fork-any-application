# 常见问题 — iOS

> 本文档收集 iOS 开发中的常见问题与解决方案。
> 每个问题包含：错误现象、根本原因、解决方案。

---

## 构建问题

### Q1: `No such module 'Alamofire'`

**现象**：
```
No such module 'Alamofire'
```
编译时报找不到 Alamofire（或 Kingfisher 等 SPM 依赖）。

**原因**：SPM 依赖未正确解析。可能因为 `project.yml` 变更后未重新生成 `.xcodeproj`，或 SPM 缓存损坏。

**解决方案**：
```bash
# 1. 重新生成工程文件
cd ios && xcodegen generate

# 2. Xcode 中 File → Packages → Reset Package Caches

# 3. 若仍不行，清除派生数据
rm -rf ~/Library/Developer/Xcode/DerivedData/ShortDrama-*
```

---

### Q2: `Cannot find 'XCTest' in scope`

**现象**：测试文件中 `XCTest` 相关类型无法识别。

**原因**：测试文件未添加到正确的 target（应属于 `ShortDramaTests` 而非 `ShortDrama`）。

**解决方案**：
1. 检查 `project.yml` 中测试 target 的 `sources` 是否包含目标目录。
2. 确认文件在 Xcode 的 Target Membership 中勾选了测试 target。
3. 确保 `import XCTest` 在文件顶部。

---

### Q3: `Swift Compiler Error: Segmentation fault: 11`

**现象**：编译时 Xcode 崩溃或报 segmentation fault。

**原因**：Swift 编译器 bug，常见于复杂泛型 + SwiftUI `some View` 组合。

**解决方案**：
1. 拆分过大的 View `body`：将一个巨型 View 拆成多个子 View。
2. 简化泛型约束，考虑用 `any View` 替代复杂泛型（注意性能）。
3. 升级 Xcode 到最新稳定版。
4. 临时方案：找到崩溃的具体方法，添加显式返回类型注解替代类型推断。

---

### Q4: `Cycle in dependencies between targets`

**现象**：
```
Cycle in dependencies between targets 'ShortDramaData' and 'ShortDramaDomain'
```

**原因**：模块间相互依赖（A depends on B, B depends on A）。常见于 Data 层直接 import 了 UI 模块。

**解决方案**：
1. 严格遵循架构分层：`Data` 不依赖 `UI`，`UI` 不直接依赖 `Data`。
2. 从 `project.yml` 中移除循环依赖。
3. 如果需要在模块间共享类型，将共享代码抽到 `Core` 模块。

---

## 签名问题

### Q5: `Signing for "ShortDrama" requires a development team`

**现象**：
```
Signing for "ShortDrama" requires a development team.
Select a development team in the Signing & Capabilities editor.
```

**原因**：`project.yml` 中未设置 `DEVELOPMENT_TEAM`。

**解决方案**：
```bash
# 在 project.yml 中设置
DEVELOPMENT_TEAM: "YOUR_TEAM_ID"

# 或通过环境变量传入
DEV_TEAM=YOUR_TEAM_ID xcodegen generate
```
在 Xcode → Signing & Capabilities → 选择 Team → 勾选 "Automatically manage signing"（Debug 模式）。

---

### Q6: `No profiles for 'com.djs66256.short_drama' were found`

**现象**：Archive 时报没有匹配的 Provisioning Profile。

**原因**：App Store 构建需要手动签名 + 有效的 Distribution Profile。

**解决方案**：
```bash
# 使用 Fastlane match 同步 Profile
cd ios && bundle exec fastlane match appstore --readonly
```
或在 Apple Developer 后台手动创建 Distribution Profile 并下载。

---

### Q7: `The executable was signed with invalid entitlements`

**现象**：安装到真机时报 entitlements 无效。

**原因**：Capability 与 Provisioning Profile 不匹配（如 App Group、Push Notifications 在 Profile 中未开启）。

**解决方案**：
1. 检查 `project.yml` 中的 capabilities 声明。
2. 在 Apple Developer 后台的 App ID 中开启对应 Capability。
3. 重新生成 Provisioning Profile。

---

## SwiftUI 问题

### Q8: `[SwiftUI] Publishing changes from background threads is not allowed`

**现象**：运行时控制台输出此警告，且 UI 不刷新。

**原因**：在后台线程中修改了 `@Published` 属性的值。

**解决方案**：
- 确保 ViewModel 标注为 `@MainActor`，或将属性更新包装在 `await MainActor.run { }` 中：
  ```swift
  await MainActor.run {
      self.dramas = newData
  }
  ```
- 不要在 `Task.detached` 或 `DispatchQueue.global()` 的闭包中直接修改 UI 状态。

---

### Q9: `NavigationLink` 点击后页面立即弹出

**现象**：使用 `NavigationLink(destination:label:)` 时，点击 label 页面闪现后立即返回。

**原因**：通常是 NavigationLink 放在 `List` 或 `LazyVStack` 中，View 被重新创建时 NavigationLink 的 `isActive` 绑定失效。

**解决方案**：改用 `NavigationStack` + `navigationDestination` 的编程式导航：
```swift
NavigationStack(path: $path) {
    List(items) { item in
        Button(item.title) { path.append(item) }
    }
    .navigationDestination(for: Item.self) { item in
        DetailView(item: item)
    }
}
```

---

### Q10: `@StateObject` 的对象在 View 重建时被重新创建

**现象**：`@StateObject` 标注的 ViewModel 在 View 的 `init` 被多次调用时也重建了。

**原因**：父 View 的 `body` 重建导致了子 View 的 `init` 被调用，但 `@StateObject` 应该只在首次创建。实际原因常是父 View 修改了子 View 的 identity（如 `id()` modifier 变化）。

**解决方案**：
1. 检查是否给子 View 传了会变化的 `id()`。
2. 确保 `ForEach` 中的数据遵循 `Identifiable` 且 id 稳定。
3. 考虑升级到 iOS 17+ `@Observable` + `@State`，语义更清晰。

---

### Q11: `LazyVStack` 中图片闪烁/重新加载

**现象**：`LazyVStack` / `List` 中滚动时图片不断闪烁或重新显示占位图。

**原因**：`KFImage` 没有设置 `downsampling` 或 cell 复用时图片未缓存。

**解决方案**：
```swift
KFImage(url)
    .downsampling(size: targetSize)
    .cacheOriginalImage()  // 缓存原图
    .diskCacheExpiration(.days(7))
    .resizable()
```
同时确保 cell 的 `id` 稳定，避免 SwiftUI 将 cell 视为"新"View 而重新加载。

---

## Xcode 问题

### Q12: Xcode 索引卡住（Indexing forever）

**现象**：Xcode 状态栏一直显示 "Indexing..."，CPU 满负荷。

**原因**：大型工程中的索引死循环或缓存损坏。

**解决方案**：
```bash
# 清除派生数据
rm -rf ~/Library/Developer/Xcode/DerivedData/*

# 清除模块缓存
rm -rf ~/Library/Developer/Xcode/DerivedData/ModuleCache.noindex
```
重启 Xcode 后等待索引完成。如果持续卡住，禁用 Swift 源代码的 "Enable Index-While-Building"（不推荐，仅临时使用）。

---

### Q13: Previews 崩溃或一直 loading

**现象**：Canvas 中显示 "Preview Crashed" 或一直转圈。

**原因**：Preview 的依赖注入未完成，或 Preview 代码中访问了不可用的服务（如 Keychain、Core Data）。

**解决方案**：
```swift
#Preview {
    // 提供完整的 Mock 数据，不依赖真实网络/数据库
    HomePage(viewModel: HomeViewModel.preview)
}
```
在 ViewModel 中添加 `.preview` 静态工厂：
```swift
extension HomeViewModel {
    static var preview: HomeViewModel {
        HomeViewModel(fetchUseCase: MockFetchRecommendationsUseCase())
    }
}
```
以及确保 Keychain、CoreData 等单例在 Preview 环境下有 Mock 实现或安全回退。

---

### Q14: Simulator 黑屏无法操作

**现象**：模拟器启动后显示黑屏，无法进入桌面。

**原因**：模拟器数据损坏。

**解决方案**：
```bash
# 擦除模拟器数据
xcrun simctl erase "iPhone 15 Pro"

# 或删除并重建
xcrun simctl shutdown "iPhone 15 Pro"
xcrun simctl delete "iPhone 15 Pro"
xcrun simctl create "iPhone 15 Pro" "iPhone 15 Pro" "com.apple.CoreSimulator.SimRuntime.iOS-17-4"
```

---

## 性能问题

### Q15: 首页列表滚动掉帧

**现象**：首页短剧列表快速滑动时有明显卡顿。

**原因**（常见排序）：
1. 图片未 downsampling，原图尺寸远超 cell 尺寸。
2. `body` 中重复创建复杂对象。
3. Core Data fetch 在主线程。
4. SwiftUI diff 开销大（未使用 `Equatable`）。

**解决方案**：
1. `KFImage.downsampling(size:)` 设置目标尺寸。
2. 将 `body` 中的 `Array.map` / `filter` 结果在 ViewModel 中预计算。
3. 所有 Core Data 查询在 `viewContext.perform { }` 或私有 context 中执行。
4. 对列表 cell 使用 `.equatable()` modifier：
   ```swift
   ForEach(viewModel.dramas) { drama in
       DramaCard(drama: drama).equatable()
   }
   ```

---

### Q16: 内存持续增长导致 OOM

**现象**：使用 App 一段时间后，内存占用从 80MB 涨到 300MB+ 最终闪退。

**原因**（常见）：
1. 图片未释放——Kingfisher 缓存未设上限。
2. ViewModel 循环引用——闭包强引用 `self`。
3. 未取消的网络请求/Task 持有 ViewModel 不放。

**解决方案**：
1. 在 Xcode Memory Graph 中定位泄漏对象。
2. 检查 ViewModel 中 Task 的生命周期——在 Model 销毁时取消 Task：
   ```swift
   .onDisappear {
       task?.cancel()
   }
   ```
3. Kingfisher 设置内存上限（见 foundation.md）。
4. 进入后台时清理内存缓存：`ImageCache.default.clearMemoryCache()`。

---

### Q17: App 启动慢（> 3 秒）

**现象**：冷启动到首屏显示超过 3 秒。

**原因**：
1. `didFinishLaunchingWithOptions` 中做了过多同步操作。
2. 三方 SDK（Firebase、统计）阻塞主线程初始化。
3. 首页 ViewModel 的 `init` 中触发网络请求等待。

**解决方案**：
1. 将 SDK 初始化移到后台队列：`DispatchQueue.global().async { }`。
2. 首页 ViewModel 的 `init` 不发起网络请求——改为 `.onAppear` 或 `.task { }` 中调用。
3. 首屏先展示骨架屏（Skeleton），数据到达后填充。
4. 使用 Instruments → App Launch 模板分析具体耗时在哪个阶段。
