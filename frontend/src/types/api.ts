export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar?: string
  role: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
}

export interface RecentInterviewItem {
  sessionId: number
  direction: string
  totalScore: number
  status: string
  finishedAt: string
}

export interface WeakPointItem {
  categoryName: string
  wrongCount: number
  score: number
}

export interface DashboardOverview {
  learningCount: number
  averageScore: number
  wrongCount: number
  planCompletionRate: number
  planHealthScore: number
  recentInterviews: RecentInterviewItem[]
  weakPoints: WeakPointItem[]
  firstVisit: boolean
}

export interface CategoryItem {
  id: number
  name: string
  type: 'question' | 'knowledge' | 'interview'
  sortOrder?: number
  status?: number
}

export interface QuestionItem {
  id: number
  title: string
  categoryId: number
  categoryName?: string
  type?: string
  difficulty: 'easy' | 'medium' | 'hard'
  frequency?: number
  tags?: string
  standardAnswer?: string
  scoreStandard?: string
  source?: string
  createTime?: string
  updateTime?: string
}

export interface KnowledgeDocItem {
  id: number
  title: string
  categoryId?: number
  categoryName?: string
  sourceType?: string
  fileUrl?: string
  status: 'draft' | 'parsed' | 'indexed'
  summary?: string
  chunkCount?: number
  updateTime?: string
}

export interface KnowledgeReferenceItem {
  docId: number
  chunkId: number
  docTitle: string
  snippet: string
  score?: number
}

export interface KnowledgeSearchResult {
  query: string
  references: KnowledgeReferenceItem[]
}

export interface ChatSessionItem {
  id: number
  title: string
  mode: 'chat' | 'rag'
  lastMessageTime?: string
  updateTime?: string
}

export interface ChatMessageItem {
  id: number
  role: 'user' | 'assistant'
  messageType: string
  content: string
  createTime: string
  references: KnowledgeReferenceItem[]
}

export interface ChatSendResult {
  sessionId: number
  sessionTitle: string
  answer: string
  references: KnowledgeReferenceItem[]
}

export interface InterviewCurrentQuestion {
  sessionId: number
  currentIndex: number
  questionCount: number
  questionId: number
  questionTitle: string
}

export interface InterviewAnswerResult {
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewRecordItem {
  questionId: number
  questionTitle: string
  userAnswer: string
  score: number
  comment: string
  standardAnswer: string
  followUp: string
}

export interface InterviewDetail {
  sessionId: number
  direction: string
  status: string
  totalScore: number
  questionCount: number
  records: InterviewRecordItem[]
}

export interface WrongQuestionItem {
  id: number
  questionId: number
  title: string
  masteryLevel: 'not_started' | 'reviewing' | 'mastered'
  standardAnswer?: string
  errorReason?: string
}

export interface StudyPlanItem {
  id: number
  title: string
  goal: string
  status: string
  version?: number
  parentPlanId?: number
  startDate?: string
  endDate?: string
  totalTasks?: number
  completedTasks?: number
  healthScore?: number
  tasks: StudyPlanTaskItem[]
}

export interface StudyPlanTaskItem {
  id: number
  taskDate: string
  taskType: string
  content: string
  status: 'todo' | 'done'
}
