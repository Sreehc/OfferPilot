export const PRODUCT_PAGE_NAMES = {
  dashboard: '首页',
  question: '题库训练',
  knowledge: '知识库',
  chat: '问答',
  favorites: '我的收藏',
  interview: '模拟面试',
  wrong: '错题本',
  review: '复习巩固',
  studyPlan: '学习计划',
  applications: '投递管理',
  resume: '简历助手',
  analytics: '数据分析',
  settings: '账户设置'
} as const

export const CHAT_MODE_NAMES = {
  direct: '直接提问',
  withKnowledge: '带资料提问',
  withResume: '结合简历提问'
} as const

export const INTERVIEW_MODE_NAMES = {
  plain: '不带简历',
  withResume: '结合简历',
  withProject: '结合项目'
} as const
