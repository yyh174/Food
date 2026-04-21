<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnType } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import BaseEditDialog from '../../components/base/BaseEditDialog.vue'
import BaseTableList from '../../components/base/BaseTableList.vue'
import CreateTicketDialog from '../../components/ticket/CreateTicketDialog.vue'
import {
  getReviewList,
  getReplyTemplateList,
  replyReview,
  replyReviewWithTemplate,
} from '../../api/review'
import { getShopSelectOptionsApi } from '../../api/shop'
import type { ReviewItem, ReplyTemplateItem } from '../../types/review'
import type { ShopItem } from '../../types/shop'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const loading = ref(false)
const dialogOpen = ref(false)
const tableData = ref<ReviewItem[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const shopList = ref<ShopItem[]>([])
const templateList = ref<ReplyTemplateItem[]>([])
const selectedRow = ref<ReviewItem | null>(null)
const replyLoading = ref(false)
const createTicketOpen = ref(false)

const filters = reactive({
  platform: undefined as number | undefined,
  replyStatus: undefined as number | undefined,
  shopId: undefined as number | undefined,
  keyword: '',
  startDate: undefined as string | undefined,
  endDate: undefined as string | undefined,
})

const replyFormModel = reactive({
  content: '',
  templateId: undefined as number | undefined,
  useTemplate: false,
})

const replyFormRules: Record<string, Rule[]> = {
  content: [{ required: true, message: '请输入回复内容' }],
}

const columns: TableColumnType[] = [
  { title: '评价内容', dataIndex: 'content', ellipsis: true, width: 200 },
  { title: '星级', dataIndex: 'starRating', width: 80 },
  { title: '门店', dataIndex: 'shopName', width: 120 },
  { title: '平台', dataIndex: 'platform', width: 80 },
  { title: '回复状态', dataIndex: 'replyStatus', width: 100 },
  { title: '评价时间', dataIndex: 'createdAt', width: 160 },
]

const isShopOwner = computed(() => authStore.isShopOwner)
const isTenantAdmin = computed(() => authStore.isTenantAdmin)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReviewList({
      page: pagination.current,
      pageSize: pagination.pageSize,
      platform: filters.platform,
      replyStatus: filters.replyStatus,
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

const loadShopList = async () => {
  const res = await getShopSelectOptionsApi()
  shopList.value = res
}

const loadTemplateList = async () => {
  try {
    const res = await getReplyTemplateList()
    templateList.value = res
  } catch {
    templateList.value = []
  }
}

const onTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadData()
}

const onReset = () => {
  filters.platform = undefined
  filters.replyStatus = undefined
  filters.shopId = undefined
  filters.keyword = ''
  filters.startDate = undefined
  filters.endDate = undefined
  pagination.current = 1
  void loadData()
}

const onSearch = () => {
  pagination.current = 1
  void loadData()
}

const onRowClick = (record: Record<string, unknown>) => {
  selectedRow.value = record as unknown as ReviewItem
  if (isTenantAdmin.value) {
    createTicketOpen.value = true
  } else if (isShopOwner.value) {
    if (selectedRow.value?.replyStatus === 1) {
      message.warning('该评价已回复')
      return
    }
    void loadTemplateList()
    dialogOpen.value = true
  }
}

const submitDialog = async () => {
  if (replyFormModel.useTemplate && replyFormModel.templateId) {
    try {
      replyLoading.value = true
      await replyReviewWithTemplate(selectedRow.value!.id, replyFormModel.templateId)
      message.success('回复成功')
      dialogOpen.value = false
      void loadData()
    } catch {
      // error handled by interceptor
    } finally {
      replyLoading.value = false
    }
  } else if (replyFormModel.content) {
    try {
      replyLoading.value = true
      await replyReview(selectedRow.value!.id, replyFormModel.content)
      message.success('回复成功')
      dialogOpen.value = false
      void loadData()
    } catch {
      // error handled by interceptor
    } finally {
      replyLoading.value = false
    }
  } else {
    message.warning('请选择模板或输入回复内容')
  }
}

const getPlatformText = (platform: number) => {
  return platform === 1 ? '美团' : '饿了么'
}

const getReplyStatusText = (status: number) => {
  return status === 1 ? '已回复' : '未回复'
}

const getReplyStatusColor = (status: number) => {
  return status === 1 ? 'green' : 'red'
}

const getStarRatingText = (rating: number) => {
  return '★'.repeat(rating) + '☆'.repeat(5 - rating)
}

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

const handleTemplateChange = (templateId: number) => {
  const template = templateList.value.find(t => t.id === templateId)
  if (template) {
    replyFormModel.content = template.content
    replyFormModel.useTemplate = true
  }
}

const onDialogReset = () => {
  replyFormModel.content = ''
  replyFormModel.templateId = undefined
  replyFormModel.useTemplate = false
}

void onMounted(() => {
  void loadData()
  void loadShopList()
})

watch(
  () => filters.platform,
  () => {
    pagination.current = 1
    void loadData()
  }
)

watch(
  () => filters.replyStatus,
  () => {
    pagination.current = 1
    void loadData()
  }
)

watch(
  () => filters.shopId,
  () => {
    pagination.current = 1
    void loadData()
  }
)
</script>

<template>
  <div>
    <BaseTableList
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="{
        current: pagination.current,
        pageSize: pagination.pageSize,
        total: pagination.total,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total: number) => `共 ${total} 条`,
      }"
      :hide-default-actions="true"
      @query="onSearch"
      @reset="onReset"
      @refresh="() => loadData()"
      @change="onTableChange"
    >
      <template #filters>
        <a-input
          v-model:value="filters.keyword"
          placeholder="搜索评价内容"
          style="width: 180px"
          allow-clear
          @pressEnter="onSearch"
        />
        <a-select
          v-model:value="filters.platform"
          allow-clear
          placeholder="平台"
          style="width: 100px"
        >
          <a-select-option :value="1">美团</a-select-option>
          <a-select-option :value="2">饿了么</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.replyStatus"
          allow-clear
          placeholder="回复状态"
          style="width: 100px"
        >
          <a-select-option :value="0">未回复</a-select-option>
          <a-select-option :value="1">已回复</a-select-option>
        </a-select>
        <a-select
          v-if="isTenantAdmin"
          v-model:value="filters.shopId"
          allow-clear
          placeholder="选择门店"
          style="width: 150px"
          :options="shopList.map((s) => ({ label: s.shopName, value: s.id }))"
        />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'content'">
          <a-tooltip :title="(record as ReviewItem).content">
            <span class="base-table-link" @click="() => onRowClick(record)">{{ (record as ReviewItem).content }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'starRating'">
          <span style="color: #faad14">{{ getStarRatingText((record as ReviewItem).starRating) }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'platform'">
          {{ getPlatformText((record as ReviewItem).platform) }}
        </template>
        <template v-else-if="column.dataIndex === 'replyStatus'">
          <a-tag :color="getReplyStatusColor((record as ReviewItem).replyStatus)">
            {{ getReplyStatusText((record as ReviewItem).replyStatus) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createdAt'">
          {{ formatDateTime((record as ReviewItem).createdAt) }}
        </template>
      </template>
    </BaseTableList>

    <BaseEditDialog
      v-model:open="dialogOpen"
      title="回复评价"
      mode="edit"
      :model="replyFormModel"
      :rules="replyFormRules"
      :loading="replyLoading"
      :width="600"
      @submit="submitDialog"
      @reset="onDialogReset"
    >
      <a-form-item label="选择模板" name="templateId">
        <a-select
          v-model:value="replyFormModel.templateId"
          placeholder="请选择回复模板（可选）"
          allow-clear
          style="width: 100%"
          @change="handleTemplateChange"
        >
          <a-select-option v-for="t in templateList" :key="t.id" :value="t.id">
            {{ t.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="回复内容" name="content">
        <template #label>
          <span class="required-label">回复内容</span>
        </template>
        <a-textarea
          v-model:value="replyFormModel.content"
          placeholder="请输入回复内容（最多500字）"
          :rows="4"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </BaseEditDialog>

    <CreateTicketDialog
      v-model:open="createTicketOpen"
      :review-id="selectedRow?.id"
      :review-content="selectedRow?.content ?? ''"
      :review-star="selectedRow?.starRating ?? 0"
      :shop-id="selectedRow?.shopId"
      @success="() => loadData()"
    />
  </div>
</template>
