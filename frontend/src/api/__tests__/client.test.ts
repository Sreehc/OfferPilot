import { describe, expect, it } from 'vitest'
import { getErrorMessage } from '../client'

describe('api client helpers', () => {
  it('extracts server error messages with a fallback', () => {
    expect(getErrorMessage({ message: 'Provider 不可用' })).toBe('Provider 不可用')
    expect(getErrorMessage(null, '默认失败')).toBe('默认失败')
  })
})
