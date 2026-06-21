import type { ReactNode } from 'react'
import { Card, Empty, List, Space, Table, Tag, Typography, type TableProps } from 'antd'
import type { ColumnType } from 'antd/es/table'
import { useSearchParams } from 'react-router-dom'
import type { AnyRecord } from '@/api/types'
import { getRecordId, normalizeRecords, pickText } from './data'
import { labelOf } from './labels'
import { StateView } from '@/components/feedback/StateView'

function normalizePage(value: string | null, fallback = 1) {
  const page = Number(value)
  return Number.isFinite(page) && page > 0 ? Math.floor(page) : fallback
}

function pageParamName(key?: string) {
  return key ? `${key}Page` : undefined
}

function getValueByDataIndex(record: AnyRecord, dataIndex: unknown) {
  if (Array.isArray(dataIndex)) {
    return dataIndex.reduce<unknown>((current, key) => {
      if (!current || typeof current !== 'object') return undefined
      return (current as AnyRecord)[String(key)]
    }, record)
  }
  if (typeof dataIndex === 'string' || typeof dataIndex === 'number') return record[String(dataIndex)]
  return undefined
}

function isColumnType<T extends AnyRecord>(column: NonNullable<TableProps<T>['columns']>[number]): column is ColumnType<T> {
  return !('children' in column)
}

function renderColumnValue<T extends AnyRecord>(column: ColumnType<T>, record: T, index: number): ReactNode {
  const value = getValueByDataIndex(record, column.dataIndex)
  if (typeof column.render === 'function') {
    const rendered = column.render(value, record, index)
    if (rendered && typeof rendered === 'object' && !Array.isArray(rendered) && 'children' in rendered) {
      return rendered.children as ReactNode
    }
    return rendered as ReactNode
  }
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

function columnTitleText<T extends AnyRecord>(column: ColumnType<T>, fallback: string) {
  if (typeof column.title === 'string' || typeof column.title === 'number') return String(column.title)
  return fallback
}

function findColumnByKey<T extends AnyRecord>(columns: NonNullable<TableProps<T>['columns']>, key: string) {
  return columns.filter(isColumnType).find((column) => {
    if (String(column.key ?? '') === key) return true
    const dataIndex = column.dataIndex
    if (Array.isArray(dataIndex)) return dataIndex.map(String).join('.') === key
    return String(dataIndex ?? '') === key
  })
}

export function DataListCard({
  title,
  data,
  loading,
  error,
  onRetry,
  emptyTitle = '暂无数据',
  emptyDescription,
  emptyAction,
  renderItem,
  actions
}: {
  title: string
  data: unknown
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  emptyTitle?: string
  emptyDescription?: string
  emptyAction?: ReactNode
  renderItem: (item: AnyRecord) => ReactNode
  actions?: ReactNode
}) {
  const rows = normalizeRecords(data)
  return (
    <Card title={title} extra={actions} className="surface-card">
      <StateView
        loading={loading}
        error={error}
        empty={!loading && !error && rows.length === 0}
        emptyTitle={emptyTitle}
        emptyDescription={emptyDescription}
        emptyAction={emptyAction}
        onRetry={onRetry}
      >
        <List dataSource={rows} renderItem={(item) => <List.Item key={getRecordId(item)}>{renderItem(item)}</List.Item>} />
      </StateView>
    </Card>
  )
}

export function DataTableCard<T extends AnyRecord>({
  title,
  data,
  loading,
  error,
  onRetry,
  columns,
  actions,
  emptyTitle = '暂无数据',
  emptyDescription,
  emptyAction,
  rowSelection,
  urlStateKey,
  pageSize = 8,
  mobilePrimaryKey,
  mobileFieldKeys,
  renderMobileItem
}: {
  title: string
  data: unknown
  loading?: boolean
  error?: unknown
  onRetry?: () => void
  columns: TableProps<T>['columns']
  actions?: ReactNode
  emptyTitle?: string
  emptyDescription?: string
  emptyAction?: ReactNode
  rowSelection?: TableProps<T>['rowSelection']
  urlStateKey?: string
  pageSize?: number
  mobilePrimaryKey?: string
  mobileFieldKeys?: string[]
  renderMobileItem?: (item: T, index: number) => ReactNode
}) {
  const [searchParams, setSearchParams] = useSearchParams()
  const rows = normalizeRecords<T>(data)
  const tableColumns = columns ?? []
  const pageKey = pageParamName(urlStateKey)
  const currentPage = pageKey ? normalizePage(searchParams.get(pageKey)) : 1
  const normalizedPageSize = Math.max(1, pageSize)
  const pagedRows = rows.slice((currentPage - 1) * normalizedPageSize, currentPage * normalizedPageSize)
  const leafColumns = tableColumns.filter(isColumnType)
  const primaryColumn = mobilePrimaryKey ? findColumnByKey(tableColumns, mobilePrimaryKey) : leafColumns[0]
  const fieldColumns = mobileFieldKeys?.length
    ? mobileFieldKeys.map((key) => findColumnByKey(tableColumns, key)).filter(Boolean)
    : leafColumns.filter((column) => column !== primaryColumn).slice(0, 4)
  const updatePage = (page: number) => {
    if (!pageKey) return
    setSearchParams((params) => {
      const next = new URLSearchParams(params)
      next.set(pageKey, String(page))
      return next
    }, { replace: true })
  }

  return (
    <Card title={title} extra={actions} className="surface-card">
      <StateView
        loading={loading}
        error={error}
        empty={!loading && !error && rows.length === 0}
        emptyTitle={emptyTitle}
        emptyDescription={emptyDescription}
        emptyAction={emptyAction}
        onRetry={onRetry}
      >
        <div className="data-table-responsive">
          <div className="data-table-desktop">
            <Table<T>
              rowKey={(row) => getRecordId(row)}
              columns={tableColumns}
              dataSource={rows}
              pagination={{
                current: pageKey ? currentPage : undefined,
                pageSize: normalizedPageSize,
                showSizeChanger: false,
                onChange: updatePage
              }}
              rowSelection={rowSelection}
              size="middle"
              scroll={{ x: 'max-content' }}
            />
          </div>
          <section className="data-table-mobile" role="region" aria-label={`${title}移动端卡片视图`}>
            <div className="data-table-mobile-list">
              {pagedRows.map((row, index) => (
                <article className="data-table-mobile-card" key={getRecordId(row)}>
                  {renderMobileItem ? renderMobileItem(row, index) : (
                    <>
                      <Typography.Text strong className="data-table-mobile-title">
                        {primaryColumn ? renderColumnValue(primaryColumn, row, index) : pickText(row, ['title', 'name', 'id'])}
                      </Typography.Text>
                      <div className="data-table-mobile-fields">
                        {fieldColumns.map((column, fieldIndex) => column ? (
                          <div className="data-table-mobile-field" key={String(column.key ?? column.dataIndex ?? fieldIndex)}>
                            <Typography.Text type="secondary">{columnTitleText(column, `字段 ${fieldIndex + 1}`)}</Typography.Text>
                            <div>{renderColumnValue(column, row, index)}</div>
                          </div>
                        ) : null)}
                      </div>
                    </>
                  )}
                </article>
              ))}
            </div>
          </section>
        </div>
      </StateView>
    </Card>
  )
}

export function EntitySummary({ record, fields }: { record?: AnyRecord; fields: Array<{ label: string; keys: string[]; tag?: boolean }> }) {
  if (!record) return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无详情" />
  return (
    <div className="entity-summary">
      {fields.map((field) => {
        const value = pickText(record, field.keys)
        return (
          <div className="entity-summary-item" key={field.label}>
            <Typography.Text type="secondary">{field.label}</Typography.Text>
            <div>{field.tag ? <Tag color="blue">{value}</Tag> : <Typography.Text strong>{value}</Typography.Text>}</div>
          </div>
        )
      })}
    </div>
  )
}

export function StatusTag({ value }: { value?: string }) {
  const text = value || 'UNKNOWN'
  const normalized = text.toUpperCase()
  const color = normalized.includes('FAIL') || normalized.includes('REJECT') || normalized.includes('BAN') ? 'red' : normalized.includes('PENDING') || normalized.includes('WAIT') ? 'gold' : normalized.includes('RUN') || normalized.includes('PROCESS') ? 'blue' : normalized.includes('DONE') || normalized.includes('SUCCESS') || normalized.includes('ACTIVE') ? 'green' : 'default'
  return <Tag color={color}>{labelOf(text)}</Tag>
}

export function InlineActions({ children }: { children: ReactNode }) {
  return <Space size={6} wrap>{children}</Space>
}
