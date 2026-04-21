<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { TableColumnType } from 'ant-design-vue'
import BaseTableList from '../../components/base/BaseTableList.vue'
import CreateTicketDialog from '../../components/ticket/CreateTicketDialog.vue'
import {
  getTicketList,
  getTicketStatistics,
  getTicketTypes,
} from '../../api/ticket'
import { getShopSelectOptionsApi } from '../../api/shop'
import type { TicketItem, TicketStatistics, TicketTypeItem } from '../../types/ticket'
import type { ShopItem } from '../../types/shop'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const statsLoading = ref(false)
const tableData = ref<TicketItem[]>([])
const createDialogOpen = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const shopList = ref<ShopItem[]>([])
const typeList = ref<TicketTypeItem[]>([])
const stats = ref<TicketStatistics | null>(null)

const isTenantAdmin = computed(() => authStore.isTenantAdmin)
const isShopOwner = computed(() => authStore.isShopOwner)

const filters = reactive({
  typeId: undefined as number | undefined,
  status: undefined as number | undefined,
  shopId: undefined as number | undefined,
  keyword: '',
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
})

const dateRange = ref<[string, string] | null>(null)
const onDateRangeChange = (dates: [string, string] | null) => {
  dateRange.value = dates
  if (dates) {
    filters.startDate = dates[0]
    filters.endDate = dates[1]
  } else {
    filters.startDate = undefined
    filters.endDate = undefined
  }
}

const baseColumns: TableColumnType[] = [
  { title: '工单标题', dataIndex: 'title', ellipsis: true, width: 240 },
  { title: '优先级', dataIndex: 'priorityText', width: 80 },
  { title: '状态', dataIndex: 'statusName', width: 110 },
  { title: '门店', dataIndex: 'shopName', width: 120 },
  { title: '工单类型', dataIndex: 'typeName', width: 100 },
  { title: '处理人', dataIndex: 'assigneeName', width: 90 },
  { title: '创建时间', dataIndex: 'createdAt', width: 160 },
]

// 店长端不显示门店列
const columns = computed(() => {
  if (isShopOwner.value) {
    return baseColumns.filter(col => col.dataIndex !== 'shopName')
  }
  return baseColumns
})

const statusOptions = computed(() => {
  if (isTenantAdmin.value) {
    return [
      { label: '处理中', value: 2 },
      { label: '待核销', value: 3 },
      { label: '已归档', value: 4 },
    ]
  }
  return [
    { label: '待处理', value: 1 },
    { label: '待核销', value: 3 },
    { label: '已归档', value: 4 },
  ]
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getTicketList({
      page: pagination.current,
      pageSize: pagination.pageSize,
      typeId: filters.typeId,
      status: filters.status,
      shopId: isTenantAdmin.value ? filters.shopId : undefined,
      keyword: filters.keyword || undefined,
      startDate: filters.startDate,
      endDate: filters.endDate,
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await getTicketStatistics({ viewAs: isTenantAdmin.value ? 'tenant' : 'shop' })
    stats.value = res
  } finally {
    statsLoading.value = false
  }
}

const loadTypes = async () => {
  try {
    const res = await getTicketTypes()
    typeList.value = res
  } catch {
    typeList.value = []
  }
}

const loadShops = async () => {
  try {
    const res = await getShopSelectOptionsApi()
    shopList.value = res
  } catch {
    shopList.value = []
  }
}

const onTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadData()
}

const onRowClick = (record: Record<string, unknown>) => {
  router.push(`/tickets/${(record as unknown as TicketItem).id}`)
}

const onSearch = () => {
  pagination.current = 1
  void loadData()
  void loadStats()
}

const onReset = () => {
  filters.typeId = undefined
  filters.status = undefined
  filters.shopId = undefined
  filters.keyword = ''
  filters.startDate = undefined
  filters.endDate = undefined
  pagination.current = 1
  void loadData()
  void loadStats()
}

const formatDateTime = (v: string | null | undefined) => {
  if (!v) return '-'
  return v.replace('T', ' ').substring(0, 19)
}

void onMounted(() => {
  void loadData()
  void loadStats()
  void loadTypes()
  if (isTenantAdmin.value) {
    void loadShops()
  }
})
</script>

<template>
  <div>
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card :loading="statsLoading" size="small">
          <a-statistic title="工单总数" :value="stats?.totalCount ?? 0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="statsLoading" size="small">
          <a-statistic :title="isShopOwner ? '待处理' : '处理中'" :value="stats?.processingCount ?? 0" :value-style="{ color: '#faad14' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="statsLoading" size="small">
          <a-statistic title="待核销" :value="stats?.pendingVerifyCount ?? 0" :value-style="{ color: '#1890ff' }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :loading="statsLoading" size="small">
          <a-statistic title="已归档" :value="stats?.archivedCount ?? 0" :value-style="{ color: '#52c41a' }" />
        </a-card>
      </a-col>
    </a-row>

    <BaseTableList
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :show-refresh="false"
      :pagination="{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total: number) => `共 ${total} 条`,
      }"
      @query="onSearch"
      @reset="onReset"
      @first-column-click="onRowClick"
      @change="onTableChange"
    >
      <template #filters>
        <a-input
          v-model:value="filters.keyword"
          placeholder="搜索工单标题"
          style="width: 180px"
          allow-clear
          @pressEnter="onSearch"
        />
        <a-select
          v-model:value="filters.typeId"
          allow-clear
          placeholder="工单类型"
          style="width: 120px"
        >
          <a-select-option v-for="t in typeList" :key="t.id" :value="t.id">
            {{ t.name }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="状态"
          style="width: 100px"
        >
          <a-select-option v-for="s in statusOptions" :key="s.value" :value="s.value">
            {{ s.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-if="isTenantAdmin"
          v-model:value="filters.shopId"
          allow-clear
          placeholder="选择门店"
          style="width: 150px"
          :options="shopList.map((s) => ({ label: s.shopName, value: s.id }))"
        />
        <a-range-picker
          :value="dateRange"
          value-format="YYYY-MM-DD"
          style="width: 220px"
          @change="onDateRangeChange"
        />
      </template>
      <template #actions>
        <a-button v-if="isTenantAdmin" type="primary" @click="createDialogOpen = true">
          新建工单
        </a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'title'">
          <a-tooltip :title="record.title">
            <span class="base-table-link">{{ record.title }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'priorityText'">
          <span :style="{ color: record.priorityColor }">
            {{ record.priorityText }}
          </span>
        </template>
        <template v-else-if="column.dataIndex === 'statusName'">
          <a-tag :color="record.status === 4 || record.status === 5 ? 'default' : record.status === 3 ? 'blue' : record.status === 2 ? 'orange' : 'red'">
            {{ isShopOwner && record.status === 2 ? '待处理' : record.statusName }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createdAt'">
          {{ formatDateTime(record.createdAt) }}
        </template>
      </template>
    </BaseTableList>

    <CreateTicketDialog
      v-model:open="createDialogOpen"
      @success="() => { loadData(); loadStats() }"
    />
  </div>
</template>
