<script setup lang="ts">
import { onMounted, reactive, ref, computed, watch } from 'vue'
import { message, Modal, type TableColumnType } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import BaseEditDialog from '../../components/base/BaseEditDialog.vue'
import BaseTableList from '../../components/base/BaseTableList.vue'
import { getUserListApi, createUserApi, updateUserApi, deleteUserApi, resetUserPasswordApi } from '../../api/user'
import { getAvailableShopListApi, getShopListApi } from '../../api/shop'
import type { UserItem } from '../../types/user'
import type { ShopItem } from '../../types/shop'

const loading = ref(false)
const dialogOpen = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const tableData = ref<UserItem[]>([])
const selectedRow = ref<UserItem | null>(null)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const shopList = ref<ShopItem[]>([])
const shopLoading = ref(false)

const filters = reactive({
  keyword: '',
  roleCode: undefined as string | undefined,
  status: undefined as number | undefined,
})

const formModel = reactive({
  username: '',
  realName: '',
  phone: '',
  roleCode: 'shop_owner',
  shopId: undefined as number | undefined,
  status: 1,
})

const formRules: Record<string, Rule[]> = {
  username: [{ required: true, message: '请输入用户名' }],
  realName: [{ required: true, message: '请输入姓名' }],
}

const columns: TableColumnType[] = [
  { title: '用户名', dataIndex: 'username', width: 120 },
  { title: '角色', dataIndex: 'roleName', width: 100 },
  { title: '关联门店', dataIndex: 'shopName', ellipsis: true },
  { title: '最后登录', dataIndex: 'lastLoginTime', width: 160 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', width: 160 },
]

const isEditMode = computed(() => dialogMode.value === 'edit')

const loadShopList = async () => {
  shopLoading.value = true
  try {
    const res = await getAvailableShopListApi()
    shopList.value = res
    // 编辑模式下，如果当前用户已关联门店，确保该门店出现在下拉框中
    if (isEditMode.value && selectedRow.value?.shopId) {
      const currentShopInList = shopList.value.some(s => s.id === selectedRow.value!.shopId)
      if (!currentShopInList) {
        const fullRes = await getShopListApi({ page: 1, pageSize: 1000 })
        const currentShop = fullRes.records.find((s: ShopItem) => s.id === selectedRow.value!.shopId)
        if (currentShop) {
          shopList.value = [currentShop, ...shopList.value]
        }
      }
    }
  } finally {
    shopLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserListApi({
      page: pagination.current,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      roleCode: filters.roleCode,
      status: filters.status,
    })
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

const onTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadData()
}

void onMounted(() => {
  void loadData()
})

const onCreate = () => {
  dialogMode.value = 'create'
  selectedRow.value = null
  void loadShopList()
  dialogOpen.value = true
}

const onRowClick = (record: Record<string, unknown>) => {
  dialogMode.value = 'edit'
  selectedRow.value = record as unknown as UserItem
  Object.assign(formModel, {
    username: selectedRow.value.username,
    realName: selectedRow.value.realName,
    phone: selectedRow.value.phone || '',
    roleCode: selectedRow.value.roleCode,
    shopId: selectedRow.value.shopId,
    status: selectedRow.value.status,
  })
  void loadShopList()
  dialogOpen.value = true
}

const onDialogReset = () => {
  Object.assign(formModel, {
    username: '',
    realName: '',
    phone: '',
    roleCode: 'shop_owner',
    shopId: undefined,
    status: 1,
  })
}

const submitDialog = async () => {
  try {
    if (dialogMode.value === 'create') {
      await createUserApi({
        username: formModel.username,
        realName: formModel.realName,
        phone: formModel.phone || undefined,
        roleCode: formModel.roleCode,
        shopId: formModel.roleCode === 'shop_owner' ? formModel.shopId : undefined,
      })
      message.success('创建成功')
    } else {
      await updateUserApi(selectedRow.value!.id, {
        realName: formModel.realName,
        phone: formModel.phone || undefined,
        roleCode: formModel.roleCode,
        shopId: formModel.roleCode === 'shop_owner' ? formModel.shopId : undefined,
        status: formModel.status,
      })
      message.success('保存成功')
    }
    dialogOpen.value = false
    void loadData()
  } catch {
    // error is handled by request interceptor
  }
}

const onDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户「${selectedRow.value?.realName}」吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteUserApi(selectedRow.value!.id)
        message.success('删除成功')
        dialogOpen.value = false
        void loadData()
      } catch {
        // error is handled by request interceptor
      }
    },
  })
}

const onResetPassword = () => {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置用户「${selectedRow.value?.realName}」的密码吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await resetUserPasswordApi(selectedRow.value!.id)
        Modal.success({
          title: '密码重置成功',
          content: `新密码为：${res.newPassword}`,
          okText: '确定',
        })
      } catch {
        // error is handled by request interceptor
      }
    },
  })
}

const mapStatusText = (status: number) => (status === 1 ? '正常' : '禁用')

const onSearch = () => {
  pagination.current = 1
  void loadData()
}

watch(
  () => filters.roleCode,
  () => {
    pagination.current = 1
    void loadData()
  }
)

watch(
  () => filters.status,
  () => {
    pagination.current = 1
    void loadData()
  }
)
</script>

<template>
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
    @first-column-click="onRowClick"
    @change="onTableChange"
  >
    <template #filters>
      <a-input
        v-model:value="filters.keyword"
        placeholder="搜索用户名/姓名/手机号"
        style="width: 200px"
        allow-clear
        @pressEnter="onSearch"
      />
      <a-select v-model:value="filters.roleCode" allow-clear placeholder="角色筛选" style="width: 140px">
        <a-select-option value="tenant_admin">租户管理员</a-select-option>
        <a-select-option value="shop_owner">门店店长</a-select-option>
      </a-select>
    </template>
    <template #actions>
      <a-button type="primary" @click="onCreate">新增</a-button>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="Number(text) === 1 ? 'green' : 'red'">
          {{ mapStatusText(Number(text)) }}
        </a-tag>
      </template>
    </template>
  </BaseTableList>

  <BaseEditDialog
    v-model:open="dialogOpen"
    :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
    :mode="dialogMode"
    :model="formModel"
    :rules="formRules"
    :show-delete="true"
    :extra-actions="[{ key: 'resetPassword', label: '重置密码', danger: false }]"
    @submit="submitDialog"
    @delete="onDelete"
    @reset="onDialogReset"
    @extra="onResetPassword"
  >
    <a-form-item v-if="isEditMode" label="用户名">
      <a-input :value="formModel.username" disabled />
    </a-form-item>
    <a-form-item v-else label="用户名" name="username">
      <template #label>
        <span class="required-label">用户名</span>
      </template>
      <a-input v-model:value="formModel.username" placeholder="请输入用户名" />
    </a-form-item>
    <a-form-item label="姓名" name="realName">
      <template #label>
        <span class="required-label">姓名</span>
      </template>
      <a-input v-model:value="formModel.realName" placeholder="请输入姓名" />
    </a-form-item>
    <a-form-item label="手机号">
      <a-input v-model:value="formModel.phone" placeholder="请输入手机号" />
    </a-form-item>
    <a-form-item label="角色">
      <a-select v-model:value="formModel.roleCode">
        <a-select-option value="tenant_admin">租户管理员</a-select-option>
        <a-select-option value="shop_owner">门店店长</a-select-option>
      </a-select>
    </a-form-item>
    <a-form-item v-if="formModel.roleCode === 'shop_owner'" label="关联门店">
      <a-tooltip :title="formModel.status === 0 ? '该用户已禁用，请先启用' : ''" placement="top">
        <a-select
          v-model:value="formModel.shopId"
          placeholder="请选择关联门店"
          :loading="shopLoading"
          :options="shopList.map((s) => ({ label: s.shopName, value: s.id }))"
          :disabled="formModel.status === 0"
          style="width: 100%"
          @click="formModel.status === 0 && message.warning('该用户已禁用，请先启用')"
        />
      </a-tooltip>
    </a-form-item>
    <a-form-item v-if="isEditMode" label="状态">
      <a-radio-group v-model:value="formModel.status">
        <a-radio :value="1">正常</a-radio>
        <a-radio :value="0">禁用</a-radio>
      </a-radio-group>
    </a-form-item>
  </BaseEditDialog>
</template>
