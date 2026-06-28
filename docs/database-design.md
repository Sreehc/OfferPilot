# OfferPilot Database Design

## 数据原则

- 以 MySQL 为主存储。
- 迁移脚本按能力分文件维护。
- 允许对 schema 做重构和增量迁移。
- 历史运行数据不要求完全兼容，但核心业务数据尽量保留。

## 核心表域

### 账号与安全

- `user`
- `login_device`
- `login_log`
- `user_provider_config`

### 内容与知识库

- `category`
- `knowledge_doc`
- `knowledge_chunk`
- `knowledge_card`
- `knowledge_card_task`
- `knowledge_card_log`
- `daily_card_task`
- `daily_memory_snapshot`

### 对话

- `chat_session`
- `chat_message`

### 题库、错题与复习

- `question`
- `wrong_question`
- `review_log`
- `study_plan`
- `study_plan_task`

### 面试与 AI 训练

- `interview_session`
- `interview_record`
- `voice_record`
- `job_prep_session`
- `copilot_prep_session`
- `copilot_realtime_session`
- `copilot_event`
- `recording_review_session`
- `recording_transcript_segment`

### 简历与投递

- `resume_file`
- `resume_project`
- `resume_version`
- `job_application`
- `job_application_event`

### Agent 与审计

- `agent_run`
- `ai_call_log`

### 社区、通知与配置

- `community_question`
- `community_answer`
- `community_vote`
- `notification`
- `system_config`
- `system_config_history`
- `user_stats`

## 迁移脚本

- `migrate_agent_run.sql`
- `migrate_agent_run_approval.sql`
- `migrate_agent_run_tool_call_telemetry.sql`
- `migrate_chat_message_client_message_id.sql`
- `migrate_chat_message_feedback.sql`
- `migrate_job_prep_session.sql`
- `migrate_copilot_prep_session.sql`
- `migrate_copilot_realtime_session.sql`
- `migrate_recording_review_session.sql`
- `migrate_recording_review_async_status.sql`
- `migrate_user_provider_config.sql`

## 说明

- `agent_run` 需要支撑审批和工具调用审计。
- `chat_message` 已承载会话消息和反馈。
- `interview` 相关表覆盖模拟面试、JD 备面、实时 Copilot 和录音复盘。
- `user_provider_config` 是用户级模型配置入口。
- `system_config` 和 `ai_call_log` 支撑治理与观测。
