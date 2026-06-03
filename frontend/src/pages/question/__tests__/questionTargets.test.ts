import { describe, expect, it } from 'vitest'
import type { QuestionItem } from '@/types/api'
import { buildQuestionChatTarget, buildQuestionInterviewTarget, questionTagList } from '../questionTargets'

const question: QuestionItem = {
  id: 42,
  title: '说一下 Redis 持久化',
  categoryId: 7,
  categoryName: 'Redis',
  difficulty: 'hard',
  jobDirection: 'Java 后端',
  tags: 'Redis, 持久化\nAOF'
}

describe('questionTargets', () => {
  it('splits tags consistently for downstream targets', () => {
    expect(questionTagList(question.tags)).toEqual(['Redis', '持久化', 'AOF'])
  })

  it('builds chat target with question context', () => {
    expect(buildQuestionChatTarget(question)).toEqual({
      path: '/chat',
      query: {
        sourceQuestionId: '42',
        sourceQuestionTitle: '说一下 Redis 持久化',
        sourceQuestionCategory: 'Redis',
        sourceQuestionTag: 'Redis',
        sourceQuestionDirection: 'Java 后端'
      }
    })
  })

  it('builds interview target with the same source context', () => {
    expect(buildQuestionInterviewTarget(question)).toEqual({
      path: '/interview',
      query: {
        sourceQuestionId: '42',
        sourceQuestionTitle: '说一下 Redis 持久化',
        sourceQuestionCategory: 'Redis',
        sourceQuestionTag: 'Redis',
        sourceQuestionDirection: 'Java 后端'
      }
    })
  })

  it('carries seed context into downstream targets when present', () => {
    expect(buildQuestionChatTarget(question, {
      seedTopic: 'Redis',
      seedWorkflow: 'analytics',
      seedNote: '优先补 Redis 持久化表达'
    })).toEqual({
      path: '/chat',
      query: {
        sourceQuestionId: '42',
        sourceQuestionTitle: '说一下 Redis 持久化',
        sourceQuestionCategory: 'Redis',
        sourceQuestionTag: 'Redis',
        sourceQuestionDirection: 'Java 后端',
        seedTopic: 'Redis',
        seedWorkflow: 'analytics',
        seedNote: '优先补 Redis 持久化表达'
      }
    })
  })
})
