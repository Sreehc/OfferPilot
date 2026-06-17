import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Button, Result } from 'antd'

interface ErrorBoundaryState {
  error?: Error
}

export class ErrorBoundary extends Component<{ children: ReactNode }, ErrorBoundaryState> {
  state: ErrorBoundaryState = {}

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('OfferPilot render failure', error, errorInfo)
  }

  render() {
    if (!this.state.error) return this.props.children
    return (
      <div style={{ minHeight: '100dvh', display: 'grid', placeItems: 'center', padding: 24 }}>
        <Result
          status="500"
          title="页面发生异常"
          subTitle={this.state.error.message || '当前页面无法继续渲染，请刷新后重试。'}
          extra={<Button type="primary" onClick={() => window.location.reload()}>刷新页面</Button>}
        />
      </div>
    )
  }
}
