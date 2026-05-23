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

export const KNOWLEDGE_STATUS_NAMES = {
  draft: '处理中',
  parsed: '待提问',
  indexed: '可以提问'
} as const

export const EMPTY_STATE_COPY = {
  leaderboard: {
    title: '当前还没有贡献排名',
    description: '先参与社区回答，后面这里会继续更新贡献排名。'
  },
  knowledgeRecommended: {
    title: '当前还没有可用推荐资料',
    description: '先切到“我的文档”上传一份资料，或调整筛选范围继续找能继续提问的内容。'
  },
  knowledgePersonal: {
    title: '上传你的第一份学习资料',
    description: '上传一份资料，处理完成后就能继续提问。'
  }
} as const

export const ERROR_COPY = {
  chatAnswerFailed: '这次回答还没生成成功，请换个问法或稍后再试。'
} as const
