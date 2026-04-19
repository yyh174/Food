<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { message, Modal, type TableColumnType } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import BaseEditDialog from '../../components/base/BaseEditDialog.vue'
import BaseTableList from '../../components/base/BaseTableList.vue'
import {
  getShopListApi,
  getShopDetailApi,
  createShopApi,
  updateShopApi,
  deleteShopApi,
  type ShopListParams,
} from '../../api/shop'
import type {
  ShopItem,
  ShopCreateRequest,
  ShopUpdateRequest,
} from '../../types/shop'

const loading = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const tableData = ref<ShopItem[]>([])
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const filters = reactive({
  keyword: '',
  status: undefined as number | undefined,
})

const formModel = reactive<ShopCreateRequest & { id?: number; status: number }>({
  shopName: '',
  province: '',
  city: '',
  district: '',
  address: '',
  managerName: '',
  managerPhone: '',
  meituanId: '',
  eleId: '',
  status: 1,
})

const createFormRules: Record<string, Rule[]> = {
  shopName: [{ required: true, message: '请输入门店名称' }],
}

const editFormRules: Record<string, Rule[]> = {
  shopName: [{ required: true, message: '请输入门店名称' }],
  status: [{ required: true, message: '请选择状态' }],
}

const columns: TableColumnType[] = [
  { title: '门店名称', dataIndex: 'shopName', width: 180 },
  { title: '门店编码', dataIndex: 'shopCode', width: 120 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 160 },
]

const mapStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '已停业', 1: '营业中', 2: '休息中' }
  return map[status] || '-'
}

const loadData = async () => {
  loading.value = true
  try {
    const params: ShopListParams = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (filters.keyword) {
      params.keyword = filters.keyword
    }
    if (filters.status !== undefined) {
      params.status = filters.status
    }
    const res = await getShopListApi(params)
    tableData.value = res.records
    pagination.total = res.total
  } catch {
    message.error('加载门店列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})

const onReset = () => {
  filters.keyword = ''
  filters.status = undefined
  pagination.current = 1
  loadData()
}

const onCreate = () => {
  dialogMode.value = 'create'
  Object.assign(formModel, {
    id: undefined,
    shopName: '',
    province: '',
    city: '',
    district: '',
    address: '',
    managerName: '',
    managerPhone: '',
    meituanId: '',
    eleId: '',
    status: 1,
  })
  dialogOpen.value = true
}

const onRowClick = (record: Record<string, unknown>) => {
  dialogMode.value = 'edit'
  const row = record as unknown as ShopItem
  Object.assign(formModel, {
    id: row.id,
    shopName: row.shopName,
    province: row.province || '',
    city: row.city || '',
    district: row.district || '',
    address: row.address || '',
    managerName: row.managerName || '',
    managerPhone: row.managerPhone || '',
    meituanId: row.meituanId || '',
    eleId: row.eleId || '',
    status: row.status,
  })
  dialogOpen.value = true
}

const onDialogReset = () => {
  if (dialogMode.value === 'create') {
    Object.assign(formModel, {
      shopName: '',
      province: '',
      city: '',
      district: '',
      address: '',
      managerName: '',
      managerPhone: '',
      meituanId: '',
      eleId: '',
      status: 1,
    })
  }
}

const submitDialog = async () => {
  try {
    if (dialogMode.value === 'create') {
      const data: ShopCreateRequest = {
        shopName: formModel.shopName,
        province: formModel.province || undefined,
        city: formModel.city || undefined,
        district: formModel.district || undefined,
        address: formModel.address || undefined,
        managerName: formModel.managerName || undefined,
        managerPhone: formModel.managerPhone || undefined,
        meituanId: formModel.meituanId || undefined,
        eleId: formModel.eleId || undefined,
      }
      await createShopApi(data)
      message.success('创建成功')
      dialogOpen.value = false
      await loadData()
    } else {
      const data: ShopUpdateRequest = {
        shopName: formModel.shopName,
        province: formModel.province || undefined,
        city: formModel.city || undefined,
        district: formModel.district || undefined,
        address: formModel.address || undefined,
        managerName: formModel.managerName || undefined,
        managerPhone: formModel.managerPhone || undefined,
        meituanId: formModel.meituanId || undefined,
        eleId: formModel.eleId || undefined,
        status: formModel.status,
      }
      await updateShopApi(formModel.id!, data)
      message.success('保存成功')
      dialogOpen.value = false
      await loadData()
    }
  } catch {
    // 错误已在拦截器处理
  }
}

const onDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除门店"${formModel.shopName}"吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteShopApi(formModel.id!)
        message.success('删除成功')
        dialogOpen.value = false
        await loadData()
      } catch {
        // 错误已在拦截器处理
      }
    },
  })
}
</script>

<template>
  <BaseTableList
    :columns="columns"
    :data-source="tableData"
    :loading="loading"
    :total="pagination.total"
    :pagination="{
      current: pagination.current,
      pageSize: pagination.pageSize,
      total: pagination.total,
    }"
    :hide-default-actions="true"
    :show-query="false"
    :show-refresh="false"
    @query="() => loadData()"
    @reset="onReset"
    @first-column-click="onRowClick"
  >
    <template #filters>
      <a-select
        v-model:value="filters.status"
        allow-clear
        placeholder="状态筛选"
        style="width: 120px"
        @change="loadData"
      >
        <a-select-option :value="1">营业中</a-select-option>
        <a-select-option :value="2">休息中</a-select-option>
        <a-select-option :value="0">已停业</a-select-option>
      </a-select>
      <a-input-search
        v-model:value="filters.keyword"
        placeholder="搜索门店名称/编码"
        style="width: 220px"
        @search="loadData"
      />
    </template>
    <template #actions>
      <a-button type="primary" @click="onCreate">新增</a-button>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">{{ mapStatusText(Number(text)) }}</template>
    </template>
  </BaseTableList>

  <BaseEditDialog
    v-model:open="dialogOpen"
    title="门店"
    :mode="dialogMode"
    :model="formModel"
    :rules="dialogMode === 'create' ? createFormRules : editFormRules"
    :show-delete="dialogMode === 'edit'"
    :width="600"
    :label-width="110"
    @submit="submitDialog"
    @delete="onDelete"
    @reset="onDialogReset"
  >
    <a-form-item label="门店名称" name="shopName">
      <template #label>
        <span class="required-label">门店名称</span>
      </template>
      <a-input v-model:value="formModel.shopName" placeholder="请输入门店名称" />
    </a-form-item>
    <a-form-item label="详细地址">
      <a-input-group compact style="display: flex; gap: 8px">
        <a-input v-model:value="formModel.province" placeholder="省份" style="flex: 1" />
        <a-input v-model:value="formModel.city" placeholder="城市" style="flex: 1" />
        <a-input v-model:value="formModel.district" placeholder="区县" style="flex: 1" />
      </a-input-group>
    </a-form-item>
    <a-form-item label="店长姓名" name="managerName">
      <a-input v-model:value="formModel.managerName" placeholder="请输入店长姓名" />
    </a-form-item>
    <a-form-item label="店长电话" name="managerPhone">
      <a-input v-model:value="formModel.managerPhone" placeholder="请输入店长电话" />
    </a-form-item>
    <a-form-item label="美团门店ID" name="meituanId">
      <a-input v-model:value="formModel.meituanId" placeholder="请输入美团门店ID" />
    </a-form-item>
    <a-form-item label="饿了么门店ID" name="eleId">
      <a-input v-model:value="formModel.eleId" placeholder="请输入饿了么门店ID" />
    </a-form-item>
    <a-form-item v-if="dialogMode === 'edit'" label="状态" name="status">
      <template #label>
        <span class="required-label">状态</span>
      </template>
      <a-radio-group v-model:value="formModel.status">
        <a-radio :value="1">营业中</a-radio>
        <a-radio :value="2">休息中</a-radio>
        <a-radio :value="0">已停业</a-radio>
      </a-radio-group>
    </a-form-item>
  </BaseEditDialog>
</template>
