# Admin Panel 开发流程

## 阶段总览

管理平台开发流程比 feature-workflow 更精简（仅 2 端口），共 5 个阶段：

```
[阶段 1: RBAC 设计] → [阶段 2: Admin API 开发] → [阶段 3: Admin UI 开发] → [阶段 4: 权限联调] → [阶段 5: 测试与交付]
```

## 阶段 1：RBAC 权限设计

### 输入

- PRD（来自 `docs/product_manager/prd/2026-07-27-admin-panel/prd.md`）
- 现有 Supabase 配置

### 输出

- 角色权限矩阵确认（admin/editor/viewer）
- `profiles` 表 `role` 列 migration 方案
- RLS 策略设计
- Auth Hook 设计（`profiles.role` → JWT `app_metadata.role` 同步）

### 步骤

1. 读取 PRD 中的角色定义（第 4.4 节）
2. 读取 [rbac-design.md](rbac-design.md) 中的权限矩阵模板
3. 确认角色与操作对应关系（如需要调整）
4. 输出 RBAC 设计确认文档

### 关键决策点

- `role` 存储位置：`profiles` 表 `role` 列（enum），还是 `auth.users.raw_app_meta_data`
- JWT 同步方式：Supabase Auth Hook（推荐）还是 API 中间件从 DB 读取
- 建议：`profiles.role` + Auth Hook

## 阶段 2：Admin API 开发

### 输入

- RBAC 设计确认文档
- 后端技术栈（Next.js 16 + Zod + Supabase）

### 输出

- Auth 中间件（`requireRole`）
- 11 个 Admin API 端点
- API 测试

### 步骤

1. 加载 `backend-development` skill
2. 按 ST-01（Auth 中间件 + RLS）→ ST-02（CRUD API）顺序执行
3. Auth 中间件实现：
   - JWT 验证 + 解析 `app_metadata.role`
   - `requireRole(...roles)` 工厂函数
4. RLS 策略：
   - 创建 migration：`supabase/migrations/<ts>_admin_rls.sql`
   - DROP 旧宽松策略 → CREATE 新细粒度策略
   - 创建 Auth Hook function
5. CRUD API 按 [admin-api-standards.md](admin-api-standards.md) 实现
6. 编写测试

### API 响应格式

- 列表接口：`{ data: [...], pagination: { page, page_size, total, total_pages } }`
- 单条 CRUD：`{ data: { ... } }`
- 统计接口：`{ data: { dramas: N, episodes: N, users: N } }`
- 不使用 `{ code, message }` 包装层

### 子任务映射

| 子任务 | 对应 ST | 工时 |
|--------|--------|------|
| Auth 中间件 + RLS | ST-01 | 2 人日 |
| 管理 CRUD API | ST-02 | 3 人日 |

## 阶段 3：Admin UI 开发

### 输入

- Admin API（至少已 Mock）
- Web 端技术栈（Next.js 16 + React 19 + CSS Modules）

### 输出

- 管理平台路由骨架 + 登录页
- 短剧管理页
- 剧集管理页
- 用户管理页
- 仪表盘

### 步骤

1. 加载 `web-development` skill
2. 按 ST-03 → ST-04 → ST-05 → ST-06 → ST-07 顺序执行
3. 每个页面按 [flat-ui-standards.md](flat-ui-standards.md) 约束实现

### 路由结构

```
web/src/app/admin/
├── layout.tsx                    # AdminShell（导航 + Header + 内容区）
├── login/page.tsx                # 登录页
├── dashboard/page.tsx            # 仪表盘（Iteration 2）
├── dramas/page.tsx               # 短剧列表
├── dramas/[id]/episodes/page.tsx # 剧集管理（某短剧下）
└── users/page.tsx                # 用户管理（admin only）
```

### 代码分层（遵循 web/CLAUDE.md 五层架构）

```
web/src/
├── app/admin/              # Page 层（路由委托）
├── features/admin/         # Feature 层（页面级组件）
│   ├── login/
│   ├── dashboard/
│   ├── dramas/
│   ├── episodes/
│   └── users/
├── components/ui/          # 复用 Shared UI
├── lib/                    # 复用 Core（api-client、schemas）
└── styles/                 # 复用 Design System（tokens.css）
```

### 子任务映射

| 子任务 | 对应 ST | 迭代 | 工时 |
|--------|--------|------|------|
| Shell + 登录 | ST-03 | 1 | 4 人日 |
| 短剧管理 | ST-04 | 1 | 3.5 人日 |
| 剧集管理 | ST-05 | 1 | 3 人日 |
| 用户管理 + RBAC UI | ST-06 | 1 | 2 人日 |
| 仪表盘 + Viewer | ST-07 | 2 | 1.5 人日 |

## 阶段 4：权限联调

### 步骤

1. 测试 admin 角色：可操作所有功能
2. 测试 editor 角色：可管理内容，不可访问用户管理页
3. 测试 viewer 角色：只读浏览，无编辑/删除按钮
4. 测试未登录：重定向到登录页
5. 测试越权访问：直接输入 `/admin/users` URL（editor/viewer）→ 403

## 阶段 5：测试与交付

### 测试清单

- [ ] Backend API 单元测试（中间件 + 各端点）
- [ ] Web 组件测试（各页面渲染、状态、交互）
- [ ] 权限场景手动验证
- [ ] 浏览器兼容性（Chrome/Firefox/Safari 最新版桌面端）

### 已知限制

- 管理平台面向桌面端，不保证移动端响应式体验
- Supabase 项目需先确认配置就绪
