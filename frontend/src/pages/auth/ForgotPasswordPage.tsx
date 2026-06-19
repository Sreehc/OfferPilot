import { useEffect, useRef, useState } from 'react'
import { Button, Form, Input, Space, App as AntApp } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { forgotPasswordApi, resetPasswordApi } from '@/api/modules/auth'
import { getErrorMessage } from '@/api/client'
import { AuthFrame } from './AuthFrame'

export function ForgotPasswordPage() {
  const { message } = AntApp.useApp()
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [countdown, setCountdown] = useState(0)
  const [sending, setSending] = useState(false)
  const [resetting, setResetting] = useState(false)
  const timer = useRef<ReturnType<typeof setInterval>>()

  useEffect(() => () => clearInterval(timer.current), [])

  const startCountdown = () => {
    setCountdown(60)
    clearInterval(timer.current)
    timer.current = setInterval(() => setCountdown((value) => {
      if (value <= 1) { clearInterval(timer.current); return 0 }
      return value - 1
    }), 1000)
  }

  const onSend = async () => {
    try {
      const email = await form.validateFields(['email'])
      setSending(true)
      await forgotPasswordApi(email.email)
      message.success('如果邮箱存在，验证码将发送到你的邮箱，10 分钟内有效')
      startCountdown()
    } catch (error) {
      if ((error as any)?.errorFields) return
      message.error(getErrorMessage(error))
    } finally {
      setSending(false)
    }
  }

  const onReset = async (values: any) => {
    setResetting(true)
    try {
      await resetPasswordApi(values)
      message.success('密码已重置，请使用新密码登录')
      navigate('/login', { replace: true })
    } catch (error) {
      message.error(getErrorMessage(error))
    } finally {
      setResetting(false)
    }
  }

  return (
    <AuthFrame title="找回密码" description="通过邮箱验证码重置登录密码">
      <Form form={form} layout="vertical" onFinish={onReset}>
        <Form.Item label="邮箱" name="email" rules={[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '请输入有效的邮箱地址' }]}>
          <Space.Compact style={{ width: '100%' }}>
            <Input style={{ flex: 1 }} placeholder="注册时使用的邮箱" />
            <Button onClick={onSend} loading={sending} disabled={countdown > 0}>{countdown > 0 ? `${countdown}s 后重试` : '发送验证码'}</Button>
          </Space.Compact>
        </Form.Item>
        <Form.Item label="验证码" name="code" rules={[{ required: true, message: '请输入验证码' }]}><Input placeholder="邮箱收到的 6 位验证码" /></Form.Item>
        <Form.Item label="新密码" name="newPassword" rules={[{ required: true, message: '请输入新密码' }, { min: 8, message: '密码至少 8 位' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字' }]} hasFeedback><Input.Password placeholder="至少 8 位，含字母和数字" /></Form.Item>
        <Button type="primary" htmlType="submit" block loading={resetting}>重置密码</Button>
        <p><Link to="/login">返回登录</Link></p>
      </Form>
    </AuthFrame>
  )
}
