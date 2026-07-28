---
name: admin-e2e-test
description: >
  Admin 管理平台 E2E 测试 skill，使用 Chrome DevTools MCP 对 admin web 进行黑盒测试。
  覆盖登录、仪表盘、短剧/剧集 CRUD、用户管理、dark/light 模式。
  触发场景：用户提到"测试 admin"、"admin e2e"、"管理平台测试"、"test admin panel"、
  "test-fix-loop"、"Chrome 测试管理后台"。
  派发 Chrome DevTools 子 agent 执行测试，产出结构化 bug 报告。
---

# Admin E2E Test

## 定位

admin-e2e-test 是一个**自动化浏览器测试 skill**。通过 Chrome DevTools MCP 操作真实浏览器，对 admin web 面板进行全面的功能、UI 和色彩可读性测试。

## 前置条件

- Backend 服务在 `http://localhost:3001` 运行
- Web 前端在 `http://localhost:3000` 运行
- Chrome DevTools MCP 可用
- Supabase 本地环境运行（提供 auth + 数据）

## 测试范围

| 页面 | 路由 | 测试内容 |
|------|------|---------|
| 登录页 | `/admin/login` | 内容渲染、表单输入、字段验证、登录按钮状态、错误提示 |
| 仪表盘 | `/admin` | 统计卡片、加载态、错误态 + 重试、数据正确性 |
| 短剧列表 | `/admin/dramas` | 表格数据、分页、空态、操作按钮、封面显示 |
| 新建短剧 | `/admin/dramas/new` | 表单字段、验证、提交、取消 |
| 编辑短剧 | `/admin/dramas/[id]/edit` | 预填数据、表单验证、保存修改 |
| 剧集列表 | `/admin/dramas/[id]/episodes` | 表格数据、返回链接、新建按钮 |
| 新建剧集 | `/admin/dramas/[id]/episodes/new` | 表单字段、验证、提交 |
| 编辑剧集 | `/admin/dramas/[id]/episodes/[eid]/edit` | 预填数据、表单验证 |
| 用户管理 | `/admin/users` | 权限控制（非 admin 不可见）、角色标签、角色修改 |
| Dark 模式 | 所有页面 | 色彩对比度、可读性、token 合规（无硬编码白色背景） |
| Light 模式 | 所有页面 | 同上 |

## 能力线

| 能力线 | 职责 | 执行者 |
|--------|------|--------|
| **测试执行** | 通过 Chrome DevTools MCP 操作浏览器、截图、检查页面状态和颜色 | Subagent (Chrome DevTools) |
| **Bug 报告** | 收集测试结果，按严重程度输出结构化报告 | Subagent |

## 测试 subagent

以下 subagent 定义由主 agent 在需要执行 admin 测试时派发。

Subagent：
  description: "Admin E2E 测试：Chrome DevTools 浏览器测试"
  prompt: |
    你是 Admin 管理平台的 E2E 测试 agent。使用 Chrome DevTools MCP 工具测试管理后台的各项功能和 UI。

    ## 测试环境

    - Web 前端：`http://localhost:3000`
    - Backend API：`http://localhost:3001`
    - Admin 登录：`http://localhost:3000/admin/login`
    - 测试账号：由主 agent 通过 prompt 参数传入（email 和 password）

    ## 测试前检查

    1. 调用 `list_pages` 查看已有页面
    2. 如果没有合适的页面，用 `new_page` 打开 `http://localhost:3000/admin/login`

    ## 配色规范（重要！）

    Admin 使用扁平 UI 设计，通过 `web/src/styles/tokens.css` 中的 CSS 自定义属性控制颜色。

    **Light 模式下的正确 token 映射：**
    - `--color-background: #ffffff` — 卡片/输入框背景
    - `--color-surface: #f9fafb` — 页面背景
    - `--color-primary: #6366f1` — 主色（indigo）
    - `--color-primary-hover: #4f46e5` — 主色悬浮
    - `--color-text-primary: #111827` — 主文字
    - `--color-text-secondary: #6b7280` — 次文字
    - `--color-border: #e5e7eb` — 边框
    - `--color-error: #ef4444` — 错误

    **Dark 模式下的正确 token 映射：**
    - `--color-background: #0f172a` — 深色背景
    - `--color-surface: #1e293b` — 表面色
    - `--color-primary: #818cf8` — 主色（浅 indigo）
    - `--color-text-primary: #f1f5f9` — 主文字（浅色）
    - `--color-text-secondary: #94a3b8` — 次文字
    - `--color-border: #334155` — 边框
    - `--color-error: #f87171` — 错误

    **硬编码颜色检测（bug 信号）：**
    如果在元素样式中看到以下颜色值（而非 CSS 变量），说明没有适配 dark mode：
    - `#ffffff` — 硬编码白色（dark 模式下不可读）
    - `#2563EB` — 硬编码蓝色（不在 tokens 定义中）
    - `#f3f4f6` — 硬编码灰色
    - `#e5e7eb` — 硬编码边框灰
    - `#fef2f2` — 硬编码错误背景
    - `#f8fafc` — 硬编码浅灰背景

    ## 测试流程

    按以下顺序执行测试，每个测试项执行后立即记录结果。

    ### 1. Light Mode — 登录页

    1. 确保在 light 模式（`emulate` colorScheme: "light"）
    2. 打开 `/admin/login`
    3. `take_snapshot` 检查页面内容：
       - 标题「管理平台」可见
       - 副标题「ShortDrama Admin Panel」可见
       - 邮箱输入框可见
       - 密码输入框可见
       - 登录按钮可见
    4. 测试表单验证：不填任何内容，直接点登录按钮
       - 应显示「请输入邮箱」「请输入密码」错误提示
       - 快照截图确认
    5. 清除错误：在邮箱输入框填入 `notanemail`，密码框填入 `xxx`
       - 应显示邮箱格式验证提示
    6. 登录表单 UI 检查：`evaluate_script` 检查登录卡片背景色
       - 在 light 模式下不应为纯白 `#ffffff`（应使用 CSS 变量）
    7. 截图保存登录页面

    ### 2. Light Mode — 登录成功

    1. 填入正确的测试账号邮箱和密码
    2. 点击登录按钮
    3. 验证跳转到 `/admin`（仪表盘），快照确认

    ### 3. Light Mode — 仪表盘

    1. 检查页面标题「仪表盘」
    2. 检查 3 个统计卡片：「总短剧数」「总剧集数」「用户数」
    3. `evaluate_script` 检查统计卡片数值是否展示（非「加载中...」）
    4. 检查 Header 显示用户邮箱和角色标签
    5. 检查侧边栏导航项：「仪表盘」「短剧管理」「用户管理」（admin 角色应有三项）
    6. 截图保存仪表盘

    ### 4. Light Mode — 短剧列表

    1. 点击侧边栏「短剧管理」或导航到 `/admin/dramas`
    2. 检查页面标题「短剧管理」
    3. 检查「新建短剧」按钮可见（admin/editor 角色）
    4. 如果有短剧数据，检查表格列：封面、标题、分类、集数、评分、操作
    5. 如果有数据，检查操作列：「剧集」「编辑」「删除」
    6. 截图保存短剧列表

    ### 5. Light Mode — 新建短剧

    1. 点击「新建短剧」按钮或导航到 `/admin/dramas/new`
    2. 检查表单字段：标题(*)、描述、封面 URL、分类、集数、标签、评分
    3. 测试验证：不填标题直接点「新建短剧」
       - 应显示「请输入标题」
    4. 测试评分验证：填入评分 15（超出 0-10 范围）
       - 应显示「评分范围为 0-10」
    5. 填入测试数据：
       - 标题: `E2E 测试短剧 <timestamp>`
       - 描述: `这是 E2E 测试创建的短剧描述`
       - 分类: `测试`
       - 集数: `24`
       - 标签: `e2e, test, automation`
       - 评分: `8.5`
    6. 点击「新建短剧」提交
    7. 验证跳转到短剧列表
    8. 验证新建的短剧出现在列表中
    9. 截图保存

    ### 6. Light Mode — 编辑短剧

    1. 找到刚创建的短剧，点击「编辑」
    2. 检查表单预填了正确的数据
    3. 修改标题（加 "编辑过" 后缀）
    4. 点击「保存修改」
    5. 验证列表中的标题已更新
    6. 截图保存

    ### 7. Light Mode — 剧集管理

    1. 在短剧列表找到测试短剧，点击「剧集」
    2. 检查页面标题包含短剧名 + 「剧集管理」
    3. 检查「返回短剧列表」链接
    4. 如果无剧集，显示「暂无剧集」空态
    5. 点击「新建剧集」
    6. 检查表单字段：标题(*)、剧集号(*)、时长、视频 URL、缩略图、描述
    7. 测试验证：不填必填字段直接提交
       - 应显示「请输入剧集标题」「请输入有效的剧集号」
    8. 填入测试数据：
       - 标题: `第1集：开始`
       - 剧集号: `1`
       - 时长: `300`
    9. 提交
    10. 验证剧集出现在列表中
    11. 截图保存

    ### 8. Light Mode — 编辑剧集

    1. 点击刚创建的剧集的「编辑」
    2. 检查表单预填正确
    3. 修改标题
    4. 保存，验证列表更新
    5. 截图保存

    ### 9. Light Mode — 用户管理

    1. 导航到 `/admin/users`
    2. Admin 角色应能看到用户列表
    3. 检查表格列：邮箱、显示名、角色、创建时间、操作
    4. 检查各用户角色标签颜色正确
    5. 验证当前用户在操作列显示「当前用户」而非下拉框
    6. 尝试修改其他用户角色（切换下拉框选项）
    7. 截图保存

    ### 10. Dark Mode — 登录页

    1. 模拟 dark 模式：`emulate` colorScheme: "dark"
    2. 访问 `/admin/login`
    3. `evaluate_script` 检查页面背景色：
       - 页面背景不应为纯白 `#ffffff` 或 `#f3f4f6`
       - 卡片背景不应为纯白
       - 文字颜色应为浅色（因为深色背景）
    4. 检查输入框是否可见（边框和文字有足够对比度）
    5. 截图保存 dark 模式登录页

    ### 11. Dark Mode — 仪表盘

    1. 在 dark 模式下登录并导航到仪表盘
    2. 检查统计卡片在 dark 模式下可读（卡片背景为深色，文字为浅色）
    3. 检查 Header 和侧边栏在 dark 模式下颜色正确
    4. `evaluate_script` 检查是否有硬编码白色背景元素：
       ```js
       () => Array.from(document.querySelectorAll('*'))
         .filter(el => {
           const bg = getComputedStyle(el).backgroundColor;
           return bg === 'rgb(255, 255, 255)';
         })
         .map(el => el.className || el.tagName)
         .slice(0, 10)
       ```
    5. 截图保存 dark 模式仪表盘

    ### 12. Dark Mode — 所有页面颜色检查

    遍历所有 admin 页面，在 dark 模式下检查：
    1. 文字可读性（与背景有足够对比度）
    2. 按钮文字可见
    3. 表格 border 可见
    4. 表单输入框边框可见
    5. 截图保存每个页面

    ### 13. 清理

    1. 删除测试创建的剧集和剧集（如可操作）
    2. 或者保留测试数据供后续测试使用

    ## Bug 报告格式

    测试完成后，按以下格式输出 bug 报告：

    ```markdown
    # Admin E2E 测试报告 — <timestamp>

    ## 摘要
    - 测试页面: <N> 个
    - 通过: <N> / 失败: <N> / 跳过: <N>
    - Light 模式状态: P0 bug <N>, P1 bug <N>, P2 bug <N>
    - Dark 模式状态: P0 bug <N>, P1 bug <N>, P2 bug <N>

    ## Bug 列表

    ### BUG-001: <简短标题>
    - **严重程度**: P0 / P1 / P2
    - **页面**: `/admin/xxx`
    - **模式**: Light / Dark
    - **描述**: <详细描述>
    - **复现步骤**: <步骤>
    - **预期**: <预期行为>
    - **实际**: <实际行为>
    - **截图**: <截图路径或描述>

    ### BUG-002: ...

    ## 通过项
    - ✅ <页面/功能> — <说明>
    ```

    ## 严重程度定义
    - **P0**: 页面白屏、不可用、dark 模式下完全不可读
    - **P1**: 功能异常、数据不显示、颜色对比度不足
    - **P2**: 样式不一致、规格不符（如阴影/动画违规）

    ## 注意事项

    1. 每次 `take_snapshot` 前先确保页面已加载完成（`wait_for` 关键文字）
    2. 表单提交后等待导航完成再继续
    3. Dark 模式检测重点：`getComputedStyle` 检查 `backgroundColor` 是否为 `rgb(255, 255, 255)`（纯白）
    4. 遇到 401 或登录失效时先重新登录
    5. 如果测试数据已存在（从上一轮测试），复用它们
    6. 控制台错误也要记录（`list_console_messages`）

## 参考

- [admin-panel-development](../admin-panel-development/SKILL.md) — 管理平台完整规范
- [flat UI 规范](../admin-panel-development/references/flat-ui-standards.md) — 扁平 UI 颜色/组件规范
- [web-development](../web-development/SKILL.md) — Web 前端工程规范
- `web/src/styles/tokens.css` — CSS 变量定义
