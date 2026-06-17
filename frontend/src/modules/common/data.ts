import type { AnyRecord, PageResult } from '@/api/types'

export function normalizeRecords<T extends AnyRecord = AnyRecord>(input: unknown): T[] {
  if (Array.isArray(input)) return input as T[]
  if (!input || typeof input !== 'object') return []
  const value = input as PageResult<T> & AnyRecord
  if (Array.isArray(value.records)) return value.records
  if (Array.isArray(value.list)) return value.list
  if (Array.isArray(value.items)) return value.items
  if (Array.isArray(value.data)) return value.data
  return []
}

export function getTotal(input: unknown, fallback = 0) {
  if (!input || typeof input !== 'object') return fallback
  const value = input as PageResult<AnyRecord> & AnyRecord
  return Number(value.total ?? value.count ?? normalizeRecords(value).length ?? fallback)
}

export function getRecordId(record: AnyRecord, fallback = 'id') {
  return String(record.id ?? record.runId ?? record.sessionId ?? record.questionId ?? record.applicationId ?? record.docId ?? record.resumeId ?? record[fallback] ?? JSON.stringify(record))
}

export function pickText(record: AnyRecord | undefined, keys: string[], fallback = '-') {
  if (!record) return fallback
  for (const key of keys) {
    const value = record[key]
    if (value !== undefined && value !== null && String(value).trim()) return String(value)
  }
  return fallback
}

export function pickNumber(record: AnyRecord | undefined, keys: string[], fallback = 0) {
  if (!record) return fallback
  for (const key of keys) {
    const value = Number(record[key])
    if (Number.isFinite(value)) return value
  }
  return fallback
}

export function pickArray<T = unknown>(record: AnyRecord | undefined, keys: string[]): T[] {
  if (!record) return []
  for (const key of keys) {
    const value = record[key]
    if (Array.isArray(value)) return value as T[]
    const nested = normalizeRecords<T & AnyRecord>(value)
    if (nested.length) return nested as T[]
  }
  return []
}

export function buildCategorySeries(records: AnyRecord[], labelKeys: string[], valueKeys: string[]) {
  return {
    labels: records.map((record, index) => pickText(record, labelKeys, String(index + 1))),
    values: records.map((record) => pickNumber(record, valueKeys, 0))
  }
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function formatDateTime(value?: string | number | Date | null) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

export function formatPercent(value?: number | null) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) return '-'
  const percent = Number(value)
  return `${percent}%`
}
