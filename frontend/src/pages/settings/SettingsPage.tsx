import { CheckCircleOutlined, CloudDownloadOutlined, DesktopOutlined, MailOutlined, SafetyOutlined, UploadOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Avatar, Button, Card, Form, Input, Popconfirm, QRCode, Space, Switch, Tabs, Tag, Typography, Upload } from 'antd'
import type { UploadProps } from 'antd'
import {
  disableTwoFactorApi,
  enableTwoFactorApi,
  exportMyDataApi,
  fetchCurrentUserApi,
  fetchDevicesApi,
  fetchLoginLogsApi,
  fetchTwoFactorStatusApi,
  revokeAllDevicesApi,
  revokeDeviceApi,
  sendEmailVerificationCodeApi,
  setupTwoFactorApi,
  uploadAvatarApi,
  verifyEmailCodeApi
} from '@/api/modules/auth'
import { checkProviderConfigsApi, fetchProviderConfigsApi, updateProviderConfigsApi } from '@/api/modules/settings'
import { getErrorMessage } from '@/api/client'
import { ModulePage, DataTableCard, EntitySummary, downloadBlob, formatDateTime, normalizeRecords, pickText, StatusTag } from '@/modules/common'
import { useAuthStore } from '@/features/auth/authStore'

export function SettingsPage() {
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const { user, persistFromResponse } = useAuthStore()
  const profile = useQuery({ queryKey: ['auth', 'me'], queryFn: () => fetchCurrentUserApi().then((response) => response.data) })
  const devices = useQuery({ queryKey: ['auth', 'devices'], queryFn: () => fetchDevicesApi().then((response) => response.data) })
  const loginLogs = useQuery({ queryKey: ['auth', 'login-logs'], queryFn: () => fetchLoginLogsApi({ pageNum: 1, pageSize: 20 }).then((response) => response.data) })
  const twoFactor = useQuery({ queryKey: ['auth', '2fa'], queryFn: () => fetchTwoFactorStatusApi().then((response) => response.data) })
  const providers = useQuery({ queryKey: ['settings', 'providers'], queryFn: () => fetchProviderConfigsApi().then((response) => response.data) })
  const currentUser = profile.data || user

  const invalidateSecurity = () => {
    queryClient.invalidateQueries({ queryKey: ['auth'] })
    queryClient.invalidateQueries({ queryKey: ['settings'] })
  }

  const avatarUpload: UploadProps = {
    showUploadList: false,
    beforeUpload: async (file) => {
      try {
        const response = await uploadAvatarApi(file)
        persistFromResponse({ token: useAuthStore.getState().token || '', userInfo: { ...(currentUser as any), avatarUrl: response.data, avatar: response.data } as any })
        message.success('头像已更新')
        invalidateSecurity()
      } catch (error) {
        message.error(getErrorMessage(error, '头像上传失败'))
      }
      return Upload.LIST_IGNORE
    }
  }

  const sendEmail = useMutation({
    mutationFn: sendEmailVerificationCodeApi,
    onSuccess: () => message.success('验证码已发送到当前邮箱'),
    onError: (error) => message.error(getErrorMessage(error, '发送验证码失败'))
  })
  const verifyEmail = useMutation({
    mutationFn: (code: string) => verifyEmailCodeApi(code),
    onSuccess: () => {
      message.success('邮箱已验证')
      invalidateSecurity()
    },
    onError: (error) => message.error(getErrorMessage(error, '邮箱验证失败'))
  })
  const setup2fa = useMutation({
    mutationFn: setupTwoFactorApi,
    onError: (error) => message.error(getErrorMessage(error, '无法初始化两步验证'))
  })
  const enable2fa = useMutation({
    mutationFn: (code: string) => enableTwoFactorApi(code),
    onSuccess: () => {
      message.success('两步验证已启用')
      queryClient.invalidateQueries({ queryKey: ['auth', '2fa'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '启用两步验证失败'))
  })
  const disable2fa = useMutation({
    mutationFn: (code: string) => disableTwoFactorApi(code),
    onSuccess: () => {
      message.success('两步验证已关闭')
      queryClient.invalidateQueries({ queryKey: ['auth', '2fa'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '关闭两步验证失败'))
  })
  const revokeDevice = useMutation({
    mutationFn: (id: number) => revokeDeviceApi(id),
    onSuccess: () => {
      message.success('设备已撤销')
      queryClient.invalidateQueries({ queryKey: ['auth', 'devices'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '撤销设备失败'))
  })
  const revokeAll = useMutation({
    mutationFn: revokeAllDevicesApi,
    onSuccess: () => {
      message.success('其他设备已撤销')
      queryClient.invalidateQueries({ queryKey: ['auth', 'devices'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '撤销设备失败'))
  })
  const checkProviders = useMutation({
    mutationFn: checkProviderConfigsApi,
    onSuccess: () => {
      message.success('Provider 状态已刷新')
      queryClient.invalidateQueries({ queryKey: ['settings', 'providers'] })
    },
    onError: (error) => message.error(getErrorMessage(error, 'Provider 检查失败'))
  })
  const saveProviders = useMutation({
    mutationFn: () => updateProviderConfigsApi(normalizeRecords(providers.data)),
    onSuccess: () => {
      message.success('Provider 配置已保存')
      queryClient.invalidateQueries({ queryKey: ['settings', 'providers'] })
    },
    onError: (error) => message.error(getErrorMessage(error, '保存 Provider 失败'))
  })
  const exportData = useMutation({
    mutationFn: exportMyDataApi,
    onSuccess: (response) => {
      downloadBlob(response.data, 'offerpilot-my-data.xlsx')
      message.success('个人数据已导出')
    },
    onError: (error) => message.error(getErrorMessage(error, '导出失败'))
  })

  const emailVerified = Boolean((currentUser as any)?.emailVerified)
  const providerRows = normalizeRecords(providers.data)

  return (
    <ModulePage
      title="设置"
      description="账号资料、安全认证、设备、Provider readiness 和个人数据导出。"
      metrics={[
        { label: '邮箱状态', value: emailVerified ? '已验证' : '待验证', hint: pickText(currentUser as any, ['email'], '未绑定邮箱') },
        { label: '两步验证', value: (twoFactor.data as any)?.enabled ? '已启用' : '未启用', hint: `恢复码 ${(twoFactor.data as any)?.recoveryCodesRemaining ?? '-'}` },
        { label: '登录设备', value: normalizeRecords(devices.data).length, hint: '当前账号设备' },
        { label: 'Provider', value: providerRows.length, hint: 'AI 与外部服务配置' }
      ]}
    >
      <Card className="surface-card">
        <Tabs
          items={[
            {
              key: 'profile',
              label: '账号资料',
              children: (
                <div className="workspace-grid two">
                  <Card title="当前账号" className="surface-card">
                    <Space align="start" size={16}>
                      <Avatar size={72} src={(currentUser as any)?.avatarUrl || (currentUser as any)?.avatar}>{pickText(currentUser as any, ['nickname', 'username'], 'U').slice(0, 1)}</Avatar>
                      <Space direction="vertical" size={8}>
                        <Typography.Title level={4} style={{ margin: 0 }}>{pickText(currentUser as any, ['nickname', 'username'], 'OfferPilot 用户')}</Typography.Title>
                        <Space wrap>
                          <Tag color={(currentUser as any)?.role === 'ADMIN' ? 'purple' : 'blue'}>{pickText(currentUser as any, ['role'], 'USER')}</Tag>
                          <StatusTag value={String((currentUser as any)?.status ?? 'ACTIVE')} />
                          {emailVerified && <Tag color="green" icon={<CheckCircleOutlined />}>邮箱已验证</Tag>}
                        </Space>
                        <Upload {...avatarUpload}><Button icon={<UploadOutlined />}>上传头像</Button></Upload>
                      </Space>
                    </Space>
                  </Card>
                  <Card title="账号信息" className="surface-card">
                    <EntitySummary record={currentUser as any} fields={[
                      { label: '用户名', keys: ['username'] },
                      { label: '邮箱', keys: ['email'] },
                      { label: '创建时间', keys: ['createTime'] },
                      { label: '上次登录', keys: ['lastLoginTime'] }
                    ]} />
                  </Card>
                </div>
              )
            },
            {
              key: 'security',
              label: '安全',
              children: (
                <div className="workspace-grid two">
                  <Card title="邮箱验证" className="surface-card">
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <Typography.Paragraph className="muted-text">用于找回密码、安全通知和高风险操作确认。</Typography.Paragraph>
                      <Button icon={<MailOutlined />} loading={sendEmail.isPending} onClick={() => sendEmail.mutate()} disabled={!currentUser?.email || emailVerified}>发送验证码</Button>
                      <Form layout="inline" onFinish={({ code }) => verifyEmail.mutate(code)}>
                        <Form.Item name="code" rules={[{ required: true, message: '请输入邮箱验证码' }]}><Input placeholder="邮箱验证码" /></Form.Item>
                        <Button type="primary" htmlType="submit" loading={verifyEmail.isPending} disabled={emailVerified}>验证邮箱</Button>
                      </Form>
                    </Space>
                  </Card>
                  <Card title="两步验证" className="surface-card">
                    <Space direction="vertical" style={{ width: '100%' }}>
                      <Space><Switch checked={Boolean((twoFactor.data as any)?.enabled)} disabled /><span>{(twoFactor.data as any)?.enabled ? '已启用' : '未启用'}</span></Space>
                      {setup2fa.data?.data?.otpauthUri && <QRCode value={setup2fa.data.data.otpauthUri} />}
                      {setup2fa.data?.data?.secret && <Typography.Text copyable code>{setup2fa.data.data.secret}</Typography.Text>}
                      <Space wrap>
                        <Button icon={<SafetyOutlined />} loading={setup2fa.isPending} onClick={() => setup2fa.mutate()} disabled={!!(twoFactor.data as any)?.enabled}>初始化 2FA</Button>
                        <Form layout="inline" onFinish={({ code }) => enable2fa.mutate(code)}>
                          <Form.Item name="code" rules={[{ required: true }]}><Input placeholder="6 位验证码" maxLength={6} /></Form.Item>
                          <Button type="primary" htmlType="submit" loading={enable2fa.isPending} disabled={!!(twoFactor.data as any)?.enabled}>启用</Button>
                        </Form>
                      </Space>
                      <Form layout="inline" onFinish={({ code }) => disable2fa.mutate(code)}>
                        <Form.Item name="code" rules={[{ required: true }]}><Input placeholder="当前验证码" maxLength={6} /></Form.Item>
                        <Button danger htmlType="submit" loading={disable2fa.isPending} disabled={!(twoFactor.data as any)?.enabled}>关闭两步验证</Button>
                      </Form>
                    </Space>
                  </Card>
                </div>
              )
            },
            {
              key: 'devices',
              label: '设备',
              children: (
                <DataTableCard
                  title="登录设备"
                  data={devices.data}
                  loading={devices.isLoading}
                  error={devices.error}
                  onRetry={() => devices.refetch()}
                  actions={<Popconfirm title="撤销其他设备" description="其他设备将需要重新登录。" onConfirm={() => revokeAll.mutate()}><Button danger loading={revokeAll.isPending}>撤销其他设备</Button></Popconfirm>}
                  columns={[
                    { title: '设备', render: (_, row) => <Space><DesktopOutlined /><span>{pickText(row, ['deviceName'], '未知设备')}</span>{row.current && <Tag color="green">当前</Tag>}</Space> },
                    { title: 'IP / 城市', render: (_, row) => `${pickText(row, ['ip'])} / ${pickText(row, ['city'])}` },
                    { title: '最后活跃', render: (_, row) => formatDateTime(row.lastActiveTime) },
                    { title: '操作', render: (_, row) => row.current ? <Tag>当前设备</Tag> : <Button danger size="small" loading={revokeDevice.isPending} onClick={() => revokeDevice.mutate(Number(row.id))}>撤销</Button> }
                  ]}
                />
              )
            },
            {
              key: 'login-history',
              label: '登录历史',
              children: (
                <DataTableCard
                  title="登录记录"
                  data={loginLogs.data}
                  loading={loginLogs.isLoading}
                  error={loginLogs.error}
                  onRetry={() => loginLogs.refetch()}
                  columns={[
                    { title: '时间', render: (_, row) => formatDateTime(row.createTime) },
                    { title: 'IP', dataIndex: 'ip' },
                    { title: '城市', render: (_, row) => pickText(row, ['city']) },
                    { title: '设备', render: (_, row) => pickText(row, ['device']) },
                    { title: '状态', render: (_, row) => Number(row.status) === 1 ? <Tag color="green">成功</Tag> : <Tag color="red">失败</Tag> },
                    { title: '备注', render: (_, row) => pickText(row, ['failReason'], '-') }
                  ]}
                />
              )
            },
            {
              key: 'providers',
              label: 'Provider',
              children: (
                <DataTableCard
                  title="Provider readiness"
                  data={providers.data}
                  loading={providers.isLoading}
                  error={providers.error}
                  onRetry={() => providers.refetch()}
                  actions={<Space><Button loading={checkProviders.isPending} onClick={() => checkProviders.mutate()}>重新检测</Button><Button type="primary" loading={saveProviders.isPending} onClick={() => saveProviders.mutate()}>保存当前配置</Button></Space>}
                  columns={[
                    { title: '范围', render: (_, row) => pickText(row, ['label', 'scope']) },
                    { title: '状态', render: (_, row) => <StatusTag value={pickText(row, ['status'])} /> },
                    { title: '服务商', render: (_, row) => pickText(row, ['providerName', 'provider']) },
                    { title: '模型', render: (_, row) => pickText(row, ['model']) },
                    { title: '最近检测', render: (_, row) => formatDateTime(row.lastCheckedAt) },
                    { title: '消息', render: (_, row) => pickText(row, ['lastCheckMessage', 'statusMessage']) }
                  ]}
                />
              )
            },
            {
              key: 'export',
              label: '数据导出',
              children: (
                <Card title="个人数据导出" className="surface-card">
                  <Space direction="vertical">
                    <Typography.Paragraph className="muted-text">导出面试记录、错题、复习数据和账号相关资料，便于本地留存或迁移。</Typography.Paragraph>
                    <Button type="primary" icon={<CloudDownloadOutlined />} loading={exportData.isPending} onClick={() => exportData.mutate()}>导出个人数据</Button>
                  </Space>
                </Card>
              )
            }
          ]}
        />
      </Card>
    </ModulePage>
  )
}
