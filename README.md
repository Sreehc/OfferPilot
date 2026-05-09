# ByteCoach

ByteCoach 是一个把知识资料持续转成记忆任务的智能学习系统。产品主线不是在多个模块里做选择，而是先完成今天的卡片，再用复习、问答和面试诊断把理解补齐。

## 核心功能

- **智能记忆卡片**：把知识库资料自动生成 deck，并在记忆工作台中形成“今日待学 + 今日待复习”的固定主任务
- **间隔复习**：基于 SM-2 调度记忆节奏，持续更新复习时机、完成率、连续学习天数和掌握状态
- **资料转卡片**：上传 Markdown / TXT / PDF 等资料后，系统自动切分、抽取核心内容并生成可切换的卡片 deck
- **知识库问答**：带资料上下文提问，获得带引用来源的回答，用来补齐卡片里没完全吃透的点
- **面试诊断**：通过限时问答做进阶验证，获得 AI 评分、点评、标准答案和追问
- **复习中心**：统一处理到期复习、错题沉淀和复盘积压
- **数据分析**：查看学习趋势、掌握分布、记忆强度和当前重点
- **管理后台**：分类、题库、知识文档 CRUD，支持种子资料导入（ADMIN 角色）
- **暗色模式**：全局深色 / 浅色主题切换，偏好持久化

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3.5、Java 17、MyBatis-Plus 3.5.7、MySQL 8、Redis Stack（RediSearch HNSW 向量检索） |
| 安全 | Spring Security、JWT 无状态认证、Redis Token 黑名单、CORS 白名单 |
| AI | OpenAI-compatible LLM + Embedding 网关抽象，支持任意兼容 API |
| 文档解析 | Apache Tika（Markdown / TXT / PDF） |
| 前端 | Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Element Plus、Tailwind CSS 3、ECharts |
| 部署 | Docker Compose、Nginx 反向代理 |

## 目录结构

```text
ByteCoach
├── backend/       Spring Boot 单体后端
├── frontend/      Vue 3 前端应用
├── sql/           数据库初始化与迁移脚本
└── deploy/        Docker Compose + Nginx 部署配置
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8.x
- Redis 7.x（需启用 RediSearch 模块）

## 快速启动

### 1. 初始化数据库

执行 `sql/init.sql` 创建表结构，再执行 `sql/initdata.sql` 插入测试数据。
如果数据库是在两步验证字段加入前初始化的，再额外执行 `sql/migrate_2fa_user_columns.sql` 补齐 `user` 表字段。

### 2. 启动本地依赖

```bash
cd deploy
docker compose up -d
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 `8080`，Swagger 文档：http://localhost:8080/doc.html

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口 `5173`

## 默认测试账号

- 用户名：`demo`
- 密码：`123456`

## 环境变量

### 后端

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `BYTECOACH_DB_HOST` | MySQL 主机 | localhost |
| `BYTECOACH_DB_PORT` | MySQL 端口 | 3306 |
| `BYTECOACH_DB_NAME` | 数据库名 | bytecoach |
| `BYTECOACH_DB_USERNAME` | 数据库用户 | root |
| `BYTECOACH_DB_PASSWORD` | 数据库密码 | 123456 |
| `BYTECOACH_REDIS_HOST` | Redis 主机 | localhost |
| `BYTECOACH_REDIS_PORT` | Redis 端口 | 6379 |
| `BYTECOACH_JWT_SECRET` | JWT 密钥 | — |
| `BYTECOACH_LLM_BASE_URL` | LLM API 地址 | — |
| `BYTECOACH_LLM_API_KEY` | LLM API Key | — |
| `BYTECOACH_LLM_MODEL` | 模型名称 | — |
| `BYTECOACH_EMBEDDING_ENABLED` | 向量检索开关 | false |
| `BYTECOACH_EMBEDDING_BASE_URL` | Embedding API 地址 | 同 LLM_BASE_URL |
| `BYTECOACH_EMBEDDING_API_KEY` | Embedding API Key | 同 LLM_API_KEY |
| `BYTECOACH_EMBEDDING_MODEL` | Embedding 模型 | text-embedding-3-small |
| `BYTECOACH_EMBEDDING_DIMENSIONS` | 向量维度 | 1536 |
| `BYTECOACH_VECTOR_THRESHOLD` | 向量相似度阈值 | 0.3 |
| `BYTECOACH_CORS_ORIGINS` | CORS 允许域名 | http://localhost:5173 |

### 前端

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | API 基础路径 | /api |
| `VITE_APP_TITLE` | 应用标题 | ByteCoach |

## 生产部署

```bash
export BYTECOACH_JWT_SECRET=your-secret-key
export BYTECOACH_DB_PASSWORD=your-db-password
export BYTECOACH_CORS_ORIGINS=https://your-domain.com
export BYTECOACH_LLM_BASE_URL=https://api.openai.com/v1
export BYTECOACH_LLM_API_KEY=your-api-key
export BYTECOACH_LLM_MODEL=gpt-4o

cd deploy
docker compose -f docker-compose.prod.yml up -d
```

前端通过 Nginx 反向代理，访问 `http://localhost` 即可。健康检查：`http://localhost:8080/actuator/health`

## 运行测试

```bash
# 后端测试
cd backend
mvn test

# 前端测试
cd frontend
npm run test
```

## 技术亮点

- **卡片主线**：资料生成卡片任务，首页围绕“今日待学、今日待复习、完成率、连续天数”组织主任务
- **事务管理**：LLM 调用与数据库事务分离，避免长事务占用连接；通过 `@Lazy` self-injection 解决 Spring 代理自调用问题
- **AI 网关抽象**：`LlmGateway` / `EmbeddingGateway` 接口 + OpenAI-compatible 实现，可无缝切换不同 LLM 提供商
- **RAG 检索**：Redis Stack RediSearch HNSW 向量语义检索 + 关键词混合检索，Embedding 批量化，Apache Tika 解析多格式文档后自动切分向量化
- **安全设计**：JWT 无状态认证 + Redis Token 黑名单 + CORS 白名单 + XSS 防护
- **速率与配额**：Redis 滑动窗口限流（`RateLimitInterceptor`）+ 每用户每日 LLM 调用配额（`LlmQuotaService`，默认 100 次/天）
- **可观测性**：请求日志拦截器、Actuator 健康检查、Redis 缓存、`@Scheduled` 计划健康检查
- **前端质量**：TypeScript strict 模式、组件拆分、ErrorBoundary 全局错误捕获、路由懒加载、暗色模式

## License

MIT
