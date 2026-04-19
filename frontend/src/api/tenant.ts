import type { PageResult } from '../types/common'
import type { TenantItem, TenantDetail, TenantCreateForm, TenantEditForm, TenantCreateResult } from '../types/tenant'
import type { ShopListItem } from '../types/shop'
import { get, post, put, del } from '../utils/request'

export interface TenantListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export interface ShopListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

// 获取租户列表
export async function getTenantList(params: TenantListParams = {}): Promise<PageResult<TenantItem>> {
  const res = await get<PageResult<TenantItem>>('/tenants', { params })
  return res.data.data
}

// 获取租户详情
export async function getTenantDetail(id: number): Promise<TenantDetail> {
  const res = await get<TenantDetail>(`/tenants/${id}`)
  return res.data.data
}

// 创建租户
export async function createTenant(data: TenantCreateForm): Promise<TenantCreateResult> {
  const res = await post<TenantCreateResult>('/tenants', data)
  return res.data.data
}

// 更新租户
export async function updateTenant(id: number, data: TenantEditForm): Promise<void> {
  await put(`/tenants/${id}`, data)
}

// 删除租户
export async function deleteTenant(id: number): Promise<void> {
  await del(`/tenants/${id}`)
}

// 重置邀请码
export async function regenerateInviteCode(id: number): Promise<string> {
  const res = await post<string>(`/tenants/${id}/invite-code/regenerate`)
  return res.data.data
}

// 获取租户门店列表
export async function getTenantShops(tenantId: number, params: ShopListParams = {}): Promise<PageResult<ShopListItem>> {
  const res = await get<PageResult<ShopListItem>>(`/tenants/${tenantId}/shops`, { params })
  return res.data.data
}
