import type { KnowledgeDocItem } from '@/types/api'

type SeedQuery = {
  seedTopic?: string
  seedWorkflow?: string
  seedNote?: string
}

export const canAskWithKnowledgeDocument = (doc: KnowledgeDocItem) =>
  doc.status === 'indexed' && doc.indexStatus === 'indexed'

export const buildKnowledgeChatTarget = (doc: KnowledgeDocItem, seed?: SeedQuery) => ({
  path: '/chat',
  query: {
    knowledgeScope: doc.libraryScope === 'system' ? 'system' : 'personal',
    sourceDocId: String(doc.id),
    sourceDocTitle: doc.title,
    ...(seed?.seedTopic ? { seedTopic: seed.seedTopic } : {}),
    ...(seed?.seedWorkflow ? { seedWorkflow: seed.seedWorkflow } : {}),
    ...(seed?.seedNote ? { seedNote: seed.seedNote } : {})
  }
})
