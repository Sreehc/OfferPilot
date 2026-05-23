import type { QuestionItem } from '@/types/api'

export const questionTagList = (tags?: string) => {
  return (tags ?? '')
    .split(/[,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

export const buildQuestionChatTarget = (question: QuestionItem) => ({
  path: '/chat',
  query: {
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: questionTagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }
})

export const buildQuestionInterviewTarget = (question: QuestionItem) => ({
  path: '/interview',
  query: {
    sourceQuestionId: String(question.id),
    sourceQuestionTitle: question.title,
    sourceQuestionCategory: question.categoryName || '',
    sourceQuestionTag: questionTagList(question.tags)[0] || '',
    sourceQuestionDirection: question.jobDirection || ''
  }
})
