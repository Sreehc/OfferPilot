import { AppstoreOutlined, BellOutlined, LogoutOutlined, MenuOutlined, MoonOutlined, SearchOutlined, SunOutlined, UserOutlined } from '@ant-design/icons'
import { Avatar, Badge, Button, Drawer, Dropdown, Input, Layout, Menu, Modal, Space } from 'antd'
import { useMemo, useState } from 'react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { fetchUnreadCountApi } from '@/api/modules/notification'
import { useQuery } from '@tanstack/react-query'
import { NotificationCenter } from '@/components/feedback/NotificationCenter'
import { OfflineBanner } from '@/components/feedback/OfflineBanner'
import { useAuthStore } from '@/features/auth/authStore'
import { useThemeStore } from '@/features/theme/themeStore'
import { mobileNavPaths, navGroups, navItems } from './navigation'

const { Sider, Content } = Layout

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
  const filtered = visibleNav.filter((item) => item.label.toLowerCase().includes(query.toLowerCase()) || item.path.includes(query))
  const menuItems = useMemo(() => navGroups
    .map((group) => ({
      key: group.key,
      type: 'group' as const,
      label: group.label,
      children: group.items.filter((item) => !item.adminOnly || isAdmin).map((item) => ({ key: item.path, icon: item.icon, label: <Link to={item.path}>{item.label}</Link> }))
    }))
    .filter((group) => group.children.length > 0), [isAdmin])

  const mobileItems = mobileNavPaths
    .map((path) => visibleNav.find((item) => item.path === path))
    .filter((item): item is (typeof visibleNav)[number] => Boolean(item))

  const side = (
    <>
      <div style={{ height: 64, display: 'flex', alignItems: 'center', gap: 10, padding: '0 18px' }}>
        <span className="app-logo-mark">OP</span>
        <div>
          <strong>OfferPilot</strong>
          <div className="muted-text" style={{ fontSize: 12 }}>职业训练平台</div>
        </div>
      </div>
      <Menu mode="inline" selectedKeys={[selected]} items={menuItems} style={{ borderInlineEnd: 0 }} />
    </>
  )

  return (
    <Layout className="app-shell">
      <a href="#main-content" className="skip-link">跳到主要内容</a>
      <Sider className="shell-side" width={248} breakpoint="lg" collapsedWidth={0} trigger={null}>{side}</Sider>
      <Layout>
        <header className="shell-header">
          <Space>
            <Button icon={<MenuOutlined />} onClick={() => setDrawerOpen(true)} className="lg-menu" />
            <Button icon={<SearchOutlined />} onClick={() => setSearchOpen(true)}>搜索页面</Button>
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
      <Drawer open={drawerOpen} onClose={() => setDrawerOpen(false)} placement="left" width={280}>{side}</Drawer>
      <nav className="mobile-nav" aria-label="主导航">
        {mobileItems.map((item) => (
          <button key={item.path} type="button" className={'mobile-nav-item' + (selected === item.path ? ' active' : '')} onClick={() => navigate(item.path)}>
            <span className="mobile-nav-icon">{item.icon}</span>
            <span className="mobile-nav-label">{item.label}</span>
          </button>
        ))}
        <button type="button" className="mobile-nav-item" onClick={() => setMoreOpen(true)}>
          <span className="mobile-nav-icon"><AppstoreOutlined /></span>
          <span className="mobile-nav-label">更多</span>
        </button>
      </nav>
      <Drawer open={moreOpen} onClose={() => setMoreOpen(false)} placement="bottom" height="auto" title="全部功能">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
          {visibleNav.map((item) => (
            <button key={item.path} type="button" className={'mobile-nav-item' + (selected === item.path ? ' active' : '')} onClick={() => { setMoreOpen(false); navigate(item.path) }}>
              <span className="mobile-nav-icon">{item.icon}</span>
              <span className="mobile-nav-label">{item.label}</span>
            </button>
          ))}
        </div>
      </Drawer>
      <Modal open={searchOpen} title="搜索页面" footer={null} onCancel={() => setSearchOpen(false)}>
        <Input autoFocus prefix={<SearchOutlined />} value={query} onChange={(event) => setQuery(event.target.value)} placeholder="输入页面名称" />
        <div style={{ marginTop: 12, display: 'grid', gap: 6 }}>{filtered.map((item) => <Button key={item.path} block style={{ textAlign: 'left' }} onClick={() => { setSearchOpen(false); navigate(item.path) }}>{item.label}<span className="muted-text" style={{ float: 'right' }}>{item.path}</span></Button>)}</div>
      </Modal>
      <NotificationCenter open={notificationsOpen} onClose={() => setNotificationsOpen(false)} />
    </Layout>
  )
}
