import { Button, Form, Input, App as AntApp } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { AuthFrame } from './AuthFrame'
import { useAuthStore } from '@/features/auth/authStore'
import { getErrorMessage } from '@/api/client'

export function TwoFactorVerifyPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const verify = useAuthStore((state) => state.verifyTwoFactor)
  const { message } = AntApp.useApp()
  const tempToken = (location.state as any)?.tempToken || ''
  const onFinish = async ({ code }: { code: string }) => { try { await verify(tempToken, code); navigate('/dashboard', { replace: true }) } catch (error) { message.error(getErrorMessage(error, '验证失败')) } }
  return <AuthFrame title="双因素验证" description="输入认证器中的 6 位验证码"><Form layout="vertical" onFinish={onFinish}><Form.Item label="验证码" name="code" rules={[{ required: true }]}><Input.OTP length={6} /></Form.Item><Button type="primary" htmlType="submit" block disabled={!tempToken}>验证并进入</Button></Form></AuthFrame>
}
