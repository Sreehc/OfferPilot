import type { QuestionItem } from '@/types/api'

type SeedQuery = {
  seedTopic?: string
  seedWorkflow?: string
  seedNote?: string
}

export const questionTagList = (tags?: string) => {
  return (tags ?? '')
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

const appendSeedQuery = <T extends Record<string, string>>(query: T, seed?: SeedQuery) => ({
  ...query,
  ...(seed?.seedTopic ? { seedTopic: seed.seedTopic } : {}),
  ...(seed?.seedWorkflow ? { seedWorkflow: seed.seedWorkflow } : {}),
  ...(seed?.seedNote ? { seedNote: seed.seedNote } : {})
})

export const buildQuestionChatTarget = (question: QuestionItem, seed?: SeedQuery) => ({
  path: '/chat',
  query: appendSeedQuery({
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: questionTagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }, seed)
})

export const buildQuestionInterviewTarget = (question: QuestionItem, seed?: SeedQuery) => ({
  path: '/interview',
  query: appendSeedQuery({
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: questionTagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }, seed)
})

export const buildQuestionJobPrepTarget = (question: QuestionItem, seed?: SeedQuery) => ({
  path: '/interview',
  query: appendSeedQuery({
    workspace: 'job-prep',
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: questionTagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }, seed)
})
