import { describe, expect, it } from 'vitest'
import { isProviderStatusAvailable, isProviderStatusMissing } from '../providerReadiness'

describe('providerReadiness', () => {
  it('treats ready and saved as available provider states', () => {
    expect(isProviderStatusAvailable('ready')).toBe(true)
    expect(isProviderStatusAvailable('saved')).toBe(true)
    expect(isProviderStatusMissing('ready')).toBe(false)
    expect(isProviderStatusMissing('saved')).toBe(false)
  })

  it('treats missing and incomplete as unavailable provider states', () => {
    expect(isProviderStatusAvailable('missing')).toBe(false)
    expect(isProviderStatusAvailable('incomplete')).toBe(false)
    expect(isProviderStatusMissing('missing')).toBe(true)
    expect(isProviderStatusMissing('incomplete')).toBe(true)
  })
})
