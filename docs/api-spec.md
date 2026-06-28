# OfferPilot API Spec

## 接口约定

- 统一走 `/api/**`
- 列表接口支持筛选、分页和排序
- 上传接口使用 `multipart/form-data`
- Chat 流式回复使用 SSE

## 主要接口

### Auth

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `POST /api/auth/email/send-verification-code`
- `POST /api/auth/email/verify`
- `POST /api/auth/password/forgot`
- `POST /api/auth/password/reset`
- `GET /api/auth/oauth/providers`
- `GET /api/auth/login-logs`
- `GET /api/auth/captcha`

### Dashboard / Analytics

- `GET /api/dashboard/overview`
- `GET /api/analytics/trend`
- `GET /api/analytics/efficiency`
- `GET /api/analytics/insights`
- `GET /api/analytics/profile`
- `GET /api/analytics/profile/topics/{topicId}`
- `POST /api/analytics/profile/topics/{topicId}/retrospectives`

### Chat

- `POST /api/chat/send`
- `POST /api/chat/stream`
- `POST /api/chat/attachments`
- `GET /api/chat/sessions`
- `GET /api/chat/messages/{sessionId}`
- `POST /api/chat/messages/{messageId}/regenerate`
- `POST /api/chat/messages/{messageId}/feedback`
- `PUT /api/chat/session/{sessionId}`
- `DELETE /api/chat/session/{sessionId}`

### Agent / Provider Settings

- `POST /api/agent/runs`
- `GET /api/agent/runs`
- `GET /api/agent/runs/{runId}`
- `POST /api/agent/runs/{runId}/approve`
- `POST /api/agent/runs/{runId}/reject`
- `POST /api/agent/runs/{runId}/cancel`
- `GET /api/settings/providers`
- `PUT /api/settings/providers`
- `POST /api/settings/providers/check`

### Knowledge / Question / Wrong / Review

- `GET /api/knowledge/list`
- `POST /api/knowledge/search`
- `POST /api/knowledge/upload`
- `GET /api/knowledge/my`
- `GET /api/knowledge/{docId}`
- `DELETE /api/knowledge/{docId}`
- `GET /api/question/list`
- `GET /api/question/{id}`
- `GET /api/wrong/list`
- `GET /api/wrong/{id}`
- `PUT /api/wrong/mastery/{id}`
- `DELETE /api/wrong/delete/{id}`
- `GET /api/review/today`
- `POST /api/review/{id}/rate`
- `GET /api/review/stats`

### Interview / Resume / Applications / Plan

- `POST /api/interview/start`
- `GET /api/interview/current/{sessionId}`
- `POST /api/interview/answer`
- `GET /api/interview/detail/{sessionId}`
- `GET /api/interview/history`
- `GET /api/interview/trend`
- `POST /api/interview/job-prep/sessions`
- `POST /api/interview/copilot/prep-sessions`
- `POST /api/interview/copilot/realtime-sessions`
- `POST /api/interview/recording-reviews`
- `GET /api/resume/list`
- `POST /api/resume/upload`
- `GET /api/resume/latest`
- `GET /api/resume/{resumeId}/score`
- `GET /api/applications/board`
- `POST /api/applications`
- `PUT /api/applications/{applicationId}/status`
- `POST /api/applications/{applicationId}/events`
- `POST /api/applications/{applicationId}/analysis`
- `POST /api/plan/generate`
- `GET /api/plan/current`
- `POST /api/plan/task/{taskId}/status`
- `POST /api/plan/{planId}/refresh`

### Admin

- `GET /api/admin/overview`
- `GET /api/admin/overview/trend`
- `GET /api/admin/ai-logs`
- `GET /api/admin/ai-logs/summary`
- `GET /api/admin/runtime-governance/summary`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`
- `POST /api/admin/users/{id}/ban`
- `POST /api/admin/users/{id}/unban`
- `GET /api/admin/interviews`
- `GET /api/admin/interviews/summary`
- `GET /api/admin/community/pending`
- `POST /api/admin/community/{id}/approve`
- `POST /api/admin/community/{id}/reject`
- `GET /api/admin/system-config`
- `PUT /api/admin/system-config/{configKey}`

## 说明

- 这里记录的是当前 controller 已暴露的主接口。
- 字段细节以后端 DTO / VO 和实际响应为准。
- 如果迁移到 Spring AI 或新表结构，优先保持路径语义稳定。
