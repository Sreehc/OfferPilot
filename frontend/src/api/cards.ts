import { request } from '@/utils/http'
import type {
  CardDeckSummary,
  CardGenerateDifficulty,
  CardGenerateType,
  CardStatsSummary,
  KnowledgeCardTask,
  TodayCardsTask
} from '@/types/api'

export interface CardTaskCreatePayload {
  docId: number
  days: number
}

export interface CardDeckGeneratePayload {
  docId: number
  days: number
  cardTypes: CardGenerateType[]
  cardCount: number
  difficulty: CardGenerateDifficulty
}

export interface CardRatePayload {
  cardId: string
  rating: 1 | 2 | 3 | 4
  responseTimeMs?: number
}

export const createCardTaskApi = (payload: CardTaskCreatePayload) => {
  return request<KnowledgeCardTask>({ url: '/cards/task', method: 'post', data: payload })
}

export const generateCardDeckApi = (payload: CardDeckGeneratePayload) => {
  return request<KnowledgeCardTask>({ url: '/cards/generate', method: 'post', data: payload })
}

export const fetchTodayCardsTaskApi = () => {
  return request<TodayCardsTask | null>({ url: '/cards/today', method: 'get' })
}

export const fetchCardDecksApi = () => {
  return request<CardDeckSummary[]>({ url: '/cards/decks', method: 'get' })
}

export const activateCardDeckApi = (deckId: string) => {
  return request<TodayCardsTask>({ url: `/cards/decks/${deckId}/activate`, method: 'post' })
}

export const reviewDeckCardApi = (deckId: string, payload: CardRatePayload) => {
  return request<TodayCardsTask>({ url: `/cards/${deckId}/review`, method: 'post', data: payload })
}

export const fetchCardStatsApi = () => {
  return request<CardStatsSummary>({ url: '/cards/stats', method: 'get' })
}

export const fetchActiveCardTaskApi = () => {
  return request<KnowledgeCardTask | null>({ url: '/cards/active', method: 'get' })
}

export const fetchCardTaskApi = (taskId: string) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}`, method: 'get' })
}

export const startCardTaskApi = (taskId: string) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/start`, method: 'post' })
}

export const submitCardRateApi = (taskId: string, payload: CardRatePayload) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/rate`, method: 'post', data: payload })
}

export const restartCardTaskApi = (taskId: string) => {
  return request<KnowledgeCardTask>({ url: `/cards/task/${taskId}/restart`, method: 'post' })
}
