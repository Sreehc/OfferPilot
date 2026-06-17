import { Button, Form, Input, App as AntApp } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { AuthFrame } from './AuthFrame'
import { useAuthStore } from '@/features/auth/authStore'
import { getErrorMessage } from '@/api/client'

export function RegisterPage() {
  const navigate = useNavigate()
  const register = useAuthStore((state) => state.register)
  const { message } = AntApp.useApp()
  const onFinish = async (values: { username: string; password: string; email?: string; nickname?: string }) => { try { await register(values); navigate('/dashboard', { replace: true }) } catch (error) { message.error(getErrorMessage(error, '注册失败')) } }
  return <AuthFrame title="创建账号" description="建立你的训练记录和能力画像"><Form layout="vertical" onFinish={onFinish}><Form.Item label="用户名" name="username" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="邮箱" name="email"><Input type="email" /></Form.Item><Form.Item label="昵称" name="nickname"><Input /></Form.Item><Form.Item label="密码" name="password" rules={[{ required: true }]}><Input.Password /></Form.Item><Button type="primary" htmlType="submit" block>注册并进入</Button><p><Link to="/login">已有账号，去登录</Link></p></Form></AuthFrame>
}
