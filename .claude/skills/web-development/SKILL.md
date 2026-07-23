---
name: web-development
description: >
  Web 前端技术方案设计与编码规范。
  凡涉及 Web 端的技术方案设计、功能开发、代码修改、架构决策、测试编写、
  构建配置等，都必须在第一时间加载本 skill。
  触发场景：用户提到"Web"、"前端"、"Next.js"、"React"、"TypeScript"、"web/"；
  需求涉及 Web 前端实现；Web 端技术方案评审；Web 端 bug 修复或功能迭代。
  优先级最高——任何 Web 相关任务都不允许跳过本 skill。
---

# Web Development

> ⚠️ **强制加载**: 任何涉及 Web 端的任务（方案设计、编码、review、测试）都必须首先加载本 skill。

## 定位

Web 端是 [PRODUCT.md](../../../PRODUCT.md) 的 Web 前端实现，负责 TypeScript + Next.js + React 工程的全生命周期开发。

## 规范索引

各规范已拆分到独立文件以便按需加载，详见下表：

| 规范 | 文件 | 说明 |
|------|------|------|
| **代码规范** | [standards/coding-standards.md](standards/coding-standards.md) | TypeScript、React、CSS、文件命名、CR 清单 |
| **架构设计** | [standards/architecture.md](standards/architecture.md) | 分层架构、路由、状态管理、数据请求、SSR/SSG/ISR、错误处理 |
| **基础库与基础能力** | [standards/foundation.md](standards/foundation.md) | HTTP、校验、表单、i18n、A11y、埋点、SEO |
| **编译、运行与调试** | [standards/build-and-debug.md](standards/build-and-debug.md) | Next.js 构建、DevTools、性能分析 |
| **AI 操作与自动化** | [standards/ai-operations.md](standards/ai-operations.md) | Playwright/Puppeteer、截图、请求拦截、日志 |
| **测试规范** | [standards/testing.md](standards/testing.md) | 单元/组件/E2E/视觉回归测试、覆盖率 |
| **安全** | [standards/security.md](standards/security.md) | XSS、CSRF、认证、敏感信息 |
| **CI/CD** | [standards/cicd.md](standards/cicd.md) | 流水线、自动部署、Preview Deploy |
| **开源库选型** | [open-source-libs.md](open-source-libs.md) | 各领域库清单与选型状态 |
| **常见问题** | [faq.md](faq.md) | 常见开发问题与解决方案 |

## 文件结构

```
web-development/
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

- [Web CLAUDE.md](../../../web/CLAUDE.md) — Web 端工程级约束
- [PRODUCT.md](../../../PRODUCT.md) — 产品信息
