import { AppstoreOutlined, BellOutlined, LogoutOutlined, MenuOutlined, MoonOutlined, SearchOutlined, SunOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Badge, Button, Drawer, Dropdown, Empty, Input, Layout, Menu, Modal, Space, Typography } from 'antd'
import { useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { fetchUnreadCountApi } from '@/api/modules/notification'
import { useQuery } from '@tanstack/react-query'
import { NotificationCenter } from '@/components/feedback/NotificationCenter'
import { OfflineBanner } from '@/components/feedback/OfflineBanner'
import { useAuthStore } from '@/features/auth/authStore'
import { useThemeStore } from '@/features/theme/themeStore'
import { BrandGlyph } from '@/components/brand/BrandGlyph'
import { mobileNavPaths, navGroups, navItems } from './navigation'

const { Sider, Content } = Layout

const commandActions = [
  { key: 'start-interview', label: '开始面试', description: '进入面试模式启动器', path: '/interview' },
  { key: 'upload-resume', label: '上传简历', description: '进入简历工作台管理版本', path: '/resume' },
  { key: 'create-agent-run', label: '创建 Agent 任务', description: '进入 Agent 工作台创建运行', path: '/agent' },
  { key: 'ask-question', label: '发起提问', description: '进入 AI 问答继续追问', path: '/chat' }
]

export function AppShell() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const { mode, toggleTheme } = useThemeStore()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)
  const [moreOpen, setMoreOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [query, setQuery] = useState('')
  const { data: unread } = useQuery({ queryKey: ['notifications', 'unread'], queryFn: () => fetchUnreadCountApi().then((r) => r.data), enabled: Boolean(user), staleTime: 60_000 })

  const isAdmin = user?.role === 'ADMIN'
  const visibleNav = useMemo(() => navItems.filter((item) => !item.adminOnly || isAdmin), [isAdmin])
  const selected = '/' + (pathname.split('/')[1] || 'dashboard')
  const normalizedQuery = query.trim().toLowerCase()
  const filtered = visibleNav.filter((item) => item.label.toLowerCase().includes(normalizedQuery) || item.path.includes(normalizedQuery))
  const filteredActions = commandActions.filter((item) => item.label.toLowerCase().includes(normalizedQuery) || item.description.toLowerCase().includes(normalizedQuery) || item.path.includes(normalizedQuery))
  const runCommand = (path: string) => {
    setSearchOpen(false)
    setQuery('')
    navigate(path)
  }
  const menuItems = useMemo(() => navGroups
    .map((group) => ({
      key: group.key,
      type: 'group' as const,
      label: group.label,
      children: group.items.filter((item) => !item.adminOnly || isAdmin).map((item) => ({
        key: item.path,
        icon: item.icon,
        label: <Link to={item.path} aria-current={selected === item.path ? 'page' : undefined}>{item.label}</Link>
      }))
    }))
    .filter((group) => group.children.length > 0), [isAdmin, selected])

  const mobileItems = mobileNavPaths
    .map((path) => visibleNav.find((item) => item.path === path))
    .filter((item): item is (typeof visibleNav)[number] => Boolean(item))
  const mobileGroups = useMemo(() => navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.adminOnly || isAdmin)
    }))
    .filter((group) => group.items.length > 0), [isAdmin])

  const renderSide = (label: string) => (
    <nav aria-label={label}>
      <div style={{ height: 64, display: 'flex', alignItems: 'center', gap: 10, padding: '0 18px' }}>
        <BrandGlyph size={34} />
        <div>
          <strong>OfferPilot</strong>
          <div className="muted-text" style={{ fontSize: 12 }}>职业训练平台</div>
        </div>
      </div>
      <Menu mode="inline" selectedKeys={[selected]} items={menuItems} style={{ borderInlineEnd: 0 }} />
    </nav>
  )

  return (
    <Layout className="app-shell">
      <a href="#main-content" className="skip-link">跳到主要内容</a>
      <Sider className="shell-side" width={248} breakpoint="lg" collapsedWidth={0} trigger={null}>{renderSide('桌面主导航')}</Sider>
      <Layout>
        <header className="shell-header">
          <Space>
            <Button icon={<MenuOutlined />} onClick={() => setDrawerOpen(true)} className="lg-menu" />
            <Button icon={<SearchOutlined />} onClick={() => setSearchOpen(true)}>命令面板</Button>
          </Space>
          <Space>
            <Button aria-label="切换主题" icon={mode === 'dark' ? <SunOutlined /> : <MoonOutlined />} onClick={toggleTheme} />
            <Badge count={typeof unread === 'number' ? unread : (unread as any)?.count || 0} size="small">
              <Button aria-label="打开通知中心" icon={<BellOutlined />} onClick={() => setNotificationsOpen(true)} />
            </Badge>
            <Dropdown menu={{ items: [{ key: 'profile', label: <Link to="/settings"><UserOutlined /> 账号设置</Link> }, { key: 'logout', danger: true, label: '退出登录', icon: <LogoutOutlined />, onClick: logout }] }}>
              <Button type="text">
                <Space>
                  <Avatar src={user?.avatarUrl}>{(user?.nickname || user?.username || 'U').slice(0, 1).toUpperCase()}</Avatar>
                  <span>{user?.nickname || user?.username || '用户'}</span>
                </Space>
              </Button>
            </Dropdown>
          </Space>
        </header>
        <Content id="main-content" tabIndex={-1} className="shell-content"><OfflineBanner /><Outlet /></Content>
      </Layout>
      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} placement="left" size={280}>{renderSide('抽屉主导航')}</Drawer>
      <nav className="mobile-nav" aria-label="主导航">
        {mobileItems.map((item) => (
          <button key={item.path} type="button" className={'mobile-nav-item' + (selected === item.path ? ' active' : '')} aria-current={selected === item.path ? 'page' : undefined} onClick={() => navigate(item.path)}>
            <span className="mobile-nav-icon">{item.icon}</span>
            <span className="mobile-nav-label">{item.label}</span>
          </button>
        ))}
        <button type="button" className="mobile-nav-item" aria-expanded={moreOpen} onClick={() => setMoreOpen(true)}>
          <span className="mobile-nav-icon"><AppstoreOutlined /></span>
          <span className="mobile-nav-label">更多</span>
        </button>
      </nav>
      <Drawer open={moreOpen} onClose={() => setMoreOpen(false)} placement="bottom" size="auto" title="全部功能">
        <div className="mobile-more-panel">
          {mobileGroups.map((group) => (
            <section key={group.key} className="mobile-more-group" aria-labelledby={`mobile-more-${group.key}`}>
              <h3 id={`mobile-more-${group.key}`} className="mobile-more-title">{group.label}</h3>
              <div className="mobile-more-grid">
                {group.items.map((item) => (
                  <button key={item.path} type="button" className={'mobile-nav-item mobile-more-item' + (selected === item.path ? ' active' : '')} aria-current={selected === item.path ? 'page' : undefined} onClick={() => { setMoreOpen(false); navigate(item.path) }}>
                    <span className="mobile-nav-icon">{item.icon}</span>
                    <span className="mobile-nav-label">{item.label}</span>
                  </button>
                ))}
              </div>
            </section>
          ))}
        </div>
      </Drawer>
      <Modal open={searchOpen} title="命令面板" footer={null} width={640} onCancel={() => setSearchOpen(false)}>
        <div className="command-panel">
          <Input autoFocus prefix={<SearchOutlined />} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="搜索页面或输入动作关键词" />
          <section className="command-section" aria-labelledby="command-actions-title">
            <Typography.Text id="command-actions-title" className="command-section-title">关键动作</Typography.Text>
            {filteredActions.length > 0 ? (
              <div className="command-grid">
                {filteredActions.map((item) => (
                  <button key={item.key} type="button" className="command-item" onClick={() => runCommand(item.path)}>
                    <span className="command-item-title">{item.label}</span>
                    <span className="command-item-description">{item.description}</span>
                  </button>
                ))}
              </div>
            ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配动作" />}
          </section>
          <section className="command-section" aria-labelledby="command-pages-title">
            <Typography.Text id="command-pages-title" className="command-section-title">页面</Typography.Text>
            {filtered.length > 0 ? (
              <div className="command-list">
                {filtered.map((item) => (
                  <button key={item.path} type="button" className="command-item command-page-item" onClick={() => runCommand(item.path)}>
                    <span className="command-item-title">{item.label}</span>
                    <span className="command-item-description">{item.path}</span>
                  </button>
                ))}
              </div>
            ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="没有匹配页面" />}
          </section>
        </div>
      </Modal>
      <NotificationCenter open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
    </Layout>
  )
}
