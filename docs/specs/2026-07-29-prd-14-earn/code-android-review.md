# PRD-14 赚钱中心 Android 代码评审

## 实现范围

本次实现严格限制在 `android/` 目录，并新增赚钱中心 Android 容器与导航闭环，覆盖以下范围：

- `AppConfig` / `BuildConfigAppConfig` 增加 `earnBaseUrl` 配置读取，业务代码不直接读取 `BuildConfig`
- `AppDestination` / `NavGraph` 增加赚钱中心专属路由、登录 handoff、任务播放器 handoff 与结果回传
- 新增 `feature/earn/` 模块，包含：
  - earn bridge 消息模型
  - host message 模型
  - 登录上下文 / 任务上下文 / 任务结果模型
  - `EarnViewModel`
  - `EarnScreen`
  - `EarnWebViewContainer`
  - `EarnLoginScreen`
- `EarnWebViewContainer` 注入宿主消息时，仅通过 `CustomEvent('earn.hostMessage')` 分发
- `PlayerScreen` 增加可选回调，支持赚钱任务完成回传
- 新增/补充单元测试，覆盖 earn ViewModel、路由与导航常量

## 验证结果

按要求先做定向验证，再做全量验证。实际可执行命令如下：

1. 定向赚钱 ViewModel 测试
   - 命令：`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android/gradlew -p /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android :app:testDebugUnitTest --tests "com.djs66256.short_drama.feature.earn.viewmodel.EarnViewModelTest"`
   - 结果：通过

2. 定向导航测试
   - 命令：`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android/gradlew -p /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android :app:testDebugUnitTest --tests "com.djs66256.short_drama.navigation.RoutesTest" --tests "com.djs66256.short_drama.navigation.NavGraphTest"`
   - 结果：通过

3. 全量单元测试
   - 命令：`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android/gradlew -p /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android test`
   - 结果：通过

4. Debug 构建
   - 命令：`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android/gradlew -p /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android assembleDebug`
   - 结果：通过

5. 静态分析
   - 命令：`/Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android/gradlew -p /Users/bytedance/Documents/github/fork-any-application/.claude/worktrees/2026-07-29-prd-14-earn/android detekt`
   - 结果：通过

补充说明：环境中需显式设置 Android Studio 自带 JBR 为 `JAVA_HOME` 后再执行上述命令。

## 发现并修复的问题

1. Gradle 命令兼容性问题
   - 现象：`./gradlew test --tests ...` 在当前工程下未按预期工作
   - 处理：切换为 `:app:testDebugUnitTest --tests ...` 完成定向验证

2. Java 运行时缺失
   - 现象：命令行环境默认无法定位 Java Runtime
   - 处理：改用 Android Studio 自带 JBR 作为 `JAVA_HOME`

3. `BuildConfigAppConfig` 临时类型回归
   - 现象：`appName` 返回类型曾被误改
   - 处理：已恢复为 `String`

4. `NavGraph` 中 `SavedStateHandle` 类型推断问题
   - 现象：任务结果去重逻辑编译失败
   - 处理：显式使用 `get<Boolean>(key)`

5. `PlayerScreen` detekt `LongParameterList`
   - 现象：`PlayerContent(...)` 参数过多导致静态分析失败
   - 处理：收敛为 `PlayerContentCallbacks` 回调对象，并完成内部引用替换

6. `NavGraph` 无用导入
   - 现象：存在未使用的 `Button` 导入
   - 处理：已删除

## 遗留风险

- `PlayerScreen` 当前为赚钱任务回传提供了一个显式“模拟任务完成”入口，用于打通宿主与任务完成闭环；如果后续播放器接入真实完播事件，应将该回调切换为真实播放完成信号。
- 编译期间存在 `LocalLifecycleOwner` 的弃用 warning，但不影响本次功能与验证结果；后续可在统一升级 `lifecycle-runtime-compose` 接入时一并处理。

## 结论

本次 Android 侧实现已覆盖 PRD-14 plan 中要求的 earn 容器、配置、导航、bridge、host message 注入、login handoff、task player handoff、`EarnTaskPlayerResult` 收口与测试。

关键约束均已满足：

- 业务代码通过 `AppConfig` 读取 earn 配置
- earn host sync 仅通过 `CustomEvent('earn.hostMessage')`
- 仅在 `EarnTaskPlayerResult(completed=true)` 时触发 `earn.completeTask`
- handoff route 稳定维护 `taskId/source/returnTarget/videoId`
- 未新增依赖，未扩需求

结合定向测试、全量测试、构建与 detekt 结果，可以将 `coding-platforms.android` 标记为 `completed`。
