// 统一枚举 → 中文映射层。面向用户的页面不应直接展示后端原始英文枚举。
// 用法：labelOf(value) 做状态/通用映射；difficultyLabel/contentTypeLabel/masteryLabel/targetTypeLabel 做领域映射。

const STATUS_MAP: Record<string, string> = {
  READY: '可用',
  ACTIVE: '可用',
  ONLINE: '在线',
  ENABLED: '已启用',
  PENDING: '待处理',
  WAITING: '等待中',
  PROCESSING: '处理中',
  RUNNING: '运行中',
  INDEXING: '索引中',
  QUEUED: '排队中',
  APPROVAL: '待审批',
  DONE: '已完成',
  COMPLETED: '已完成',
  SUCCESS: '成功',
  FINISHED: '已结束',
  FAILED: '失败',
  ERROR: '错误',
  REJECTED: '已拒绝',
  BANNED: '已封禁',
  DISABLED: '已禁用',
  CANCELLED: '已取消',
  CANCELED: '已取消',
  OPEN: '待解决',
  SOLVED: '已解决',
  CLOSED: '已关闭',
  DRAFT: '草稿',
  SAVED: '待投递',
  WRITTEN: '笔试 / 作业',
  STANDARD: '标准',
  UNKNOWN: '未知'
}

const DIFFICULTY_MAP: Record<string, string> = {
  EASY: '简单',
  MEDIUM: '中等',
  HARD: '困难'
}

const CONTENT_TYPE_MAP: Record<string, string> = {
  QUESTION: '题目',
  KNOWLEDGE: '知识',
  INTERVIEW: '面试',
  RESUME: '简历',
  ALL: '全部'
}

const MASTERY_MAP: Record<string, string> = {
  NOT_STARTED: '未开始',
  REVIEWING: '复习中',
  MASTERED: '已掌握'
}

const TARGET_TYPE_MAP: Record<string, string> = {
  QUESTION: '题目',
  KNOWLEDGE: '知识',
  APPLICATION: '投递',
  RESUME: '简历',
  COMMUNITY: '社区'
}

const APPLICATION_STATUS_MAP: Record<string, string> = {
  SAVED: '待投递',
  DRAFT: '草稿',
  APPLIED: '已投递',
  SCREENING: '筛选中',
  WRITTEN: '笔试 / 作业',
  INTERVIEW: '面试中',
  INTERVIEWING: '面试中',
  OFFER: 'Offer',
  REJECTED: '已拒绝',
  CLOSED: '已关闭',
  UNKNOWN: '未知状态'
}

function lookup(map: Record<string, string>, value?: string | null, fallback?: string): string {
  if (value === undefined || value === null || String(value).trim() === '') return fallback ?? '-'
  const key = String(value).trim().toUpperCase()
  return map[key] ?? fallback ?? String(value)
}

/** 通用状态映射，找不到时回退原值（已做大写归一）。 */
export function labelOf(value?: string | null, fallback?: string): string {
  return lookup(STATUS_MAP, value, fallback)
}

export function difficultyLabel(value?: string | null): string {
  return lookup(DIFFICULTY_MAP, value)
}

export function contentTypeLabel(value?: string | null): string {
  return lookup(CONTENT_TYPE_MAP, value)
}

export function masteryLabel(value?: string | null): string {
  return lookup(MASTERY_MAP, value)
}

export function targetTypeLabel(value?: string | null): string {
  return lookup(TARGET_TYPE_MAP, value)
}

export function applicationStatusLabel(value?: string | null): string {
  return lookup(APPLICATION_STATUS_MAP, value)
}
