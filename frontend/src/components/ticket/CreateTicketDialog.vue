<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import { createTicket, getTicketTypes, getReviewsForTicket } from '../../api/ticket'
import { getShopSelectOptionsApi } from '../../api/shop'
import type { TicketTypeItem, ReviewForTicket } from '../../types/ticket'
import type { ShopItem } from '../../types/shop'
import dayjs from 'dayjs'

const props = withDefaults(
  defineProps<{
    open: boolean
    reviewId?: number
    reviewContent?: string
    reviewStar?: number
    shopId?: number
  }>(),
  {
    reviewId: undefined,
    reviewContent: '',
    reviewStar: 0,
    shopId: undefined,
  },
)

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const formRef = ref()
const shopList = ref<ShopItem[]>([])
const typeList = ref<TicketTypeItem[]>([])
const reviewList = ref<ReviewForTicket[]>([])
const reviewLoading = ref(false)

const selectedType = computed(() => typeList.value.find(t => t.id === formModel.value.typeId))
const needReview = computed(() => !!selectedType.value?.supportReview)
const reviewFromList = ref(false)
const isReviewPreset = ref(false) // 标记 reviewId 是否由外部传入

const formModel = ref({
  title: '',
  description: '',
  typeId: undefined as number | undefined,
  shopId: undefined as number | undefined,
  deadline: dayjs().add(7, 'day').format('YYYY-MM-DD 23:59:59') as string | undefined,
  images: [] as string[],
  reviewId: undefined as number | undefined,
  reviewContent: '',
  reviewStar: 0,
})

const rules: Record<string, Rule[]> = {
  title: [{ required: true, message: '请输入工单标题' }],
  description: [{ required: true, message: '请输入工单描述' }],
  typeId: [{ required: true, message: '请选择工单类型' }],
  shopId: [{ required: true, message: '请选择关联门店' }],
}

const reviewIdRules = computed((): Rule[] => {
  // 如果 reviewId 已存在（无论是列表选择还是外部传入），无需校验
  if (formModel.value.reviewId) return []
  // 如果类型不需要关联评价，跳过校验
  if (!needReview.value) return []
  return [{ required: true, message: '差评预警类型必须关联一个评价' }]
})

const handleCancel = () => {
  emit('update:open', false)
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await createTicket({
      title: formModel.value.title,
      description: formModel.value.description,
      typeId: formModel.value.typeId!,
      shopId: formModel.value.shopId!,
      deadline: formModel.value.deadline || undefined,
      images: formModel.value.images.length > 0 ? formModel.value.images : undefined,
      reviewId: needReview.value ? formModel.value.reviewId : undefined,
    })
    message.success('工单创建成功')
    emit('success')
    emit('update:open', false)
    resetForm()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formModel.value = {
    title: '',
    description: '',
    typeId: undefined,
    shopId: undefined,
    deadline: dayjs().add(7, 'day').format('YYYY-MM-DD 23:59:59'),
    images: [],
    reviewId: undefined,
    reviewContent: '',
    reviewStar: 0,
  }
  reviewList.value = []
  isReviewPreset.value = false
}

const loadShops = async () => {
  try {
    shopList.value = await getShopSelectOptionsApi()
  } catch {
    shopList.value = []
  }
}

const loadTypes = async () => {
  try {
    typeList.value = await getTicketTypes()
  } catch {
    typeList.value = []
  }
}

const loadReviews = async () => {
  if (!needReview.value || !formModel.value.shopId) {
    reviewList.value = []
    return
  }
  reviewLoading.value = true
  try {
    const res = await getReviewsForTicket({
      shopId: formModel.value.shopId,
      starRating: 2,
      page: 1,
      pageSize: 20,
    })
    reviewList.value = res.records
  } catch {
    reviewList.value = []
  } finally {
    reviewLoading.value = false
  }
}

watch(() => props.open, async (val) => {
  if (val) {
    void loadShops()
    await loadTypes()
    reviewFromList.value = false
    isReviewPreset.value = !!props.reviewId
    formModel.value.reviewId = props.reviewId
    formModel.value.reviewContent = props.reviewContent || ''
    formModel.value.reviewStar = props.reviewStar || 0
    formModel.value.shopId = props.shopId
    if (props.reviewId) {
      reviewFromList.value = true
      const negativeType = typeList.value.find(t => !!t.supportReview)
      if (negativeType) {
        formModel.value.typeId = negativeType.id
      }
    }
  } else {
    resetForm()
  }
})

watch(() => formModel.value.shopId, () => {
  // 如果 reviewId 是外部传入的，不清空
  if (!isReviewPreset.value) {
    formModel.value.reviewId = undefined
  }
  if (needReview.value) {
    void loadReviews()
  }
})

watch(() => formModel.value.typeId, () => {
  if (!needReview.value) {
    formModel.value.reviewId = undefined
    reviewList.value = []
  } else if (formModel.value.shopId) {
    void loadReviews()
  }
})
</script>

<template>
  <a-modal
    :open="open"
    title="新建工单"
    :width="600"
    :confirm-loading="loading"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <a-form-item label="工单类型" name="typeId">
        <template #label>
          <span class="required-label">工单类型</span>
        </template>
        <a-select
          v-model:value="formModel.typeId"
          placeholder="请选择工单类型"
          :options="typeList.map(t => ({ label: t.name, value: t.id }))"
        />
      </a-form-item>

      <a-form-item label="关联门店" name="shopId">
        <template #label>
          <span class="required-label">关联门店</span>
        </template>
        <a-select
          v-model:value="formModel.shopId"
          placeholder="请选择关联门店"
          :options="shopList.map(s => ({ label: s.shopName, value: s.id }))"
        />
      </a-form-item>

      <a-form-item label="关联评价" name="reviewId" :rules="reviewIdRules" v-show="needReview && !reviewFromList">
        <a-select
          v-model:value="formModel.reviewId"
          placeholder="请选择关联评价"
          :loading="reviewLoading"
          :options="reviewList.map(r => ({ label: `[${r.platform === 1 ? '美团' : '饿了么'}] ${r.starRating}星: ${r.content.substring(0, 30)}...`, value: r.id }))"
        />
      </a-form-item>

      <a-form-item label="关联评价" v-show="needReview && reviewFromList">
        <div style="padding: 8px 12px; background: #f5f5f5; border-radius: 4px; color: #333">
          <span style="color: #faad14">{{ '★'.repeat(formModel.reviewStar) }}{{ '☆'.repeat(5 - formModel.reviewStar) }}</span>
          {{ formModel.reviewContent }}
        </div>
      </a-form-item>

      <a-form-item label="工单标题" name="title">
        <template #label>
          <span class="required-label">工单标题</span>
        </template>
        <a-input
          v-model:value="formModel.title"
          placeholder="请输入工单标题（最多100字）"
          :maxlength="100"
          show-count
        />
      </a-form-item>

      <a-form-item label="问题描述" name="description">
        <template #label>
          <span class="required-label">问题描述</span>
        </template>
        <a-textarea
          v-model:value="formModel.description"
          placeholder="请输入问题描述（最多1000字）"
          :rows="4"
          :maxlength="1000"
          show-count
        />
      </a-form-item>

      <a-form-item label="截止时间">
        <a-date-picker
          v-model:value="formModel.deadline"
          :show-time="{ format: 'HH:mm:ss' }"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
          placeholder="截止时间（默认7天后）"
          :disabled-date="(current: dayjs.Dayjs) => current && current < dayjs().startOf('day')"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
