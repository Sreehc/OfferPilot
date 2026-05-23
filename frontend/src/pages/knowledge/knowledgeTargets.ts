import type { KnowledgeDocItem } from '@/types/api'

export const canAskWithKnowledgeDocument = (doc: KnowledgeDocItem) =>
  doc.status === 'indexed' && doc.indexStatus === 'indexed'

export const buildKnowledgeChatTarget = (doc: KnowledgeDocItem) => ({
  path: '/chat',
  query: {
    knowledgeScope: doc.libraryScope === 'system' ? 'system' : 'personal',
    sourceDocId: String(doc.id),
    sourceDocTitle: doc.title
  }
})
