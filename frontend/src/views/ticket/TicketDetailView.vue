<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  getTicketDetail,
  submitTicket,
  verifyTicket,
  archiveTicket,
  rejectTicket,
} from '../../api/ticket'
import type { TicketDetail } from '../../types/ticket'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<TicketDetail | null>(null)

const isTenantAdmin = computed(() => authStore.isTenantAdmin)
const isShopOwner = computed(() => authStore.isShopOwner)

const statusTagColor = computed(() => {
  if (!detail.value) return 'default'
  const s = detail.value.status
  if (s === 4 || s === 5) return 'default'
  if (s === 3) return 'blue'
  if (s === 2) return 'orange'
  return 'red'
})

const priorityColor = computed(() => detail.value?.priorityColor || '#52c41a')
const priorityText = computed(() => detail.value?.priorityText || '低')

const loadData = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    detail.value = await getTicketDetail(id)
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/tickets')
}

const formatDateTime = (v: string | null | undefined) => {
  if (!v) return '-'
  return v.replace('T', ' ').substring(0, 19)
}

const getActionText = (action: string) => {
  const map: Record<string, string> = {
    created: '创建工单',
    assigned: '派单',
    submit: '提交整改',
    verified: '核销',
    archived: '归档',
    rejected: '打回',
  }
  return map[action] || action
}

const getLogColor = (action: string) => {
  const map: Record<string, string> = {
    created: '#1890ff',
    assigned: '#faad14',
    submit: '#52c41a',
    verified: '#722ed1',
    archived: '#8c8c8c',
    rejected: '#ff4d4f',
  }
  return map[action] || '#1890ff'
}

// 提交整改
const showSubmitModal = ref(false)
const submitForm = ref({ content: '' })

const onSubmit = async () => {
  if (!submitForm.value.content.trim()) {
    message.warning('请输入整改说明')
    return
  }
  actionLoading.value = true
  try {
    await submitTicket(detail.value!.id, { content: submitForm.value.content })
    message.success('提交成功')
    showSubmitModal.value = false
    submitForm.value.content = ''
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

// 核销
const showVerifyModal = ref(false)
const verifyForm = ref({ content: '' })

const onVerify = async () => {
  if (!verifyForm.value.content.trim()) {
    message.warning('请输入核销说明')
    return
  }
  actionLoading.value = true
  try {
    await verifyTicket(detail.value!.id, { content: verifyForm.value.content })
    message.success('核销成功')
    showVerifyModal.value = false
    verifyForm.value.content = ''
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

// 归档
const showArchiveModal = ref(false)
const archiveForm = ref({ content: '' })

const onArchive = async () => {
  if (!archiveForm.value.content.trim()) {
    message.warning('请输入归档原因')
    return
  }
  actionLoading.value = true
  try {
    await archiveTicket(detail.value!.id, { content: archiveForm.value.content })
    message.success('归档成功')
    showArchiveModal.value = false
    archiveForm.value.content = ''
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

// 打回
const showRejectModal = ref(false)
const rejectForm = ref({ content: '', deadline: '' as string | undefined })

const onReject = async () => {
  if (!rejectForm.value.content.trim()) {
    message.warning('请输入打回原因')
    return
  }
  actionLoading.value = true
  try {
    await rejectTicket(detail.value!.id, {
      content: rejectForm.value.content,
      deadline: rejectForm.value.deadline || undefined,
    })
    message.success('打回成功')
    showRejectModal.value = false
    rejectForm.value = { content: '', deadline: '' }
    await loadData()
  } finally {
    actionLoading.value = false
  }
}

void onMounted(() => {
  void loadData()
})
</script>

<template>
  <div class="ticket-detail">
    <a-spin :spinning="loading">
      <div class="detail-toolbar">
        <a-button @click="goBack">
          <template #icon>
            <span>&#8592;</span>
          </template>
          返回列表
        </a-button>
        <span class="detail-title">工单详情</span>
      </div>

      <a-row :gutter="16" v-if="detail">
        <a-col :span="15">
          <div class="left-column">
            <!-- 基本信息卡片 -->
            <a-card title="基本信息" size="small">
              <a-descriptions :column="2" size="small" bordered>
                <a-descriptions-item label="工单编号">{{ detail.ticketNo }}</a-descriptions-item>
                <a-descriptions-item label="工单类型">{{ detail.typeName }}</a-descriptions-item>
                <a-descriptions-item label="关联门店">{{ detail.shopName }}</a-descriptions-item>
                <a-descriptions-item label="处理人">{{ detail.assigneeName || '-' }}</a-descriptions-item>
                <a-descriptions-item label="优先级">
                  <span :style="{ color: priorityColor }">{{ priorityText }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="截止时间">{{ formatDateTime(detail.deadline) }}</a-descriptions-item>
                <a-descriptions-item label="工单状态">
                  <a-tag :color="statusTagColor">{{ detail.statusName }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="创建人">{{ detail.creatorName }}</a-descriptions-item>
                <a-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</a-descriptions-item>
                <a-descriptions-item label="更新时间">{{ formatDateTime(detail.updatedAt) }}</a-descriptions-item>
                <a-descriptions-item label="工单标题" :span="2">{{ detail.title }}</a-descriptions-item>
                <a-descriptions-item label="问题描述" :span="2">{{ detail.description }}</a-descriptions-item>
              </a-descriptions>
            </a-card>

            <!-- 关联评价卡片 -->
            <a-card v-if="detail.review" title="关联评价" size="small">
              <a-descriptions :column="2" size="small" bordered>
                <a-descriptions-item label="平台">{{ detail.review.platform === 1 ? '美团' : '饿了么' }}</a-descriptions-item>
                <a-descriptions-item label="订单号">{{ detail.review.platformOrderId }}</a-descriptions-item>
                <a-descriptions-item label="评分">
                  <span style="color: #faad14">{{ '★'.repeat(detail.review.starRating) }}{{ '☆'.repeat(5 - detail.review.starRating) }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="评价时间">{{ formatDateTime(detail.review.createdAt) }}</a-descriptions-item>
                <a-descriptions-item label="评价内容" :span="2">{{ detail.review.content }}</a-descriptions-item>
              </a-descriptions>
            </a-card>

            <!-- 操作面板 -->
            <a-card title="操作" size="small">
              <!-- 租户管理员视角 -->
              <template v-if="isTenantAdmin">
                <template v-if="detail.status === 2">
                  <a-button type="primary" @click="showArchiveModal = true">归档</a-button>
                </template>
                <template v-else-if="detail.status === 3">
                  <a-button type="primary" style="margin-right: 8px" @click="showVerifyModal = true">核销</a-button>
                  <a-button @click="showRejectModal = true">打回</a-button>
                </template>
                <template v-else-if="detail.status === 4 || detail.status === 5">
                  <span style="color: #999">该工单已结束</span>
                </template>
              </template>

              <!-- 店长视角 -->
              <template v-if="isShopOwner">
                <template v-if="detail.status === 1 || detail.status === 2">
                  <a-button type="primary" @click="showSubmitModal = true">提交整改</a-button>
                </template>
                <template v-else-if="detail.status === 3">
                  <span style="color: #999">等待核销中...</span>
                </template>
                <template v-else-if="detail.status === 4 || detail.status === 5">
                  <span style="color: #999">该工单已结束</span>
                </template>
              </template>
            </a-card>
          </div>
        </a-col>

        <a-col :span="9">
          <!-- 时间线 -->
          <a-card title="处理记录" size="small" class="right-column">
            <a-timeline>
              <a-timeline-item
                v-for="log in detail.logs"
                :key="log.id"
                :color="getLogColor(log.action)"
              >
                <div class="log-item">
                  <div class="log-header">
                    <span class="log-action">{{ getActionText(log.action) }}</span>
                    <span class="log-operator">{{ log.operatorName }}</span>
                  </div>
                  <div class="log-content">{{ log.content }}</div>
                  <div class="log-time">{{ formatDateTime(log.createdAt) }}</div>
                </div>
              </a-timeline-item>
            </a-timeline>
            <div v-if="!detail.logs || detail.logs.length === 0" style="color: #999; text-align: center; padding: 16px">
              暂无处理记录
            </div>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>

    <!-- 提交整改弹窗 -->
    <a-modal v-model:open="showSubmitModal" title="提交整改" :confirm-loading="actionLoading" @ok="onSubmit">
      <a-form :model="submitForm" layout="vertical">
        <a-form-item label="整改说明" required>
          <a-textarea v-model:value="submitForm.content" placeholder="请输入整改说明（最多500字）" :rows="4" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 核销弹窗 -->
    <a-modal v-model:open="showVerifyModal" title="核销工单" :confirm-loading="actionLoading" @ok="onVerify">
      <a-form :model="verifyForm" layout="vertical">
        <a-form-item label="核销说明" required>
          <a-textarea v-model:value="verifyForm.content" placeholder="请输入核销说明（最多500字）" :rows="4" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 归档弹窗 -->
    <a-modal v-model:open="showArchiveModal" title="归档工单" :confirm-loading="actionLoading" @ok="onArchive">
      <a-form :model="archiveForm" layout="vertical">
        <a-form-item label="归档原因" required>
          <a-textarea v-model:value="archiveForm.content" placeholder="请输入归档原因（最多500字）" :rows="4" :maxlength="500" show-count />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 打回弹窗 -->
    <a-modal v-model:open="showRejectModal" title="打回工单" :confirm-loading="actionLoading" @ok="onReject">
      <a-form :model="rejectForm" layout="vertical">
        <a-form-item label="打回原因" required>
          <a-textarea v-model:value="rejectForm.content" placeholder="请输入打回原因（最多500字）" :rows="4" :maxlength="500" show-count />
        </a-form-item>
        <a-form-item label="更新截止时间（可选）">
          <a-date-picker
            v-model:value="rejectForm.deadline"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
            placeholder="可选择新的截止时间"
            :disabled-date="(current: any) => current && current < dayjs().startOf('day')"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.ticket-detail {
  padding: 16px;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
}

.left-column,
.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.left-column :deep(.ant-card) {
  flex: 1;
}

.right-column :deep(.ant-card) {
  flex: 1;
}

@media (min-width: 1200px) {
  .left-column {
    min-height: calc(100vh - 200px);
  }
  .right-column {
    min-height: calc(100vh - 200px);
  }
}

.log-item {
  line-height: 1.6;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.log-action {
  font-weight: 600;
  color: #333;
}

.log-operator {
  color: #666;
  font-size: 12px;
}

.log-content {
  color: #333;
  margin-bottom: 4px;
}

.log-time {
  color: #999;
  font-size: 12px;
}
</style>
