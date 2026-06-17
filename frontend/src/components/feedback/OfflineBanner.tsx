import { WifiOutlined } from '@ant-design/icons'
import { Alert } from 'antd'
import { useEffect, useState } from 'react'

export function OfflineBanner() {
  const [online, setOnline] = useState(() => typeof navigator === 'undefined' ? true : navigator.onLine)

  useEffect(() => {
    const sync = () => setOnline(navigator.onLine)
    window.addEventListener('online', sync)
    window.addEventListener('offline', sync)
    return () => {
      window.removeEventListener('online', sync)
      window.removeEventListener('offline', sync)
    }
  }, [])

  if (online) return null
  return (
    <Alert
      className="offline-banner"
      type="warning"
      showIcon
      icon={<WifiOutlined />}
      message="当前网络不可用，页面会保留已加载内容，新的操作将在网络恢复后重试。"
    />
  )
}
