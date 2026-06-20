import { useState } from 'react'
import { Button, Form, Input, App as AntApp } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { AuthFrame } from './AuthFrame'
import { useAuthStore } from '@/features/auth/authStore'
import { getErrorMessage } from '@/api/client'

export function RegisterPage() {
  const navigate = useNavigate()
  const register = useAuthStore((state) => state.register)
  const { message } = AntApp.useApp()
  const [submitting, setSubmitting] = useState(false)
  const onFinish = async (values: { username: string; password: string; confirmPassword?: string; email?: string; nickname?: string }) => {
    setSubmitting(true)
    try {
      await register({ username: values.username, password: values.password, email: values.email, nickname: values.nickname })
      message.success('账号已创建，正在进入工作台')
      navigate('/dashboard', { replace: true })
    } catch (error) {
      message.error(getErrorMessage(error, '注册失败'))
    } finally {
      setSubmitting(false)
    }
  }
  return (
    <AuthFrame title="创建账号" description="建立你的训练记录和能力画像">
      <Form layout="vertical" size="large" onFinish={onFinish}>
        <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}><Input autoComplete="username" /></Form.Item>
        <Form.Item label="邮箱（选填，用于找回密码）" name="email" rules={[{ type: 'email', message: '请输入有效的邮箱地址' }]}><Input type="email" autoComplete="email" /></Form.Item>
        <Form.Item label="昵称（选填）" name="nickname"><Input /></Form.Item>
        <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }, { min: 8, message: '密码至少 8 位' }, { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '密码需同时包含字母和数字' }]} hasFeedback><Input.Password autoComplete="new-password" placeholder="至少 8 位，含字母和数字" /></Form.Item>
        <Form.Item label="确认密码" name="confirmPassword" dependencies={['password']} hasFeedback rules={[{ required: true, message: '请再次输入密码' }, ({ getFieldValue }) => ({ validator(_, value) { return !value || getFieldValue('password') === value ? Promise.resolve() : Promise.reject(new Error('两次输入的密码不一致')) } })]}><Input.Password autoComplete="new-password" /></Form.Item>
        <Button type="primary" htmlType="submit" block loading={submitting}>注册并进入</Button>
        <p><Link to="/login">已有账号，去登录</Link></p>
      </Form>
    </AuthFrame>
  )
}
