<script setup lang="ts">
import { reactive, ref, computed, onMounted, h } from 'vue'
import { message, Modal, type TableColumnType, Descriptions } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form/interface'
import dayjs from 'dayjs'
import BaseEditDialog from '../../components/base/BaseEditDialog.vue'
import { getTenantList, getTenantDetail, createTenant, updateTenant, deleteTenant, regenerateInviteCode, getTenantShops, type TenantListParams } from '../../api/tenant'
import type { TenantItem, TenantCreateForm, TenantEditForm } from '../../types/tenant'
import type { ShopItem } from '../../types/shop'

const loading = ref(false)
const shopLoading = ref(false)
const dialogOpen = ref(false)
const shopDetailOpen = ref(false)
const shopDetail = ref<ShopItem | null>(null)
const dialogMode = ref<'create' | 'edit'>('create')
const selectedTenant = ref<TenantItem | null>(null)
const tenantList = ref<TenantItem[]>([])
const shopList = ref<ShopItem[]>([])
const shopTotal = ref(0)
const shopParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
})

const filters = reactive({
  keyword: '',
})

const formModel = reactive<TenantCreateForm & { status: number }>({
  tenantName: '',
  logoUrl: '',
  contactName: '',
  contactPhone: '',
  apiQuota: 1000,
  expireTime: null as string | dayjs.Dayjs | null,
  status: 1,
})

const createFormRules: Record<string, Rule[]> = {
  tenantName: [{ required: true, message: '请输入租户名称' }],
}

const editFormRules: Record<string, Rule[]> = {
  tenantName: [{ required: true, message: '请输入租户名称' }],
  status: [{ required: true, message: '请选择状态' }],
}

const shopColumns: TableColumnType[] = [
  { title: '门店编码', dataIndex: 'shopCode' },
  {
    title: '门店名称',
    dataIndex: 'shopName',
    customRender: ({ text, record }: { text: string; record: Record<string, unknown> }) => {
      return h(
        'a',
        {
          class: 'base-table-link',
          onClick: (e: Event) => {
            e.stopPropagation()
            onShopView(record)
          },
        },
        text,
      )
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    customRender: ({ text }: { text: number }) => {
      const map: Record<number, string> = { 0: '已停业', 1: '营业中', 2: '休息中' }
      return map[text] || '-'
    },
  },
  { title: '店长', dataIndex: 'managerName' },
  { title: '创建时间', dataIndex: 'createdAt' },
]

const loadTenantList = async () => {
  loading.value = true
  try {
    const params: TenantListParams = {}
    if (filters.keyword) {
      params.keyword = filters.keyword
    }
    const res = await getTenantList(params)
    tenantList.value = res.records
  } catch {
    message.error('加载租户列表失败')
  } finally {
    loading.value = false
  }
}

const loadShopList = async () => {
  if (!selectedTenant.value) return
  shopLoading.value = true
  try {
    const res = await getTenantShops(selectedTenant.value.id, shopParams)
    shopList.value = res.records
    shopTotal.value = res.total
  } catch {
    message.error('加载门店列表失败')
  } finally {
    shopLoading.value = false
  }
}

const onTenantSelect = async (tenant: TenantItem) => {
  selectedTenant.value = tenant
  shopParams.page = 1
  shopParams.keyword = ''
  shopParams.status = undefined
  await loadShopList()
}

const onCreate = () => {
  dialogMode.value = 'create'
  Object.assign(formModel, {
    tenantName: '',
    logoUrl: '',
    contactName: '',
    contactPhone: '',
    apiQuota: 1000,
    expireTime: null,
    status: 1,
  })
  dialogOpen.value = true
}

const onEdit = async () => {
  if (!selectedTenant.value) return
  dialogMode.value = 'edit'
  try {
    const detail = await getTenantDetail(selectedTenant.value.id)
    Object.assign(formModel, {
      tenantName: detail.tenantName,
      logoUrl: detail.logoUrl || '',
      contactName: detail.contactName || '',
      contactPhone: detail.contactPhone || '',
      apiQuota: detail.apiQuota || 1000,
      expireTime: detail.expireTime ? dayjs(detail.expireTime) : null,
      status: detail.status,
    })
    dialogOpen.value = true
  } catch {
    message.error('加载租户详情失败')
  }
}

const onDialogReset = () => {
  if (dialogMode.value === 'create') {
    Object.assign(formModel, {
      tenantName: '',
      logoUrl: '',
      contactName: '',
      contactPhone: '',
      apiQuota: 1000,
      expireTime: null,
      status: 1,
    })
  }
}

const submitDialog = async () => {
  try {
    // 处理到期时间：dayjs对象或字符串都转为后端格式
    const formatExpireTime = (val: string | dayjs.Dayjs | null): string | undefined => {
      if (!val) return undefined
      if (dayjs.isDayjs(val)) {
        return val.format('YYYY-MM-DD HH:mm:ss')
      }
      return val.replace('T', ' ') + ':00'
    }

    if (dialogMode.value === 'create') {
      const data: TenantCreateForm = {
        tenantName: formModel.tenantName,
        logoUrl: formModel.logoUrl,
        contactName: formModel.contactName,
        contactPhone: formModel.contactPhone,
        apiQuota: formModel.apiQuota,
        expireTime: formatExpireTime(formModel.expireTime),
      }
      const result = await createTenant(data)
      message.success(`创建成功，默认管理员账号：${result.tenantCode}_admin`)
      dialogOpen.value = false
      await loadTenantList()
      // 选中新创建的租户
      const newTenant = tenantList.value.find(t => t.id === result.id)
      if (newTenant) {
        await onTenantSelect(newTenant)
      }
    } else {
      const data: TenantEditForm = {
        tenantName: formModel.tenantName,
        logoUrl: formModel.logoUrl,
        contactName: formModel.contactName,
        contactPhone: formModel.contactPhone,
        apiQuota: formModel.apiQuota,
        expireTime: formatExpireTime(formModel.expireTime),
        status: formModel.status,
      }
      await updateTenant(selectedTenant.value!.id, data)
      message.success('保存成功')
      dialogOpen.value = false
      await loadTenantList()
      // 刷新门店列表
      if (selectedTenant.value) {
        const updated = tenantList.value.find(t => t.id === selectedTenant.value!.id)
        if (updated) {
          selectedTenant.value = updated
        }
      }
    }
  } catch {
    // 错误已在拦截器处理
  }
}

const onDelete = () => {
  if (!selectedTenant.value) return
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除租户"${selectedTenant.value.tenantName}"吗？删除后该租户下所有数据保留，但用户无法登录。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await deleteTenant(selectedTenant.value.id)
        message.success('删除成功')
        dialogOpen.value = false  // 关闭编辑弹窗
        selectedTenant.value = null
        shopList.value = []
        shopTotal.value = 0
        await loadTenantList()
      } catch {
        // 错误已在拦截器处理
      }
    },
  })
}

const onResetInviteCode = () => {
  if (!selectedTenant.value) return
  Modal.confirm({
    title: '确认重置',
    content: '确定要重置该租户的邀请码吗？原邀请码将立即失效。',
    okText: '确认重置',
    onOk: async () => {
      try {
        const newCode = await regenerateInviteCode(selectedTenant.value!.id)
        message.success(`重置成功，新邀请码：${newCode}`)
        await loadTenantList()
        if (selectedTenant.value) {
          const updated = tenantList.value.find(t => t.id === selectedTenant.value!.id)
          if (updated) {
            selectedTenant.value = updated
          }
        }
      } catch {
        // 错误已在拦截器处理
      }
    },
  })
}

const onShopPageChange = (page: number, pageSize: number) => {
  shopParams.page = page
  shopParams.pageSize = pageSize
  loadShopList()
}

const onShopFilter = () => {
  shopParams.page = 1
  loadShopList()
}

// 查看门店详情（只读，不能编辑/删除）
const onShopView = async (record: Record<string, unknown>) => {
  try {
    const detail = await getTenantShops(selectedTenant.value!.id, { page: 1, pageSize: 100, keyword: record.shopCode as string })
    const found = detail.records.find((s: ShopListItem) => s.id === record.id)
    if (found) {
      shopDetail.value = found
      shopDetailOpen.value = true
    }
  } catch {
    message.error('加载门店详情失败')
  }
}

onMounted(() => {
  loadTenantList()
})
</script>

<template>
  <div class="tenant-layout">
    <!-- 左侧租户列表 -->
    <div class="tenant-sidebar">
      <div class="sidebar-header">
        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索租户"
          style="width: 100%"
          @search="loadTenantList"
        />
        <a-button type="primary" @click="onCreate">+</a-button>
      </div>
      <div class="tenant-list">
        <div
          v-for="tenant in tenantList"
          :key="tenant.id"
          class="tenant-item"
          :class="{ active: selectedTenant?.id === tenant.id }"
          @click="onTenantSelect(tenant)"
        >
          <div class="tenant-name">{{ tenant.tenantName }}</div>
          <div class="tenant-code">{{ tenant.tenantCode }}</div>
        </div>
        <a-empty v-if="tenantList.length === 0 && !loading" description="暂无租户" />
      </div>
    </div>

    <!-- 右侧门店列表 -->
    <div class="shop-panel">
      <template v-if="selectedTenant">
        <div class="panel-header">
          <div class="header-left">
            <a-select
              v-model:value="shopParams.status"
              allow-clear
              placeholder="状态筛选"
              style="width: 120px"
              @change="onShopFilter"
            >
              <a-select-option :value="1">营业中</a-select-option>
              <a-select-option :value="2">休息中</a-select-option>
              <a-select-option :value="0">已停业</a-select-option>
            </a-select>
            <a-input-search
              v-model:value="shopParams.keyword"
              placeholder="搜索门店"
              style="width: 200px"
              @search="onShopFilter"
            />
          </div>
          <div class="header-right">
            <a-button @click="onResetInviteCode">重置邀请码</a-button>
            <a-button type="primary" @click="onEdit">编辑租户</a-button>
          </div>
        </div>
        <a-table
          :columns="shopColumns"
          :data-source="shopList"
          :loading="shopLoading"
          :pagination="{
            current: shopParams.page,
            pageSize: shopParams.pageSize,
            total: shopTotal,
            showSizeChanger: true,
            showTotal: (total: number) => `共 ${total} 条`,
          }"
          row-key="id"
          size="middle"
          @change="(pagination: { current: number; pageSize: number }) => onShopPageChange(pagination.current, pagination.pageSize)"
        />
      </template>
      <div v-else class="placeholder">
        <span>请选择租户</span>
      </div>
    </div>
  </div>

  <!-- 新增/编辑弹窗 -->
  <BaseEditDialog
    v-model:open="dialogOpen"
    title="租户"
    :mode="dialogMode"
    :model="formModel"
    :rules="dialogMode === 'create' ? createFormRules : editFormRules"
    :show-delete="dialogMode === 'edit'"
    :width="520"
    @submit="submitDialog"
    @delete="onDelete"
    @reset="onDialogReset"
  >
    <a-form-item label="租户名称" name="tenantName">
      <template #label>
        <span class="required-label">租户名称</span>
      </template>
      <a-input v-model:value="formModel.tenantName" placeholder="请输入租户名称" />
    </a-form-item>
    <a-form-item label="品牌Logo" name="logoUrl">
      <a-input v-model:value="formModel.logoUrl" placeholder="请输入Logo URL" />
    </a-form-item>
    <a-form-item label="联系人" name="contactName">
      <a-input v-model:value="formModel.contactName" placeholder="请输入联系人" />
    </a-form-item>
    <a-form-item label="联系电话" name="contactPhone">
      <a-input v-model:value="formModel.contactPhone" placeholder="请输入联系电话" />
    </a-form-item>
    <a-form-item label="API配额" name="apiQuota">
      <a-input-number v-model:value="formModel.apiQuota" :min="0" style="width: 100%" />
    </a-form-item>
    <a-form-item label="到期时间" name="expireTime">
      <a-date-picker
        v-model:value="formModel.expireTime"
        show-time
        format="YYYY-MM-DD HH:mm:ss"
        style="width: 100%"
        placeholder="选择到期时间"
        :disabled-date="(current: dayjs.Dayjs) => current && current < dayjs().startOf('day')"
        :show-time="{ defaultValue: dayjs('23:59:59', 'HH:mm:ss') }"
      />
    </a-form-item>
    <a-form-item v-if="dialogMode === 'edit'" label="状态" name="status">
      <template #label>
        <span class="required-label">状态</span>
      </template>
      <a-radio-group v-model:value="formModel.status">
        <a-radio :value="1">正常</a-radio>
        <a-radio :value="0">禁用</a-radio>
      </a-radio-group>
    </a-form-item>
  </BaseEditDialog>

  <!-- 门店详情弹窗（只读） -->
  <a-modal
    v-model:open="shopDetailOpen"
    title="门店详情"
    :footer="null"
    :width="520"
  >
    <a-descriptions :column="1" bordered size="small">
      <a-descriptions-item label="门店编码">{{ shopDetail?.shopCode }}</a-descriptions-item>
      <a-descriptions-item label="门店名称">{{ shopDetail?.shopName }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="shopDetail?.status === 1 ? 'green' : shopDetail?.status === 2 ? 'orange' : 'red'">
          {{ shopDetail?.status === 1 ? '营业中' : shopDetail?.status === 2 ? '休息中' : '已停业' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="所在地区">
        {{ [shopDetail?.province, shopDetail?.city, shopDetail?.district].filter(Boolean).join('') || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="详细地址">{{ shopDetail?.address || '-' }}</a-descriptions-item>
      <a-descriptions-item label="店长姓名">{{ shopDetail?.managerName || '-' }}</a-descriptions-item>
      <a-descriptions-item label="店长电话">{{ shopDetail?.managerPhone || '-' }}</a-descriptions-item>
      <a-descriptions-item label="美团门店ID">{{ shopDetail?.meituanId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="饿了么门店ID">{{ shopDetail?.eleId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ shopDetail?.createdAt || '-' }}</a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<style scoped>
.tenant-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
}

.tenant-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.tenant-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tenant-item {
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;
}

.tenant-item:hover {
  background: #f6f8fa;
}

.tenant-item.active {
  background: #e6f7ff;
  border: 1px solid #1890ff;
}

.tenant-name {
  font-weight: 500;
  color: #24292f;
  margin-bottom: 4px;
}

.tenant-code {
  font-size: 12px;
  color: #8c8c8c;
}

.shop-panel {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left,
.header-right {
  display: flex;
  gap: 8px;
  align-items: center;
}

.placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
  font-size: 14px;
}
</style>
