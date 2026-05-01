# ByteCoach

ByteCoach 是一个面向 Java 后端学习与面试准备的智能学习系统。一期目标是跑通 `登录 -> 问答/面试 -> 错题 -> 学习计划 -> Dashboard` 的最小可展示闭环，并保留后续 `RAG`、管理端和 AI 能力扩展位。

## 技术栈

- 后端：`Spring Boot 3`、`Java 17`、`MyBatis-Plus`、`MySQL`、`Redis`、`Spring Security`、`JWT`、`Knife4j/Swagger`
- 前端：`Vue 3`、`Vite`、`TypeScript`、`Pinia`、`Vue Router`、`Axios`、`Element Plus`、`Tailwind CSS`
- AI：单一 `OpenAI-compatible` 网关抽象，阶段 1 接真实 `LLM`，不强制接入 `Qdrant`

## 目录结构

```text
ByteCoach
├── backend/   Spring Boot 单体后端
├── frontend/  Vue 3 前端
├── docs/      需求、架构、接口、数据库文档
├── sql/       初始化脚本
└── deploy/    本地依赖与部署样例
```

## 一期冻结范围

- 保留：登录注册、Dashboard、Chat、Knowledge、Interview、Wrong、Plan、Question/Category 管理入口
- 不做：用户上传资料、长期记忆、多 Agent、代码沙箱、复杂追问状态机、复杂画像系统、完整 RAG 优化平台
- 后台形态：同一个前端应用内嵌 `/admin` 路由，不拆独立 `admin-web`

## 环境要求

- `JDK 17+`
- `Maven 3.9+`
- `Node.js 20+`
- `MySQL 8.x`
- `Redis 7.x`

## 快速启动

### 1. 初始化数据库

执行 [sql/init.sql](/Users/cheers/Desktop/workspace/ByteCoach/sql/init.sql)。

### 2. 启动本地依赖

如果本地没有 `MySQL/Redis`，可以使用 [deploy/docker-compose.yml](/Users/cheers/Desktop/workspace/ByteCoach/deploy/docker-compose.yml)：

```bash
cd deploy
docker compose up -d
```

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

Swagger / Knife4j：

- [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- [http://localhost:8080/doc.html](http://localhost:8080/doc.html)

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5173`

初始化数据默认账号：

- 用户名：`demo`
- 密码：`123456`

## 环境变量

### 后端

使用 [backend/src/main/resources/application-dev.yml](/Users/cheers/Desktop/workspace/ByteCoach/backend/src/main/resources/application-dev.yml) 中的默认配置，或通过环境变量覆盖：

- `BYTECOACH_DB_HOST`
- `BYTECOACH_DB_PORT`
- `BYTECOACH_DB_NAME`
- `BYTECOACH_DB_USERNAME`
- `BYTECOACH_DB_PASSWORD`
- `BYTECOACH_REDIS_HOST`
- `BYTECOACH_REDIS_PORT`
- `BYTECOACH_REDIS_PASSWORD`
- `BYTECOACH_JWT_SECRET`
- `BYTECOACH_LLM_ENABLED`
- `BYTECOACH_LLM_BASE_URL`
- `BYTECOACH_LLM_API_KEY`
- `BYTECOACH_LLM_MODEL`

### 前端

使用 [frontend/.env.development](/Users/cheers/Desktop/workspace/ByteCoach/frontend/.env.development)：

- `VITE_API_BASE_URL`
- `VITE_APP_TITLE`

## 当前实现状态

- **认证**：注册、登录、登出（JWT + Redis 黑名单）
- **智能问答**：Chat / RAG 两种模式，支持知识库引用
- **模拟面试**：AI 评分、点评、标准答案、追问，自动入错题本
- **错题本**：自动沉淀低分题，掌握状态三态切换
- **学习计划**：基于薄弱点 AI 生成 N 天计划，任务状态追踪
- **数据看板**：学习次数、平均分、错题数、计划完成率、薄弱点雷达图
- **知识库**：内置种子资料导入、切分、检索
- **管理后台**：分类、题库、知识文档 CRUD（ADMIN 角色）
- **安全**：JWT 鉴权、CORS、XSS 防护、DTO 参数校验
- **性能**：Redis 缓存、事务范围优化、DB 级预过滤、LLM 超时重试
- **代码质量**：TypeScript strict 模式、Checkstyle、日志轮转、全局错误边界
- **部署**：Dockerfile + docker-compose 生产方案、Actuator 健康检查

## 推荐开发顺序

1. `Auth/User`
2. `Category/Question/Knowledge`
3. `Dashboard`
4. `Chat`
5. `Interview`
6. `Wrong`
7. `Plan`
8. `/admin` 最小管理闭环

## 生产部署

```bash
# 设置环境变量
export BYTECOACH_JWT_SECRET=your-secret-key
export BYTECOACH_DB_PASSWORD=your-db-password
export BYTECOACH_CORS_ORIGINS=https://your-domain.com
export BYTECOACH_LLM_API_KEY=your-api-key

# 一键启动
cd deploy
docker compose -f docker-compose.prod.yml up -d
```

访问 `http://localhost` 即可打开前端，API 通过 Nginx 反向代理到后端。

健康检查：`http://localhost:8080/actuator/health`

## 运行测试

```bash
# 后端测试
cd backend
mvn test

# 前端测试
cd frontend
npm run test
```

## 相关文档

- [项目规划与功能评审](/Users/cheers/Desktop/workspace/ByteCoach/docs/01-项目规划与功能评审.md)
- [MVP 范围与迭代规划](/Users/cheers/Desktop/workspace/ByteCoach/docs/02-MVP范围与迭代规划.md)
- [系统架构设计](/Users/cheers/Desktop/workspace/ByteCoach/docs/03-系统架构设计.md)
- [数据库设计](/Users/cheers/Desktop/workspace/ByteCoach/docs/04-数据库设计.md)
- [接口设计](/Users/cheers/Desktop/workspace/ByteCoach/docs/05-接口设计.md)
