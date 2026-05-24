import { storage } from './storage'

export const markGuideSeenForCriticalAction = (userId?: number | string | null) => {
  if (!userId && userId !== 0) return false
  storage.setGuideSeen(userId)
  return true
}
