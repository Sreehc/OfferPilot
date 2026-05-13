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
  email?: string
  emailVerified?: boolean
  emailVerifiedAt?: string
  githubLinked?: boolean
  githubUsername?: string
  role: string
  status?: number
  createTime?: string
  lastLoginTime?: string
}

export interface LoginResponse {
  token: string
  userInfo: UserInfo
  deviceId?: number
  requires2fa?: boolean
  tempToken?: string
}

export interface TwoFactorStatus {
  enabled: boolean
  recoveryCodesRemaining?: number
}

export interface TwoFactorSetup {
  otpauthUri: string
  secret: string
}

export interface AuthDelivery {
  message: string
  maskedEmail?: string
  expiresInMinutes?: number
  debugCode?: string
}

export interface OAuthProviderInfo {
  provider: string
  displayName: string
  enabled: boolean
  configured: boolean
  authUrl?: string | null
}

export interface TwoFactorEnable {
  recoveryCodes: string[]
}

export interface RecentInterviewItem {
  sessionId: string
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

export interface CategoryMasteryItem {
  categoryId?: number | null
  categoryName: string
  totalCards: number
  masteredCards: number
  dueCount: number
  masteryRate: number
}

export interface DashboardOverview {
  learningCount: number
  averageScore: number
  wrongCount: number
  recentInterviews: RecentInterviewItem[]
  weakPoints: WeakPointItem[]
  firstVisit: boolean
  todayLearnCards?: number
  todayReviewCards?: number
  todayCompletedCards?: number
  todayCardCompletionRate?: number
  masteredCardCount?: number
  reviewDebtCount?: number
  studyStreak?: number
  todayCompletionStatus?: string
  reviewDebtDelta?: number
  categoryMasterySummary?: CategoryMasteryItem[]
  overallAbility?: number
  recommendedDifficulty?: string
  weakCategories?: string[]
  suggestedFocus?: string | null
  categoryAbilities?: CategoryAbility[]

  // Analytics insights
  thisWeekAvgScore?: number
  lastWeekAvgScore?: number
  thisWeekInterviewCount?: number
  categoryChanges?: CategoryChange[]
  bestStudyHours?: HourDistribution[]
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
  jobDirection?: string
  applicableScope?: string
  tags?: string
  standardAnswer?: string
  interviewAnswer?: string
  followUpSuggestions?: string
  commonMistakes?: string
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
  libraryScope?: 'system' | 'personal' | string
  businessType?: string
  sourceType?: string
  fileType?: string
  fileUrl?: string
  parseStatus?: 'pending' | 'parsed' | 'failed' | string
  indexStatus?: 'pending' | 'indexed' | 'failed' | string
  status: 'draft' | 'parsed' | 'indexed'
  summary?: string
  chunkCount?: number
  cardDeckId?: number
  cardDeckTitle?: string
  cardCount?: number
  cardGeneratedAt?: string
  cardTypes?: string
  updateTime?: string
}

export interface KnowledgeReferenceItem {
  docId: number
  chunkId: number
  docTitle: string
  snippet: string
  score?: number
  libraryScope?: 'system' | 'personal' | string
  businessType?: string
  fileType?: string
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

export type ChatAnswerMode = 'learning' | 'interview' | 'concise' | 'project'
export type ChatKnowledgeScope = 'all' | 'system' | 'personal'

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
  answerMode?: ChatAnswerMode
  knowledgeScope?: ChatKnowledgeScope
  references: KnowledgeReferenceItem[]
  suggestedQuestions?: string[]
}

export interface InterviewCurrentQuestion {
  sessionId: string
  currentIndex: number
  questionCount: number
  questionId: string
  questionTitle: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
}

export interface InterviewScoreDimension {
  dimension: string
  score: number
  summary: string
}

export interface InterviewAnswerResult {
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewRecordItem {
  questionId: string
  questionTitle: string
  userAnswer: string
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  isLowScore?: boolean
  recommendedCardFront?: string
  recommendedCardBack?: string
  recommendedCardExplanation?: string
  recommendedCardFollowUp?: string
  generatedCardId?: string
  voiceTranscript?: string
  voiceConfidence?: number
}

export interface InterviewDetail {
  sessionId: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  status: string
  mode?: string
  totalScore: number
  questionCount: number
  startTime?: string
  endTime?: string
  cardsGenerated?: boolean
  generatedCardCount?: number
  interviewDeckId?: string
  interviewDeckTitle?: string
  records: InterviewRecordItem[]
}

export interface VoiceSubmitResult {
  transcript: string
  transcriptConfidence?: number
  transcriptTimeMs?: number
  score: number
  comment: string
  standardAnswer: string
  followUp: string
  scoreBreakdown?: InterviewScoreDimension[]
  weakPointTags?: string[]
  reviewSummary?: string
  addedToWrongBook: boolean
  hasNextQuestion: boolean
}

export interface InterviewHistoryItem {
  sessionId: string
  direction: string
  jobRole?: string
  experienceLevel?: string
  techStack?: string
  durationMinutes?: number
  includeResumeProject?: boolean
  status: string
  mode?: string
  totalScore: number
  questionCount: number
  startTime?: string
  endTime?: string
  cardsGenerated?: boolean
  generatedCardCount?: number
  interviewDeckId?: string
}

export interface StudyPlanTaskItem {
  id: string
  dayIndex: number
  taskDate: string
  module: 'question' | 'chat' | 'review' | 'interview' | string
  title: string
  description: string
  actionPath: string
  estimatedMinutes: number
  priority: 'high' | 'medium' | 'low' | string
  status: 'pending' | 'completed' | string
  completedAt?: string
}

export interface StudyPlan {
  id: string
  title: string
  durationDays: number
  focusDirection: string
  targetRole: string
  techStack: string
  weakPoints: string[]
  reviewSuggestion: string
  status: 'active' | 'completed' | 'archived' | string
  startDate: string
  endDate: string
  currentDay: number
  progressRate: number
  totalTaskCount: number
  completedTaskCount: number
  todayTaskCount: number
  dailyTargetMinutes: number
  tasks: StudyPlanTaskItem[]
}

export interface WrongQuestionItem {
  id: number
  questionId: number
  title: string
  masteryLevel: 'not_started' | 'reviewing' | 'mastered'
  standardAnswer?: string
  errorReason?: string
  easeFactor?: number
  intervalDays?: number
  nextReviewDate?: string
  streak?: number
  reviewCount?: number
}

export interface ReviewTodayItem {
  wrongQuestionId: number
  questionId: number
  title: string
  standardAnswer?: string
  errorReason?: string
  easeFactor?: number
  intervalDays?: number
  streak?: number
  nextReviewDate?: string
  overdueDays: number
  masteryLevel: string
}

export type ReviewContentType = 'all' | 'knowledge_card' | 'wrong_card' | 'interview_card'

export interface UnifiedReviewItem {
  reviewItemId: string
  contentType: ReviewContentType | string
  sourceType?: string
  title: string
  answer?: string
  explanation?: string
  easeFactor?: number
  intervalDays?: number
  streak?: number
  nextReviewDate?: string
  nextReviewAt?: string
  overdueDays: number
  masteryLevel: string
  wrongQuestionId?: string
  cardId?: string
  deckId?: string
  deckTitle?: string
  sourceQuote?: string
}

export interface ReviewTodayData {
  selectedContentType: ReviewContentType | string
  totalPending: number
  overdueCount: number
  todayCompleted: number
  currentStreak: number
  countsByContentType: Record<string, number>
  items: UnifiedReviewItem[]
}

export interface ReviewStats {
  totalReviews: number
  currentStreak: number
  todayPending: number
  todayCompleted?: number
  overdueCount?: number
  contentTypeDistribution?: Record<string, number>
  heatmap?: Record<string, number>
}

export interface KnowledgeCardItem {
  id: string
  question: string
  answer: string
  explanation?: string
  cardType?: 'concept' | 'qa' | 'scenario' | 'compare' | string
  difficulty?: 'easy' | 'medium' | 'hard' | 'auto' | string
  tags?: string
  sourceQuote?: string
  sortOrder: number
  scheduledDay: number
  state: 'new' | 'learning' | 'weak' | 'mastered'
  reviewCount: number
  sourceRefId?: string | null
  sourceRefType?: 'knowledge_chunk' | 'wrong_question' | string | null
  lastRating?: number | null
  easeFactor?: number
  intervalDays?: number
  streak?: number
}

export interface KnowledgeCardTask {
  id: string
  docId: string
  docTitle: string
  deckTitle?: string
  sourceType?: 'knowledge_doc' | 'wrong_auto' | string
  status: 'draft' | 'active' | 'completed' | 'invalid'
  isCurrent?: number
  days: number
  currentDay: number
  dailyTarget: number
  totalCards: number
  masteredCards: number
  reviewCount: number
  estimatedMinutes?: number
  invalidReason?: string
  startedAt?: string
  completedAt?: string
  lastStudiedAt?: string
  dueCount: number
  reviewedTodayCount: number
  currentCard?: KnowledgeCardItem | null
  cards: KnowledgeCardItem[]
}

export interface TodayCardsTask {
  deckId: string
  deckTitle: string
  sourceType: 'knowledge_doc' | 'wrong_auto' | string
  status: 'draft' | 'active' | 'completed' | 'invalid'
  todayLearnCount: number
  todayReviewCount: number
  todayCompletedCount: number
  completionRate: number
  estimatedMinutes: number
  streak: number
  tomorrowDueCount: number
  totalCards: number
  masteredCards: number
  dueCount: number
  reviewedTodayCount: number
  currentCard?: KnowledgeCardItem | null
}

export interface CardDeckSummary {
  deckId: string
  deckTitle: string
  sourceType: 'knowledge_doc' | 'wrong_auto' | string
  status: 'draft' | 'active' | 'completed' | 'invalid'
  totalCards: number
  masteredCards: number
  dueCount: number
  reviewedTodayCount: number
  isCurrent: number
  lastStudiedAt?: string | null
}

export interface CardStatsSummary {
  currentDeckId?: string | null
  deckCount: number
  totalCards: number
  masteredCards: number
  dueTodayCount: number
  streak: number
  completionRate: number
}

export type CardGenerateType = 'concept' | 'qa' | 'scenario' | 'compare'
export type CardGenerateDifficulty = 'auto' | 'easy' | 'medium' | 'hard'

export interface CommunityQuestion {
  id: number
  userId: number
  authorName?: string
  authorRank?: string
  title: string
  content: string
  categoryId?: number
  categoryName?: string
  status: string
  upvoteCount: number
  answerCount: number
  accepted?: boolean
  hasVoted?: boolean
  createdAt?: string
}

export interface CommunityAnswer {
  id: number
  questionId: number
  userId: number
  authorName?: string
  authorRank?: string
  content: string
  isAccepted: boolean
  upvoteCount: number
  hasVoted?: boolean
  createdAt?: string
}

export interface CommunityQuestionDetail extends CommunityQuestion {
  answers: CommunityAnswer[]
}

export interface LeaderboardEntry {
  userId: number
  username?: string
  rankTitle: string
  communityScore: number
  communityQuestions: number
  communityAnswers: number
  communityAccepted: number
  position: number
}

export interface CategoryAbility {
  categoryId: number
  categoryName: string
  abilityScore: number
  interviewCount: number
  wrongCount: number
  isWeak: boolean
  recommendedDifficulty: string
}

export interface AbilityProfile {
  overallAbility: number
  recommendedDifficulty: string
  categoryAbilities: CategoryAbility[]
  weakCategories: string[]
  suggestedFocus: string | null
}

export interface RecommendInterview {
  direction: string
  questionCount: number
  reason: string
  difficulty: string
}

export interface RecommendQuestion {
  questionId: number
  title: string
  categoryId: number
  categoryName?: string
  difficulty: string
  reason: string
}

export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string
  link?: string
  isRead: boolean
  createTime: string
}

export interface WeeklyPoint {
  week: string
  score: number
  count: number
}

export interface MemoryTrendPoint {
  week: string
  value: number
  count: number
}

export interface CategoryTrend {
  categoryId: number
  categoryName: string
  points: WeeklyPoint[]
}

export interface AbilityTrend {
  weeks: string[]
  completionRateTrend: MemoryTrendPoint[]
  reviewDebtTrend: MemoryTrendPoint[]
  masteredGrowthTrend: MemoryTrendPoint[]
  overallTrend: WeeklyPoint[]
  categoryTrends: CategoryTrend[]
}

export interface WeeklyEF {
  week: string
  avgEF: number
  reviewCount: number
}

export interface WeeklyForgettingRate {
  week: string
  forgettingRate: number
  totalRatings: number
  againCount: number
}

export interface EfficiencyData {
  avgEaseFactor: number
  efTrend: WeeklyEF[]
  ratingDistribution: Record<number, number>
  forgettingRateTrend: WeeklyForgettingRate[]
  completionRateTrend: Array<{
    label: string
    completionRate: number
    plannedCount: number
    completedCount: number
  }>
  reviewDebtTrend: Array<{
    label: string
    reviewDebtCount: number
  }>
  masteredGrowthTrend: Array<{
    label: string
    masteredCardCount: number
  }>
  masteryDistribution: Record<string, number>
  contentTypeDistribution?: Record<string, number>
  categoryMastery?: CategoryMasteryItem[]
  totalReviews: number
  currentStreak: number
  reviewCompletionRate?: number
  forgettingRate?: number
}

export interface CategoryChange {
  categoryId: number
  categoryName: string
  thisWeekScore: number
  lastWeekScore: number
  change: number
}

export interface HourDistribution {
  timeSlot: string
  sessionCount: number
  avgScore: number
}

export interface LearningInsights {
  thisWeekAvgScore: number
  lastWeekAvgScore: number
  thisWeekInterviewCount: number
  lastWeekInterviewCount: number
  todayCompletionStatus?: string
  reviewDebtStatus?: string
  masteryGrowthStatus?: string
  categoryChanges: CategoryChange[]
  bestStudyHours: HourDistribution[]
}

export interface LoginDeviceItem {
  id: number
  deviceFingerprint: string
  deviceName?: string
  ip?: string
  city?: string
  lastActiveTime: string
  createTime: string
  current: boolean
}

export interface LoginLogItem {
  id: number
  userId: number
  username?: string
  ip?: string
  city?: string
  device?: string
  status: number
  failReason?: string
  createTime: string
}
