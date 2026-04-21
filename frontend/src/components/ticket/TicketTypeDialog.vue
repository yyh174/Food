<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import { createTicketType, deleteTicketType, getTicketTypes } from '../../api/ticket'
import type { TicketTypeItem } from '../../types/ticket'

const props = defineProps<{ open: boolean }>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const loading = ref(false)
const formRef = ref()
const formModel = ref({ name: '' })
const typeList = ref<TicketTypeItem[]>([])

const rules: Record<string, Rule[]> = {
  name: [
    { required: true, message: '请输入类型名称' },
    { max: 20, message: '类型名称最多20字' },
  ],
}

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
    await createTicketType(formModel.value.name)
    message.success('创建成功')
    emit('success')
    emit('update:open', false)
    resetForm()
  } finally {
    loading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await deleteTicketType(id)
    message.success('删除成功')
    void loadTypes()
    emit('success')
  } catch {
    // error handled by interceptor
  }
}

const resetForm = () => {
  formModel.value.name = ''
}

const loadTypes = async () => {
  try {
    const res = await getTicketTypes('custom')
    typeList.value = res
  } catch {
    typeList.value = []
  }
}
</script>

<template>
  <a-modal
    :open="open"
    title="工单类型管理"
    :width="520"
    :confirm-loading="loading"
    @cancel="handleCancel"
    @ok="handleSubmit"
  >
    <a-form ref="formRef" :model="formModel" :rules="rules" layout="vertical">
      <a-form-item label="新建自定义类型" name="name">
        <template #label>
          <span class="required-label">新建自定义类型</span>
        </template>
        <div style="display: flex; gap: 8px">
          <a-input
            v-model:value="formModel.name"
            placeholder="请输入类型名称（最多20字）"
            :maxlength="20"
            show-count
            style="flex: 1"
          />
          <a-button type="primary" @click="handleSubmit" :loading="loading">添加</a-button>
        </div>
      </a-form-item>
    </a-form>

    <a-divider>已有自定义类型</a-divider>

    <a-table
      :data-source="typeList"
      :pagination="false"
      size="small"
      :columns="[{ title: '名称', dataIndex: 'name' }, { title: '操作', dataIndex: 'action', width: 80 }]"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'action'">
          <a-popconfirm title="确定删除该类型？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger size="small">删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>
