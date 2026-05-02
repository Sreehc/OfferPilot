# ByteCoach

ByteCoach 是一个面向 Java 后端学习与面试准备的智能学习系统。通过 AI 模拟面试、智能问答、错题沉淀和个性化学习计划，帮助学习者系统性地提升面试能力。

## 核心功能

- **智能问答**：Chat / RAG 两种模式，支持知识库引用溯源
- **模拟面试**：AI 评分、点评、标准答案、追问，低分题自动入错题本
- **错题本**：自动沉淀薄弱题目，掌握状态三态切换（未开始 / 复习中 / 已掌握）
- **学习计划**：基于薄弱点 AI 生成 N 天学习计划，每日任务状态追踪
- **数据看板**：学习次数、平均分、错题数、计划完成率、薄弱点雷达图
- **知识库**：内置种子资料导入、文档切分、向量语义检索 + 关键词混合检索
- **管理后台**：分类、题库、知识文档 CRUD（ADMIN 角色）

## 技术栈

- **后端**：Spring Boot 3、Java 17、MyBatis-Plus、MySQL 8、Redis Stack（RediSearch 向量检索）、Spring Security、JWT、Knife4j
- **前端**：Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Element Plus、Tailwind CSS、ECharts
- **AI**：OpenAI-compatible LLM + Embedding 网关抽象，支持任意兼容 API

## 目录结构

```text
ByteCoach
├── backend/       Spring Boot 单体后端
├── frontend/      Vue 3 前端应用
├── sql/           数据库初始化与迁移脚本
└── deploy/        Docker Compose 部署配置
```

## 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 20+
- MySQL 8.x
- Redis 7.x

## 快速启动

### 1. 初始化数据库

执行 `sql/init.sql`。

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

默认端口 `5173`，默认账号：`demo` / `123456`

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

## 一期技术亮点

- **事务管理**：LLM 调用与数据库事务分离，避免长事务占用连接；通过 `@Lazy` self-injection 解决 Spring 代理自调用问题
- **AI 网关抽象**：`LlmGateway` 接口 + OpenAI-compatible 实现，可无缝切换不同 LLM 提供商
- **RAG 检索**：Redis Stack RediSearch 向量语义检索 + 关键词混合检索，Embedding 批量化，自动向量化文档 chunk
- **安全设计**：JWT 无状态认证 + Redis Token 黑名单 + CORS 白名单 + XSS 防护
- **前端质量**：TypeScript strict 模式、组件拆分、ErrorBoundary 全局错误捕获、路由懒加载
- **可观测性**：结构化日志、日志轮转、Actuator 健康检查、Redis 缓存

## License

MIT
