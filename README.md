# OfferPilot

OfferPilot 是一个面向 Java 求职者的 AI 面试训练与求职管理平台。它把题库训练、知识问答、模拟面试、学习计划、简历整理和投递管理串成一条连续工作流，让用户进入首页后就能判断“现在先做什么”。

## 核心功能

- **求职训练工作台**：首页集中展示当前主任务、训练摘要、计划进度、简历状态和投递推进情况
- **题库训练**：按关键字、分类、题型、难度、岗位方向和标签筛题，查看题目详情、追问建议和常见错误
- **知识库与问答**：上传资料、检索文档、基于知识库或简历项目继续提问，获得带引用来源的回答
- **模拟面试**：通过文字或语音模式验证表达、追问和答题质量，查看评分、点评和历史复盘
- **学习计划**：生成 7 / 14 / 30 天计划，执行每日任务并刷新后续节奏
- **简历助手**：上传并解析简历，查看项目拆分、项目追问、自我介绍和面试简历提纲
- **投递管理**：记录岗位、分析 JD、更新状态、维护事件时间线
- **数据分析**：回看训练趋势、计划执行、简历准备和投递相关洞察
- **管理后台**：查看用户、题库、文档、AI 日志、系统配置和面试治理

## 主线流程

```text
进入工作台
  -> 题库训练定位薄弱点
  -> 知识库与问答补齐理解
  -> 模拟面试验证表达
  -> 学习计划安排接下来几天的训练
  -> 简历助手整理项目与自我介绍
  -> 投递管理记录和推进岗位
  -> 数据分析与后台治理辅助复盘
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3.5、Java 17、MyBatis-Plus 3.5.7、MySQL 8、Redis Stack |
| 安全 | Spring Security、JWT 无状态认证、Redis Token 黑名单、CORS 白名单 |
| AI | OpenAI-compatible LLM + Embedding 网关抽象 |
| 文档解析 | Apache Tika（Markdown / TXT / PDF） |
| 前端 | Vue 3、Vite、TypeScript、Pinia、Vue Router、Axios、Element Plus、Tailwind CSS 3、ECharts |
| 部署 | Docker Compose、Nginx 反向代理 |

## 目录结构

```text
OfferPilot
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

默认端口 `8080`，Swagger 文档：<http://localhost:8080/doc.html>

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
| `OFFERPILOT_DB_HOST` | MySQL 主机 | localhost |
| `OFFERPILOT_DB_PORT` | MySQL 端口 | 3306 |
| `OFFERPILOT_DB_NAME` | 数据库名 | offerpilot |
| `OFFERPILOT_DB_USERNAME` | 数据库用户 | root |
| `OFFERPILOT_DB_PASSWORD` | 数据库密码 | 123456 |
| `OFFERPILOT_REDIS_HOST` | Redis 主机 | localhost |
| `OFFERPILOT_REDIS_PORT` | Redis 端口 | 6379 |
| `OFFERPILOT_JWT_SECRET` | JWT 密钥 | — |
| `OFFERPILOT_LLM_BASE_URL` | LLM API 地址 | — |
| `OFFERPILOT_LLM_API_KEY` | LLM API Key | — |
| `OFFERPILOT_LLM_MODEL` | 模型名称 | — |
| `OFFERPILOT_EMBEDDING_ENABLED` | 向量检索开关 | false |
| `OFFERPILOT_EMBEDDING_BASE_URL` | Embedding API 地址 | 同 `OFFERPILOT_LLM_BASE_URL` |
| `OFFERPILOT_EMBEDDING_API_KEY` | Embedding API Key | 同 `OFFERPILOT_LLM_API_KEY` |
| `OFFERPILOT_EMBEDDING_MODEL` | Embedding 模型 | text-embedding-3-small |
| `OFFERPILOT_EMBEDDING_DIMENSIONS` | 向量维度 | 1536 |
| `OFFERPILOT_VECTOR_THRESHOLD` | 向量相似度阈值 | 0.3 |
| `OFFERPILOT_CORS_ORIGINS` | CORS 允许域名 | http://localhost:5173 |

### 前端

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | API 基础路径 | /api |
| `VITE_APP_TITLE` | 应用标题 | OfferPilot |

## 生产部署

```bash
export OFFERPILOT_JWT_SECRET=your-secret-key
export OFFERPILOT_DB_PASSWORD=your-db-password
export OFFERPILOT_CORS_ORIGINS=https://your-domain.com
export OFFERPILOT_LLM_BASE_URL=https://api.openai.com/v1
export OFFERPILOT_LLM_API_KEY=your-api-key
export OFFERPILOT_LLM_MODEL=gpt-4o

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

- **工作台主线**：首页围绕当前主任务、计划进度、简历状态和投递推进组织，不让辅助模块抢占主叙事
- **AI 网关抽象**：`LlmGateway` / `EmbeddingGateway` 接口 + OpenAI-compatible 实现，可切换不同兼容提供商
- **RAG 检索**：Redis Stack 向量检索 + 关键词混合检索，Apache Tika 解析多格式文档后自动切分向量化
- **事务管理**：LLM 调用与数据库事务分离，避免长事务占用连接；通过 `@Lazy` self-injection 解决 Spring 代理自调用问题
- **安全设计**：JWT 无状态认证 + Redis Token 黑名单 + CORS 白名单 + XSS 防护
- **速率与配额**：Redis 滑动窗口限流（`RateLimitInterceptor`）+ 每用户每日 LLM 调用配额
- **可观测性**：请求日志拦截器、Actuator 健康检查、Redis 缓存与治理日志
- **前端质量**：TypeScript strict 模式、组件拆分、ErrorBoundary 全局错误捕获、路由懒加载、主题切换

## License

MIT
