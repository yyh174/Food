export interface ShopItem {
  id: number
  shopCode: string
  shopName: string
  province?: string
  city?: string
  district?: string
  address?: string
  managerName?: string
  managerPhone?: string
  meituanId?: string
  eleId?: string
  status: number
  createdAt: string
  updatedAt?: string
}

export interface ShopCreateRequest {
  shopName: string
  province?: string
  city?: string
  district?: string
  address?: string
  managerName?: string
  managerPhone?: string
  meituanId?: string
  eleId?: string
}

export interface ShopUpdateRequest extends ShopCreateRequest {
  status: number
}

export interface ShopStatusRequest {
  status: number
}

export interface ShopCreateResponse {
  id: number
  shopCode: string
  shopName: string
  status: number
  createdAt: string
}
