export const isProviderStatusAvailable = (status?: string) => {
  return status === 'ready' || status === 'saved'
}

export const isProviderStatusMissing = (status?: string) => {
  return !isProviderStatusAvailable(status)
}
