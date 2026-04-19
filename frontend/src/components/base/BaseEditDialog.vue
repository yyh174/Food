<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'

export interface ExtraAction {
  key: string
  label: string
  danger?: boolean
}

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    mode: 'create' | 'edit'
    model: Record<string, unknown>
    rules?: Record<string, Rule[]>
    loading?: boolean
    showDelete?: boolean
    width?: number
    labelWidth?: number | string
    extraActions?: ExtraAction[]
  }>(),
  {
    loading: false,
    showDelete: false,
    width: 520,
    labelWidth: 100,
    rules: () => ({}),
    extraActions: () => [],
  },
)

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'submit'): void
  (e: 'delete'): void
  (e: 'reset'): void
  (e: 'extra', key: string): void
}>()

const formRef = ref<FormInstance>()

const dialogTitle = computed(() => `${props.mode === 'create' ? '新增' : '编辑'}${props.title}`)

const labelColStyle = computed(() => {
  const width = typeof props.labelWidth === 'number' ? `${props.labelWidth}px` : props.labelWidth
  return { style: { width } }
})

const handleCancel = () => {
  emit('update:open', false)
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    emit('submit')
  } catch {
    // 验证失败，不提交
  }
}

const handleDelete = () => {
  emit('delete')
}

const handleExtra = (key: string) => {
  emit('extra', key)
}

watch(
  () => props.open,
  async (value) => {
    if (!value) {
      emit('reset')
      return
    }
    await nextTick()
    formRef.value?.clearValidate()
  },
)
</script>

<template>
  <a-modal :open="open" :title="dialogTitle" :width="width" @cancel="handleCancel">
    <a-form ref="formRef" :model="model" :rules="rules" :label-col="labelColStyle">
      <slot />
    </a-form>
    <template #footer>
      <div class="dialog-footer">
        <template v-if="mode === 'edit'">
          <a-button v-if="showDelete" danger @click="handleDelete">删除</a-button>
          <template v-for="action in extraActions" :key="action.key">
            <a-button :danger="action.danger" @click="handleExtra(action.key)">
              {{ action.label }}
            </a-button>
          </template>
        </template>
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleSubmit">确定</a-button>
      </div>
    </template>
  </a-modal>
</template>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
