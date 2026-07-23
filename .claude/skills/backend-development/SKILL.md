---
name: backend-development
description: >
  Backend 端技术方案设计与编码规范。
  凡涉及后端的技术方案设计、功能开发、代码修改、架构决策、API 设计、测试编写、
  数据库操作等，都必须在第一时间加载本 skill。
  触发场景：用户提到"后端"、"backend"、"API"、"接口"、"数据库"、"Supabase"、"backend/"；
  需求涉及后端实现；后端技术方案评审；后端 bug 修复或功能迭代。
  优先级最高——任何 Backend 相关任务都不允许跳过本 skill。
---

# Backend Development

> ⚠️ **强制加载**: 任何涉及 Backend 端的任务（方案设计、编码、review、测试）都必须首先加载本 skill。

## 定位

Backend 端是 [PRODUCT.md](../../../PRODUCT.md) 的后端服务实现，负责 TypeScript + Next.js + Supabase 工程的全生命周期开发。

## 规范索引

各规范已拆分到独立文件以便按需加载，详见下表：

| 规范 | 文件 | 说明 |
|------|------|------|
| **代码规范** | [standards/coding-standards.md](standards/coding-standards.md) | TypeScript、API 路由、命名规范、CR 清单 |
| **架构设计** | [standards/architecture.md](standards/architecture.md) | 分层架构、API 设计、认证授权、数据库设计、缓存、错误处理 |
| **基础库与基础能力** | [standards/foundation.md](standards/foundation.md) | HTTP、校验、数据库访问、文件存储、日志、任务队列、i18n |
| **编译、运行与调试** | [standards/build-and-debug.md](standards/build-and-debug.md) | 环境配置、常用命令、调试、性能分析 |
| **AI 操作与自动化** | [standards/ai-operations.md](standards/ai-operations.md) | API 测试、数据库操作、日志分析、Mock |
| **测试规范** | [standards/testing.md](standards/testing.md) | 单元/集成/数据库测试、覆盖率 |
| **安全** | [standards/security.md](standards/security.md) | 输入校验、API 安全、密钥管理、依赖审计 |
| **CI/CD** | [standards/cicd.md](standards/cicd.md) | 流水线、自动测试、自动部署、Migration |
| **开源库选型** | [open-source-libs.md](open-source-libs.md) | 各领域库清单与选型状态 |
| **常见问题** | [faq.md](faq.md) | 常见开发问题与解决方案 |

## 文件结构

```
backend-development/
├── SKILL.md
├── references/
│   ├── open-source-libs.md
│   ├── faq.md
│   └── standards/
│       ├── coding-standards.md
│       ├── architecture.md
│       ├── foundation.md
│       ├── build-and-debug.md
│       ├── ai-operations.md
│       ├── testing.md
│       ├── security.md
│       └── cicd.md
├── assets/
└── scripts/
```

## 参考

- [Backend CLAUDE.md](../../../backend/CLAUDE.md) — Backend 端工程级约束
- [PRODUCT.md](../../../PRODUCT.md) — 产品信息
