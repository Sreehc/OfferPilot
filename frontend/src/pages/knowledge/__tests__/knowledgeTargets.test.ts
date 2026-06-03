import { describe, expect, it } from 'vitest'
import type { KnowledgeDocItem } from '@/types/api'
import { buildKnowledgeChatTarget, canAskWithKnowledgeDocument } from '../knowledgeTargets'

describe('knowledgeTargets', () => {
  it('allows chat only when document and index are both ready', () => {
    const readyDoc: KnowledgeDocItem = {
      id: 1,
      title: 'JVM 调优笔记',
      status: 'indexed',
      indexStatus: 'indexed',
      libraryScope: 'system'
    }
    const pendingDoc: KnowledgeDocItem = {
      id: 2,
      title: '项目复盘',
      status: 'parsed',
      indexStatus: 'pending',
      libraryScope: 'personal'
    }

    expect(canAskWithKnowledgeDocument(readyDoc)).toBe(true)
    expect(canAskWithKnowledgeDocument(pendingDoc)).toBe(false)
  })

  it('builds chat target with explicit knowledge scope and source doc', () => {
    const systemDoc: KnowledgeDocItem = {
      id: 7,
      title: 'MySQL 索引清单',
      status: 'indexed',
      indexStatus: 'indexed',
      libraryScope: 'system'
    }
    const personalDoc: KnowledgeDocItem = {
      id: 8,
      title: '项目经历草稿',
      status: 'indexed',
      indexStatus: 'indexed',
      libraryScope: 'personal'
    }

    expect(buildKnowledgeChatTarget(systemDoc)).toEqual({
      path: '/chat',
      query: {
        knowledgeScope: 'system',
        sourceDocId: '7',
        sourceDocTitle: 'MySQL 索引清单'
      }
    })

    expect(buildKnowledgeChatTarget(personalDoc)).toEqual({
      path: '/chat',
      query: {
        knowledgeScope: 'personal',
        sourceDocId: '8',
        sourceDocTitle: '项目经历草稿'
      }
    })
  })

  it('carries seed context into chat target when present', () => {
    const systemDoc: KnowledgeDocItem = {
      id: 7,
      title: 'MySQL 索引清单',
      status: 'indexed',
      indexStatus: 'indexed',
      libraryScope: 'system'
    }

    expect(buildKnowledgeChatTarget(systemDoc, {
      seedTopic: 'MySQL',
      seedWorkflow: 'analytics',
      seedNote: '优先补索引与锁的表达'
    })).toEqual({
      path: '/chat',
      query: {
        knowledgeScope: 'system',
        sourceDocId: '7',
        sourceDocTitle: 'MySQL 索引清单',
        seedTopic: 'MySQL',
        seedWorkflow: 'analytics',
        seedNote: '优先补索引与锁的表达'
      }
    })
  })
})
