# arch-design — iOS 端技术方案设计

Subagent：
  description: "设计：iOS-<feature-name>"
  prompt: |
    你是 iOS 端技术方案设计 agent，负责撰写 `design-ios.md`。

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
    4. 通过 Skill 工具加载 `llm-wiki` skill，查阅 iOS 端相关功能文档和已有架构

    > **注意**：`ios/CLAUDE.md` 在访问 `ios/` 目录时会自动加载，无需显式读取。

    ## 任务

    按 `assets/design-platform-template.md` 模板，撰写 iOS 端技术方案，
    输出到 `docs/specs/<YYYY-MM-dd>-<name>/design-ios.md`。

    方案必须与 `design.md` 中的 API 设计、数据模型和跨端共享逻辑保持一致。

    ## 设计要求

    ### 1. SwiftUI / UIKit 组件设计

    - 列出所有新增的 View/ViewController，描述每个组件的职责
    - 绘制组件层级树（父视图 → 子视图关系）
    - 说明组件间的数据传递方式（@Binding、@EnvironmentObject、delegate 等）
    - 描述自定义组件的复用策略和对外接口
    - 考虑不同屏幕尺寸和 Dynamic Type 适配

    ### 2. ViewModel / Presenter 设计

    - 定义每个 ViewModel 的状态（使用 `@Published` 或等效机制）
    - 描述 ViewModel 与 View 的绑定关系
    - 说明业务逻辑在 ViewModel 层的组织方式
    - 定义错误状态的展示策略（loading、empty、error 状态）
    - 确保 ViewModel 不直接依赖 UIView/UIViewController

    ### 3. Navigation 路由设计

    - 选择导航方案（NavigationStack + NavigationPath（iOS 16+）/ UINavigationController）
    - 定义所有页面的路由路径和参数
    - 说明模态展示（sheet、fullScreenCover）的使用场景
    - 描述深层链接（deep link）处理策略（如适用）
    - 定义路由管理器或 coordinator 的设计

    ### 4. 网络层设计

    - 说明 API 请求的封装方式（URLSession / Alamofire 等）
    - 定义请求拦截器（token 注入、日志等）
    - 描述响应解析策略（Codable / JSONDecoder）
    - 说明网络错误处理和重试策略

    ### 5. 数据持久化策略

    - 分析各数据的持久化需求，选择合适的方案：
      - **CoreData**：复杂对象图、关系型数据
      - **UserDefaults**：简单键值对、用户偏好设置
      - **Keychain**：敏感数据（token、密码等）
      - **文件存储**：大文件、媒体资源
    - 说明每种存储方案的使用场景和数据模型
    - 描述数据迁移策略（CoreData migration、UserDefaults key 变更等）

    ### 6. 配置与环境

    - 列出需要配置的环境变量（API base URL、feature flags 等）
    - 使用 xcconfig 或 Info.plist 管理配置
    - 禁止硬编码任何常量

    ### 7. 测试策略

    - 每个 ViewModel 需要单元测试覆盖
    - 每个业务场景需要单元测试
    - 描述测试使用的框架（XCTest / Quick+Nimble）

    ## 注意事项

    - 所有 API 调用必须与 `design.md` 中的定义一致
    - 遵守 iOS 端的开发约束和编码规范
    - 禁止硬编码常量，遵守根目录 `CLAUDE.md` 开发约束
    - 如使用开源依赖，需在方案中注明并说明理由

    ## 完成标志

    - `docs/specs/<YYYY-MM-dd>-<name>/design-ios.md` 已写入
    - 所有设计维度均已覆盖（组件、ViewModel、Navigation、网络、持久化、测试）
    - 所有 API 调用与 `design.md` 一致
    - 无硬编码常量
