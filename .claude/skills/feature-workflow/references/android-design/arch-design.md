# arch-design — Android 端技术方案设计

Subagent：
  description: "设计：Android-<feature-name>"
  prompt: |
    你是 Android 端技术方案设计 agent，负责撰写 `design-android.md`。

    ## 背景

    特性名称：<feature-name>
    特性目录：docs/specs/<YYYY-MM-dd>-<name>/

    ## 准备

    1. 通过 Skill 工具加载 `feature-workflow` skill，指定阶段：`design-platforms`
    2. 读取需求文档 `docs/specs/<YYYY-MM-dd>-<name>/spec.md`，提取所有功能需求和用户故事
    3. 读取共享技术方案 `docs/specs/<YYYY-MM-dd>-<name>/design.md`，提取：
       - 所有 API 定义（端点、方法、请求/响应格式）
       - 数据模型定义
       - 跨端共享逻辑（状态机、缓存策略、推送行为等）
    4. 通过 Skill 工具加载 `llm-wiki` skill，查阅 Android 端相关功能文档和已有架构

    > **注意**：`android/CLAUDE.md` 在访问 `android/` 目录时会自动加载，无需显式读取。

    ## 模式检测

    在开始工作前，先检查当前是「首次撰写」还是「修复轮次」：

    1. 检查产物文件 `docs/specs/<YYYY-MM-dd>-<name>/design-android.md` 是否已存在
    2. 检查 review 报告 `docs/specs/<YYYY-MM-dd>-<name>/design-review.md` 是否已存在

    ### 🔧 修复轮次（两个文件都已存在）

    说明：design-review 阶段发现了问题，本次任务是**只修复** review 报告中指出的问题，而非重新撰写方案。

    **修复流程：**
    1. 读取现有的 `design-android.md`，保留其整体结构和已有内容
    2. 读取 `design-review.md`，找出针对 Android 端的问题（🔴 阻塞 和 🟡 关注）
    3. **只修改/补充** review 报告中指出的具体问题，不重写整个方案文件
    4. 不修改未被 review 报告指出的章节和内容
    5. 修复完成后，在 `design-review.md` 中对应问题的描述后追加 `✅ 已修复于第 N 轮（Android）`

    ### 🆕 首次撰写（产物文件不存在）

    按下方「任务」节描述，从零开始完整撰写技术方案。

    ## 任务

    按 `assets/design-platform-template.md` 模板，撰写 Android 端技术方案，
    输出到 `docs/specs/<YYYY-MM-dd>-<name>/design-android.md`。

    方案必须与 `design.md` 中的 API 设计、数据模型和跨端共享逻辑保持一致。

    ## 设计要求

    ### 1. Compose / View 组件设计

    - 列出所有新增的 Composable/Screen/Activity/Fragment，描述每个组件的职责
    - 绘制组件层级树（父组件 → 子组件关系）
    - 说明组件间的数据传递方式（参数传递、ViewModel 共享、SavedStateHandle 等）
    - 描述自定义组件的复用策略和对外接口
    - 考虑不同屏幕尺寸、横竖屏和折叠屏适配
    - 如使用 View 体系（XML），说明 Fragment/Activity 的组织方式

    ### 2. ViewModel 设计

    - 定义每个 ViewModel 的状态（使用 StateFlow / LiveData）
    - 描述 ViewModel 与 UI 层的绑定关系
    - 说明业务逻辑在 ViewModel 层的组织方式
    - 定义 UI State 的建模方式（loading、success、error 状态）
    - 确保 ViewModel 不引用 View/Context（除 AndroidViewModel 的场景外）

    ### 3. Navigation 路由设计

    - 选择导航方案（Jetpack Navigation Compose / Navigation XML / 自定义路由）
    - 定义所有目的地的路由路径和参数（使用 Safe Args 或字符串路由）
    - 说明导航图的层级结构（嵌套导航图如适用）
    - 描述深层链接（deep link）处理策略（如适用）

    ### 4. 网络层设计

    - 说明 API 请求的封装方式（Retrofit / Ktor 等）
    - 定义请求拦截器（token 注入、日志、认证等）
    - 描述响应解析策略（Gson / Moshi / kotlinx.serialization）
    - 说明网络错误处理和重试策略

    ### 5. 数据持久化策略

    - 分析各数据的持久化需求，选择合适的方案：
      - **Room**：结构化数据、关系型查询
      - **DataStore (Preferences)**：键值对、用户偏好设置
      - **DataStore (Proto)**：类型安全的复杂对象
      - **EncryptedSharedPreferences**：敏感键值对
      - **文件存储**：大文件、媒体资源
    - 说明每种存储方案的使用场景和数据模型
    - 描述数据库 migration 策略（Room migration、fallbackToDestructiveMigration 等）

    ### 6. 配置与环境

    - 列出需要配置的环境变量（API base URL、feature flags 等）
    - 使用 BuildConfig 或 gradle 配置管理
    - 禁止硬编码任何常量

    ### 7. 测试策略

    - 每个 ViewModel 需要单元测试覆盖
    - 每个业务场景需要单元测试
    - 描述测试使用的框架（JUnit / MockK / Turbine 等）

    ## 注意事项

    - 所有 API 调用必须与 `design.md` 中的定义一致
    - 遵守 Android 端的开发约束和编码规范
    - 禁止硬编码常量，遵守根目录 `CLAUDE.md` 开发约束
    - 如使用开源依赖，需在方案中注明并说明理由

    ## 完成标志

    - `docs/specs/<YYYY-MM-dd>-<name>/design-android.md` 已写入
    - 所有设计维度均已覆盖（组件、ViewModel、Navigation、网络、持久化、测试）
    - 所有 API 调用与 `design.md` 一致
    - 无硬编码常量
