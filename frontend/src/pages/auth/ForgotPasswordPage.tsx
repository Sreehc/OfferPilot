import { Button, Form, Input, App as AntApp } from 'antd'
import { Link } from 'react-router-dom'
import { forgotPasswordApi, resetPasswordApi } from '@/api/modules/auth'
import { getErrorMessage } from '@/api/client'
import { AuthFrame } from './AuthFrame'

export function ForgotPasswordPage() {
  const { message } = AntApp.useApp()
  const onSend = async ({ email }: { email: string }) => { try { await forgotPasswordApi(email); message.success('如果邮箱存在，验证码将发送到你的邮箱') } catch (error) { message.error(getErrorMessage(error)) } }
  const onReset = async (values: any) => { try { await resetPasswordApi(values); message.success('密码已重置') } catch (error) { message.error(getErrorMessage(error)) } }
  return <AuthFrame title="找回密码" description="通过邮箱验证码重置登录密码"><Form layout="vertical" onFinish={onReset}><Form.Item label="邮箱" name="email" rules={[{ required: true }]}><Input.Search enterButton="发送验证码" onSearch={(email) => onSend({ email })} /></Form.Item><Form.Item label="验证码" name="code" rules={[{ required: true }]}><Input /></Form.Item><Form.Item label="新密码" name="newPassword" rules={[{ required: true }]}><Input.Password /></Form.Item><Button type="primary" htmlType="submit" block>重置密码</Button><p><Link to="/login">返回登录</Link></p></Form></AuthFrame>
}
