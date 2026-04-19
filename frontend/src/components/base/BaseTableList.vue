<script setup lang="ts">
import { computed, h, reactive, watch } from 'vue'
import type { TableColumnType, TableProps } from 'ant-design-vue'

interface PaginationState {
  current: number
  pageSize: number
  total: number
}

const props = withDefaults(
  defineProps<{
    columns: TableColumnType[]
    dataSource: Record<string, unknown>[]
    loading?: boolean
    firstColumnKey?: string
    rowKey?: string
    total?: number
    hideDefaultActions?: boolean
    showQuery?: boolean
    showReset?: boolean
    showRefresh?: boolean
    pagination?: {
      current: number
      pageSize: number
      total: number
      showSizeChanger?: boolean
      showTotal?: (total: number) => string
    }
    rowSelection?: TableProps['rowSelection']
  }>(),
  {
    loading: false,
    firstColumnKey: 'name',
    rowKey: 'id',
    total: 0,
    hideDefaultActions: false,
    showQuery: true,
    showReset: true,
    showRefresh: true,
  },
)

const emit = defineEmits<{
  (e: 'query', payload: { page: number; pageSize: number }): void
  (e: 'reset'): void
  (e: 'refresh', payload: { page: number; pageSize: number }): void
  (e: 'first-column-click', record: Record<string, unknown>): void
  (e: 'change', payload: { current: number; pageSize: number }): void
}>()

const pagination = reactive<PaginationState>({
  current: props.pagination?.current || 1,
  pageSize: props.pagination?.pageSize || 10,
  total: props.pagination?.total || props.total,
})

watch(
  () => props.pagination,
  (newPagination) => {
    if (newPagination) {
      pagination.current = newPagination.current
      pagination.pageSize = newPagination.pageSize
      pagination.total = newPagination.total
    }
  },
  { deep: true },
)

watch(
  () => props.total,
  (newTotal) => {
    pagination.total = newTotal
  },
)

const tableColumns = computed(() =>
  props.columns.map((column, index) => {
    if (index !== 0) {
      return column
    }
    const key = (column.dataIndex as string) || props.firstColumnKey
    return {
      ...column,
      customRender: ({ record }: { record: Record<string, unknown> }) => {
        const value = record[key]
        return h(
          'a',
          {
            class: 'base-table-link',
            onClick: () => emit('first-column-click', record),
          },
          String(value ?? ''),
        )
      },
    }
  }),
)

const handleSearch = () => {
  pagination.current = 1
  emit('query', { page: pagination.current, pageSize: pagination.pageSize })
}

const handleReset = () => {
  pagination.current = 1
  emit('reset')
}

const handleRefresh = () => {
  emit('refresh', { page: pagination.current, pageSize: pagination.pageSize })
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  emit('query', { page, pageSize })
}

const handleChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  emit('query', { page: pag.current, pageSize: pag.pageSize })
}

const query = handleSearch
const reset = handleReset
const refresh = handleRefresh

defineExpose({
  query,
  reset,
  refresh,
})
</script>

<template>
  <a-card :bordered="false">
    <div class="query-row">
      <div class="query-left">
        <slot name="filters" />
      </div>
      <div class="query-right">
        <slot name="batch-actions" />
        <slot name="actions" />
        <template v-if="!hideDefaultActions">
          <a-button v-if="showQuery" @click="handleSearch">查询</a-button>
          <a-button v-if="showReset" @click="handleReset">重置</a-button>
          <a-button v-if="showRefresh" @click="handleRefresh">刷新</a-button>
        </template>
      </div>
    </div>

    <a-table
      :columns="tableColumns"
      :data-source="dataSource"
      :loading="loading"
      :row-key="rowKey"
      :pagination="false"
      :row-selection="rowSelection"
      size="middle"
      @change="handleChange"
    >
      <template v-for="(_, name) in $slots" #[name]="slotProps">
        <slot :name="name" v-bind="slotProps || {}" />
      </template>
    </a-table>

    <div style="display: flex; justify-content: center; margin-top: 12px">
      <a-pagination
        v-if="pagination.total > 0"
        :current="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        show-size-changer
        show-quick-jumper
        :page-size-options="['10', '20', '50']"
        @change="handlePageChange"
      />
    </div>
  </a-card>
</template>

<style scoped>
.query-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.query-left {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.query-right {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

:deep(.base-table-link) {
  color: #1890ff;
  cursor: pointer;
}

:deep(.base-table-link:hover) {
  text-decoration: underline;
}
</style>
