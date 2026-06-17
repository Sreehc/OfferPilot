import { Button, Result } from 'antd'
import { Link } from 'react-router-dom'
export function NotFoundPage() { return <Result status="404" title="页面不存在" subTitle="当前地址没有对应的 OfferPilot 页面。" extra={<Link to="/dashboard"><Button type="primary">返回首页</Button></Link>} /> }
