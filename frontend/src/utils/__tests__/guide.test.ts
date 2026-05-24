import { beforeEach, describe, expect, it, vi } from 'vitest'

const setGuideSeenMock = vi.fn()

vi.mock('@/utils/storage', () => ({
  storage: {
    setGuideSeen: setGuideSeenMock
  }
}))

describe('guide utility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('writes guide_seen only when a user id exists', async () => {
    const { markGuideSeenForCriticalAction } = await import('../guide')

    expect(markGuideSeenForCriticalAction(undefined)).toBe(false)
    expect(markGuideSeenForCriticalAction(null)).toBe(false)
    expect(setGuideSeenMock).not.toHaveBeenCalled()

    expect(markGuideSeenForCriticalAction(12)).toBe(true)
    expect(setGuideSeenMock).toHaveBeenCalledWith(12)
  })
})
