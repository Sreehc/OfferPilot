import { useState } from 'react'
import { Alert, Button, Form, Input, Space, App as AntApp } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { AuthFrame } from './AuthFrame'
import { useAuthStore } from '@/features/auth/authStore'
import { getErrorMessage } from '@/api/client'

export function TwoFactorVerifyPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const verify = useAuthStore((state) => state.verifyTwoFactor)
  const { message } = AntApp.useApp()
  const [submitting, setSubmitting] = useState(false)
  const tempToken = (location.state as any)?.tempToken || ''
  const onFinish = async ({ code }: { code: string }) => {
    setSubmitting(true)
    try {
      await verify(tempToken, code)
      navigate('/dashboard', { replace: true })
    } catch (error) {
      message.error(getErrorMessage(error, '验证失败'))
    } finally {
      setSubmitting(false)
    }
  }
  if (!tempToken) {
    return (
      <AuthFrame title="双因素验证" description="安全校验">
        <Space orientation="vertical" style={{ width: '100%' }} size={16}>
          <Alert type="warning" showIcon message="验证已失效" description="登录态已过期或页面被直接打开，请重新登录后再进行双因素验证。" />
          <Button type="primary" block onClick={() => navigate('/login', { replace: true })}>返回登录</Button>
        </Space>
      </AuthFrame>
    )
  }
  return (
    <AuthFrame title="双因素验证" description="输入认证器中的 6 位验证码">
      <Form layout="vertical" onFinish={onFinish}>
        <Form.Item label="验证码" name="code" rules={[{ required: true, message: '请输入验证码' }]}><Input.OTP length={6} /></Form.Item>
        <Button type="primary" htmlType="submit" block loading={submitting}>验证并进入</Button>
        <p style={{ marginTop: 12, marginBottom: 0 }}><a onClick={() => navigate('/login', { replace: true })}>返回登录</a></p>
      </Form>
    </AuthFrame>
  )
}
