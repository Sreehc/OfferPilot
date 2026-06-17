import { CheckOutlined, InboxOutlined } from '@ant-design/icons'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { App as AntApp, Button, Drawer, Empty, List, Space, Tag, Typography } from 'antd'
import { fetchNotificationsApi, markAllNotificationsReadApi, markNotificationsReadApi } from '@/api/modules/notification'
import { getErrorMessage } from '@/api/client'
import { normalizeRecords, pickText } from '@/modules/common/data'

export function NotificationCenter({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient()
  const { message } = AntApp.useApp()
  const notifications = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => fetchNotificationsApi(1, 20).then((response) => response.data),
    enabled: open
  })
  const items = normalizeRecords(notifications.data)
  const refreshUnread = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })
  const markOne = useMutation({
    mutationFn: (id: number) => markNotificationsReadApi([id]),
    onSuccess: refreshUnread,
    onError: (error) => message.error(getErrorMessage(error, '标记通知失败'))
  })
  const markAll = useMutation({
    mutationFn: markAllNotificationsReadApi,
    onSuccess: () => {
      message.success('已清空未读通知')
      refreshUnread()
    },
    onError: (error) => message.error(getErrorMessage(error, '清空通知失败'))
  })

  return (
    <Drawer
      title="通知中心"
      open={open}
      onClose={onClose}
      width={420}
      extra={<Button icon={<CheckOutlined />} loading={markAll.isPending} onClick={() => markAll.mutate()}>全部已读</Button>}
    >
      <List
        loading={notifications.isLoading}
        dataSource={items}
        locale={{ emptyText: <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无通知" /> }}
        renderItem={(item) => {
          const id = Number(item.id || item.notificationId || 0)
          const unread = item.read === false || item.isRead === false || item.status === 'UNREAD'
          return (
            <List.Item
              actions={[
                unread && id ? <Button key="read" size="small" type="link" loading={markOne.isPending} onClick={() => markOne.mutate(id)}>已读</Button> : null
              ].filter(Boolean)}
            >
              <List.Item.Meta
                avatar={<InboxOutlined style={{ color: unread ? 'var(--op-primary)' : 'var(--op-text-tertiary)' }} />}
                title={<Space><span>{pickText(item, ['title', 'subject', 'type'], '系统通知')}</span>{unread && <Tag color="blue">未读</Tag>}</Space>}
                description={<Typography.Paragraph style={{ marginBottom: 0 }}>{pickText(item, ['content', 'message', 'description'], '没有更多内容')}</Typography.Paragraph>}
              />
            </List.Item>
          )
        }}
      />
    </Drawer>
  )
}
