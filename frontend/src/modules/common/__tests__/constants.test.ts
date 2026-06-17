import { describe, expect, it } from 'vitest'
import { fallbackMetrics } from '../constants'

describe('common module fixtures', () => {
  it('keeps dashboard fallback metrics available', () => {
    expect(fallbackMetrics).toHaveLength(4)
    expect(fallbackMetrics.map((item) => item.label)).toContain('Agent 建议')
  })
})
