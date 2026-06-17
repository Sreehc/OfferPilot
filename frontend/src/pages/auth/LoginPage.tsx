import { Button, Form, Input, Typography, App as AntApp } from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthFrame } from './AuthFrame'
import { useAuthStore } from '@/features/auth/authStore'
import { getErrorMessage } from '@/api/client'

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const login = useAuthStore((state) => state.login)
  const { message } = AntApp.useApp()
  const onFinish = async (values: { username: string; password: string }) => {
    try {
      const data = await login(values)
      if (data.requires2fa && data.tempToken) navigate('/verify-2fa', { state: { tempToken: data.tempToken } })
      else navigate((location.state as any)?.redirect || '/dashboard', { replace: true })
    } catch (error) { message.error(getErrorMessage(error, '登录失败')) }
  }
  return <AuthFrame title="登录 OfferPilot" description="继续你的求职训练闭环"><Form layout="vertical" onFinish={onFinish}><Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}><Input autoComplete="username" /></Form.Item><Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}><Input.Password autoComplete="current-password" /></Form.Item><Button type="primary" htmlType="submit" block>登录</Button><Typography.Paragraph style={{ marginTop: 16, marginBottom: 0 }}><Link to="/register">注册账号</Link><span style={{ margin: '0 8px' }} /> <Link to="/forgot-password">忘记密码</Link></Typography.Paragraph></Form></AuthFrame>
}
