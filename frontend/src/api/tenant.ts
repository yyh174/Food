import type { PageResult } from '../utils/request'
import type { TenantItem, TenantDetail, TenantCreateForm, TenantEditForm, TenantCreateResult } from '../types/tenant'
import type { ShopItem } from '../types/shop'
import { get, post, put, del } from '../utils/request'

export interface TenantListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export interface TenantShopListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

// 获取租户列表
export async function getTenantList(params: TenantListParams = {}): Promise<PageResult<TenantItem>> {
  return get<PageResult<TenantItem>>('/tenants', { params })
}

// 获取租户详情
export async function getTenantDetail(id: number): Promise<TenantDetail> {
  return get<TenantDetail>(`/tenants/${id}`)
}

// 创建租户
export async function createTenant(data: TenantCreateForm): Promise<TenantCreateResult> {
  return post<TenantCreateResult>('/tenants', data)
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
  return post<string>(`/tenants/${id}/invite-code/regenerate`)
}

// 获取租户门店列表
export async function getTenantShops(tenantId: number, params: TenantShopListParams = {}): Promise<PageResult<ShopItem>> {
  return get<PageResult<ShopItem>>(`/tenants/${tenantId}/shops`, { params })
}
