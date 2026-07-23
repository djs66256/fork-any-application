# Android 端技术方案：{{功能名称}}

> 创建日期：{{YYYY-MM-DD}}
> 对应共享方案：design.md
> 对应需求：spec.md

---

## 1. 架构设计

<!-- 描述 Android 端架构层级（UI → ViewModel → Repository → DataSource） -->

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose / View)              │
│  ├── Screens / Activities / Fragments           │
│  └── Reusable Composables / Views               │
├─────────────────────────────────────────────────┤
│  ViewModel Layer (StateFlow / LiveData)         │
│  ├── UI State Management                        │
│  └── Business Logic                             │
├─────────────────────────────────────────────────┤
│  Repository Layer                               │
│  ├── API Repository (Retrofit / Ktor)           │
│  ├── Cache Repository                           │
│  └── Data Repository (Room / DataStore)         │
└─────────────────────────────────────────────────┘
```

### 1.1 与现有架构的关系

| 现有模块 | 变更类型 | 说明 |
|---------|---------|------|
| | 扩展 / 新增 / 不变 | |

---

## 2. 核心文件变更

| 文件路径 | 操作 | 变更说明 |
|---------|------|---------|
| `android/.../xxx.kt` | 新增 | |
| `android/.../xxx.kt` | 修改 | |
| `android/.../xxx.kt` | 删除 | |

---

## 3. UI 层设计

### 3.1 组件层级树

```
{{Screen}} (Composable / Activity)
├── {{TopBar}}
├── {{ContentSection}}
│   ├── {{ListItem}} (LazyColumn items)
│   │   ├── {{Thumbnail}}
│   │   └── {{InfoText}}
│   └── {{EmptyState}}
└── {{BottomBar}} / {{FAB}}
```

### 3.2 组件清单

| 组件名称 | 类型 | 职责 | 是否复用 |
|---------|------|------|---------|
| | Composable / Fragment / Activity | | 是 / 否 |

### 3.3 Composable 接口定义

```kotlin
@Composable
fun {{ComponentName}}(
    param: Type,                    // 外部传入
    modifier: Modifier = Modifier,  // 修饰符
    onAction: () -> Unit = {},      // 回调
) {
    // ...
}
```

### 3.4 数据传递方式

| 传递方向 | 方式 | 适用场景 |
|---------|------|---------|
| 父 → 子 | Composable 参数 / Fragment Arguments | |
| 子 → 父 | Lambda Callback | |
| 跨 Composable 共享 | ViewModel 共享 / CompositionLocal | |
| Fragment → Fragment | Shared ViewModel / Navigation Args | |

### 3.5 屏幕适配

| 适配维度 | 策略 | 说明 |
|---------|------|------|
| 屏幕尺寸 | `BoxWithConstraints` / `WindowSizeClass` | |
| 横竖屏 | `rememberSaveable` + 配置变更处理 | |
| 折叠屏 | `WindowSizeClass` / `FoldingFeature` | |
| 字体缩放 | `fontScale` / `TextUnit` | |
| 深色模式 | `isSystemInDarkTheme()` / MaterialTheme | |

---

## 4. ViewModel 设计

### 4.1 ViewModel 清单

| ViewModel | 关联 UI | 职责 |
|-----------|---------|------|
| | | |

### 4.2 状态定义

```kotlin
class {{ViewModelName}}(
    private val repository: {{Repository}},
) : ViewModel() {

    // UI State
    data class UiState(
        val items: List<Item> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Actions
    fun loadData() {
        viewModelScope.launch { ... }
    }
}
```

### 4.3 状态字段详情

| 状态字段 | 类型 | 初始值 | 说明 |
|---------|------|--------|------|
| | | | |

### 4.4 UI 状态建模

| UI 状态 | 判别条件 | UI 层表现 |
|---------|---------|----------|
| Loading | `isLoading == true` | 骨架屏 / CircularProgressIndicator |
| Success (有数据) | `items.isNotEmpty() && errorMessage == null` | 正常列表/内容 |
| Empty (空数据) | `items.isEmpty() && !isLoading` | 空态插图 + 引导文案 |
| Error (可重试) | `errorMessage != null && isRetryable` | 错误提示 + 重试按钮 |
| Error (不可重试) | `errorMessage != null && !isRetryable` | 错误提示 + 返回引导 |

---

## 5. Navigation 路由设计

### 5.1 导航方案

<!-- Jetpack Navigation Compose / Navigation XML / 自定义路由 -->

### 5.2 路由清单

| 路由标识 | 目标 Composable/Activity | 参数 | 导航方式 | 说明 |
|---------|------------------------|------|---------|------|
| | | | NavController.navigate / startActivity | |

### 5.3 导航图

```kotlin
// NavGraph 定义
NavHost(navController, startDestination = "{{start}}") {
    composable("{{route}}/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
        {{Screen}}(navController)
    }
}
```

### 5.4 Deep Link 处理（如适用）

| Deep Link Pattern | 解析目标 | 参数提取 |
|------------------|---------|---------|
| `app://xxx/{id}` | {{Screen}} | id |

---

## 6. 网络层设计

### 6.1 网络栈分层

| 层级 | 实现 | 说明 |
|------|------|------|
| HTTP 客户端 | Retrofit / Ktor | |
| 数据模型 | `@Serializable` data class / Gson / Moshi | |
| 请求拦截器 | `Interceptor`：Token 注入、日志、设备信息 | |
| 响应解析 | kotlinx.serialization / GsonConverter | |
| 错误处理 | 统一 `ApiResult<T>` / `sealed class` 封装 | |

### 6.2 API 接口定义

```kotlin
interface {{ApiService}} {
    @GET("api/path")
    suspend fun getItems(): Response<ItemsResponse>

    @POST("api/path")
    suspend fun createItem(@Body request: CreateRequest): Response<CreateResponse>
}
```

### 6.3 请求重试策略

| 场景 | 重试次数 | 退避策略 | 说明 |
|------|---------|---------|------|
| 网络超时 | 2 | 指数退避 | |
| 5xx 服务端错误 | 3 | 指数退避 | |
| 401 Token 过期 | — | — | Authenticator 刷新 token 后重放 |

### 6.4 网络状态监听

<!-- ConnectivityManager / NetworkCallback，处理网络变化 -->

---

## 7. 数据持久化策略

### 7.1 存储方案选择

| 数据类型 | 存储方案 | 容器/Key | 过期策略 | 说明 |
|---------|---------|----------|---------|------|
| | Room | | | |
| | DataStore (Preferences) | | | |
| | DataStore (Proto) | | | |
| | EncryptedSharedPreferences | | | |
| | Internal Storage | | | |

### 7.2 Room 实体设计（如适用）

```kotlin
@Entity(tableName = "{{table_name}}")
data class {{EntityName}}(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
)

@Dao
interface {{EntityName}}Dao {
    @Query("SELECT * FROM {{table_name}}")
    fun getAll(): Flow<List<{{EntityName}}>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<{{EntityName}}>)
}
```

### 7.3 缓存策略

| 缓存内容 | 策略 | TTL | 淘汰策略 |
|---------|------|-----|---------|
| | 内存缓存（LruCache） | | |
| | 磁盘缓存 | | |

### 7.4 数据库 Migration

<!-- Room migration 策略、版本编号、fallbackToDestructiveMigration 的风险说明 -->

---

## 8. 配置与环境

| 配置项 | 管理方式 | 开发环境值 | 生产环境值 | 说明 |
|--------|---------|----------|-----------|------|
| API Base URL | BuildConfig / gradle | | | |
| Feature Flag | Remote Config / Firestore | | | |
| API Key | BuildConfig / EncryptedSharedPreferences | | | |

> ⚠️ 禁止硬编码任何常量。使用 BuildConfig / gradle 属性管理配置。

---

## 9. API 调用清单

<!-- 列出本端需要调用的所有 API，与 design.md 保持严格一致 -->

| API 端点 | 调用时机 | 请求数据来源 | 成功后操作 | 错误处理 |
|---------|---------|-------------|-----------|---------|
| `METHOD /api/path` | 页面加载 / 用户操作 | ViewModel 状态 | 更新 StateFlow / 导航 | Toast / 内联 / Snackbar |

---

## 10. 跨端共享逻辑落地

<!-- 对应 design.md 中「跨端共享逻辑」章节 -->

| 共享逻辑 | design.md 定义 | Android 端实现方式 |
|---------|---------------|-------------------|
| | | |

---

## 11. 边界与错误处理

### 11.1 全局错误拦截

| 层级 | 机制 | 说明 |
|------|------|------|
| 网络层 | OkHttp Interceptor → `ApiResult` sealed class | |
| ViewModel | `try-catch` / `catch` operator (Flow) | |
| UI 层 | `Snackbar` / `Toast` / AlertDialog / 内联错误 | |
| 日志 | Timber / Firebase Crashlytics | |

### 11.2 错误码映射表

<!-- 与 design.md 中错误码对应，补充 Android 端交互方式 -->

| 后端错误码 | 用户提示文案 | 交互方式 |
|-----------|------------|---------|
| `INVALID_PARAMS` | | 内联 TextInputLayout error |
| `UNAUTHORIZED` | | Snackbar + 跳转登录 |
| `FORBIDDEN` | | Toast / 返回上一页 |
| `NOT_FOUND` | | Toast / 空态页 |
| `CONFLICT` | | Snackbar + Action("重试") |
| `RATE_LIMITED` | | Snackbar（含倒计时） |
| `INTERNAL_ERROR` | | Snackbar + Action("重试") |
| `NETWORK_ERROR` | | Snackbar + Action("重试") |

### 11.3 端侧特有边界场景

| 场景 | 触发条件 | 处理策略 | 优先级 |
|------|---------|---------|--------|
| 网络切换（Wi-Fi ↔ 蜂窝） | NetworkCallback 回调 | 中断的请求自动重试 / 暂停大文件下载 | 🟡 |
| App 进入后台 | `onStop()` / Lifecycle | 暂停播放/任务、保存状态 | 🟡 |
| App 返回前台 | `onStart()` / Lifecycle | 恢复任务、刷新过期数据 | 🟡 |
| Activity/Fragment 销毁时未完成请求 | ViewModel `onCleared()` / Lifecycle | `viewModelScope` 自动取消协程 | 🔴 |
| 配置变更（旋转屏幕） | `onConfigurationChanged` | ViewModel 保留 + `rememberSaveable` 恢复 | 🟡 |
| 进程被杀死恢复 | `SavedStateHandle` | 从 `SavedStateHandle` 恢复关键状态 | 🟡 |
| 本地缓存过期/损坏 | 读取时解析异常 | 清除损坏缓存，降级到网络请求 | 🟡 |
| 用户快速连续操作 | 快速点击 / 重复提交 | 防抖（300ms）/ 按钮 disabled | 🔴 |
| 内存不足 | `onTrimMemory()` | 释放 LruCache / 图片缓存 | 🟢 |
| 首次安装 | DataStore 无缓存标记 | 展示骨架屏 / Onboarding | 🟡 |

### 11.4 UI 态覆盖矩阵

| 页面/组件 | Loading | Success | Empty | Error（可重试） | Error（不可重试） |
|-----------|---------|---------|-------|----------------|------------------|
| | | | | | |

---

## 12. 测试策略

### 12.1 测试范围

| 测试类型 | 覆盖内容 | 目标覆盖率 | 框架 |
|---------|---------|-----------|------|
| 单元测试 | ViewModel 逻辑、Repository | | JUnit + MockK |
| UI 测试 | Composable 关键交互 | | Compose Testing |
| 端到端测试 | 关键用户流程 | | Espresso / UIAutomator |

### 12.2 关键测试场景

| 编号 | 测试场景 | Given | When | Then | 测试类型 |
|------|---------|-------|------|------|---------|
| | ViewModel 加载成功 | 正常网络 | 调用 loadData() | items 非空，isLoading = false | 单元 |
| | ViewModel 加载失败 | 网络异常 | 调用 loadData() | errorMessage 非空 | 单元 |
| | 空列表展示 | 返回空数据 | 页面加载完成 | 展示 empty 视图 | 单元 |

### 12.3 Mock 策略

| 依赖 | Mock 方式 | 说明 |
|------|----------|------|
| API 请求 | MockK / MockWebServer | |
| Room | In-Memory Database | |
| DataStore | TestDataStore | |

---

## 13. 新增依赖

| 依赖名称 | 版本 | 用途 | 选型理由 |
|---------|------|------|---------|
| | | | |

> ⚠️ 新增开源依赖前必须征得用户同意（遵守根目录 CLAUDE.md 开发约束）。

---

## 14. 风险与对策

| 风险 | 影响范围 | 严重程度 | 发生概率 | 对策 | 回退方案 |
|------|---------|---------|---------|------|---------|
| | | 🔴/🟡/🟢 | 高/中/低 | | |

---

## 15. 参考资料

### 已查阅的 wiki 文档

| 文档 | 相关章节 | 关键信息 |
|------|---------|---------|
| | | |

### 已查阅的代码文件

| 文件 | 关键内容 |
|------|---------|
| | |
