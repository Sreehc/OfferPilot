# OfferPilot Tech Design

## 技术基线

- 后端：Spring Boot 单体应用
- 前端：React SPA
- 数据库：MySQL
- 缓存：Redis
- 文件存储：本地存储
- AI：Spring AI

当前仓库仍可见 Spring Boot 3.x 基线；迁移时可评估 Spring Boot 4.0，但优先级服从 Spring AI 架构收口和整体可交付性。

## 架构原则

- `LlmGateway` 负责统一聊天模型接入。
- `EmbeddingGateway` 负责统一向量接入。
- Agent 层负责 run、step、审批、工具调用、记忆和审计。
- 领域 service 保持业务边界，必要时通过工具层暴露给 Agent。
- 前端继续使用现有 React + Ant Design 技术栈。

## 关键设计

- Spring AI 作为模型与工具调用主通道。
- Provider 配置保留用户级别管理能力。
- 运行日志、配额和限流继续保留。
- 画像、掌握度和复习计划要可写回。

## 迁移原则

- 以 Spring AI 为主线重构，不维护两套 AI 运行时。
- 允许 API、表结构和迁移脚本调整。
- 只保留必要配置。
- 历史运行数据不作为强兼容目标。

## 交付边界

- Chat、Agent、Interview、Knowledge、Analytics、Admin 可独立演进。
- 前端先保证主流程可用，再做体验收口。
