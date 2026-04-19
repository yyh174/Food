import type { PageResult } from './common'
import type {
  ShopItem,
  ShopCreateRequest,
  ShopUpdateRequest,
  ShopStatusRequest,
  ShopCreateResponse,
} from './shop'
import { get, post, put, del } from '../utils/request'

export interface ShopListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

export const getShopListApi = async (params: ShopListParams): Promise<PageResult<ShopItem>> => {
  const res = await get<PageResult<ShopItem>>('/shops', { params })
  return res.data.data
}

export const getShopDetailApi = async (id: number): Promise<ShopItem> => {
  const res = await get<ShopItem>(`/shops/${id}`)
  return res.data.data
}

/**
 * 获取可分配的门店列表（排除已被其他店长分配的门店）
 */
export const getAvailableShopListApi = async (): Promise<ShopItem[]> => {
  const res = await get<ShopItem[]>('/shops/available')
  return res.data.data
}

export const createShopApi = async (data: ShopCreateRequest): Promise<ShopCreateResponse> => {
  const res = await post<ShopCreateResponse>('/shops', data)
  return res.data.data
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
