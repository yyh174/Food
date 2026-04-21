import type { PageResult } from '../utils/request'
import type {
  ShopItem,
  ShopCreateRequest,
  ShopUpdateRequest,
  ShopStatusRequest,
  ShopCreateResponse,
} from '../types/shop'
import { get, post, put, del } from '../utils/request'

export interface ShopListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export const getShopListApi = async (params: ShopListParams): Promise<PageResult<ShopItem>> => {
  return get<PageResult<ShopItem>>('/shops', { params })
}

export const getShopDetailApi = async (id: number): Promise<ShopItem> => {
  return get<ShopItem>(`/shops/${id}`)
}

/**
 * 获取可分配的门店列表（排除已被其他店长分配的门店）
 */
export const getAvailableShopListApi = async (): Promise<ShopItem[]> => {
  return get<ShopItem[]>('/shops/available')
}

/**
 * 获取门店筛选选项（用于列表筛选，返回所有营业中门店）
 */
export const getShopSelectOptionsApi = async (): Promise<ShopItem[]> => {
  return get<ShopItem[]>('/shops/select-options')
}

export const createShopApi = async (data: ShopCreateRequest): Promise<ShopCreateResponse> => {
  return post<ShopCreateResponse>('/shops', data)
}

export const updateShopApi = async (id: number, data: ShopUpdateRequest): Promise<void> => {
  await put(`/shops/${id}`, data)
}

export const deleteShopApi = async (id: number): Promise<void> => {
  await del(`/shops/${id}`)
}

export const updateShopStatusApi = async (id: number, data: ShopStatusRequest): Promise<void> => {
  await put(`/shops/${id}/status`, data)
}
