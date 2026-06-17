import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import type { PropsWithChildren } from 'react'
import { darkTheme, lightTheme } from '@/styles/theme'
import { useThemeStore } from '@/features/theme/themeStore'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
    mutations: { retry: 0 }
  }
})

export function AppProviders({ children }: PropsWithChildren) {
  const mode = useThemeStore((state) => state.mode)
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN} theme={mode === 'dark' ? darkTheme : lightTheme}>
        <AntApp>{children}</AntApp>
      </ConfigProvider>
    </QueryClientProvider>
  )
}
