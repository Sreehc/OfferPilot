import { describe, expect, it, vi } from 'vitest'
import { Button, Space } from 'antd'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router-dom'
import { StateView } from '@/components/feedback/StateView'
import { ModulePage } from '../ModulePage'
import { DataListCard, DataTableCard } from '../DataViews'
import { renderWithProviders } from '@/test/renderWithProviders'

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-search">{location.search}</div>
}

describe('common page state components', () => {
  it('renders an empty state with a business next action', () => {
    renderWithProviders(
      <StateView
        empty
        emptyTitle="暂无训练记录"
        emptyDescription="先开始一次模拟面试，系统会自动沉淀复盘记录。"
        emptyAction={<Button type="primary">开始面试</Button>}
      >
        <div>loaded content</div>
      </StateView>
    )

    expect(screen.queryByText('loaded content')).not.toBeInTheDocument()
    expect(screen.getByText('暂无训练记录')).toBeInTheDocument()
    expect(screen.getByText('先开始一次模拟面试，系统会自动沉淀复盘记录。')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '开始面试' })).toBeInTheDocument()
  })

  it('renders an error state with retry', async () => {
    const onRetry = vi.fn()
    renderWithProviders(
      <StateView error={new Error('服务暂时不可用')} onRetry={onRetry}>
        <div>loaded content</div>
      </StateView>
    )

    expect(screen.getByText('加载失败')).toBeInTheDocument()
    expect(screen.getByText('服务暂时不可用')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(onRetry).toHaveBeenCalledTimes(1)
  })

  it('renders a permission state without leaking page content', () => {
    renderWithProviders(
      <StateView permission="admin" permissionAction={<Button>返回工作台</Button>}>
        <div>管理员密钥</div>
      </StateView>
    )

    expect(screen.queryByText('管理员密钥')).not.toBeInTheDocument()
    expect(screen.getByText('无权限访问')).toBeInTheDocument()
    expect(screen.getByText('当前账号没有访问该内容的权限。')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '返回工作台' })).toBeInTheDocument()
  })

  it('lets ModulePage keep the page header while rendering shared states', () => {
    renderWithProviders(
      <ModulePage
        title="训练计划"
        description="统一页面标题、操作区和状态层。"
        actions={<Space><Button>新建计划</Button></Space>}
        state={{
          empty: true,
          emptyTitle: '还没有计划',
          emptyDescription: '创建一个计划后，这里会展示今天的训练动作。',
          emptyAction: <Button type="primary">创建计划</Button>
        }}
      >
        <div>计划列表</div>
      </ModulePage>
    )

    expect(screen.getByRole('heading', { name: '训练计划' })).toBeInTheDocument()
    expect(screen.getByText('统一页面标题、操作区和状态层。')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '新建计划' })).toBeInTheDocument()
    expect(screen.queryByText('计划列表')).not.toBeInTheDocument()
    expect(screen.getByText('还没有计划')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '创建计划' })).toBeInTheDocument()
  })

  it('reuses the shared state in list cards', () => {
    renderWithProviders(
      <DataListCard
        title="最近行动"
        data={[]}
        emptyTitle="暂无行动"
        emptyDescription="完成一次刷题或面试后会出现在这里。"
        emptyAction={<Button>去刷题</Button>}
        renderItem={(item) => <div>{item.title}</div>}
      />
    )

    expect(screen.getByText('最近行动')).toBeInTheDocument()
    expect(screen.getByText('暂无行动')).toBeInTheDocument()
    expect(screen.getByText('完成一次刷题或面试后会出现在这里。')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '去刷题' })).toBeInTheDocument()
  })

  it('syncs table pagination to URL and renders a mobile card alternative', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <>
        <LocationProbe />
        <DataTableCard
          title="训练记录"
          data={[
            { id: 1, title: 'Redis 缓存击穿', status: 'reviewing', owner: '小陈' },
            { id: 2, title: '线程池参数', status: 'done', owner: '小李' },
            { id: 3, title: 'JVM GC 日志', status: 'pending', owner: '小王' }
          ]}
          columns={[
            { title: '题目', dataIndex: 'title' },
            { title: '状态', dataIndex: 'status' },
            { title: '负责人', dataIndex: 'owner' }
          ]}
          urlStateKey="training"
          mobilePrimaryKey="title"
          mobileFieldKeys={['status', 'owner']}
          pageSize={2}
        />
      </>,
      { route: '/review?trainingPage=2' }
    )

    expect(screen.getAllByText('JVM GC 日志')).toHaveLength(2)
    expect(screen.queryByText('Redis 缓存击穿')).not.toBeInTheDocument()

    await user.click(screen.getByTitle('上一页'))

    expect(screen.getByTestId('location-search')).toHaveTextContent('trainingPage=1')
    expect(screen.getAllByText('Redis 缓存击穿')).toHaveLength(2)

    const mobileRegion = screen.getByRole('region', { name: '训练记录移动端卡片视图' })
    const cards = within(mobileRegion).getAllByRole('article')
    expect(cards).toHaveLength(2)
    expect(within(cards[0]).getByText('Redis 缓存击穿')).toBeInTheDocument()
    expect(within(cards[0]).getByText('状态')).toBeInTheDocument()
    expect(within(cards[0]).getByText('reviewing')).toBeInTheDocument()
    expect(within(cards[0]).getByText('负责人')).toBeInTheDocument()
    expect(within(cards[0]).getByText('小陈')).toBeInTheDocument()
  })
})
