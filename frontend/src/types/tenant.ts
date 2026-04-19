export interface TenantItem {
  id: number
  tenantCode: string
  tenantName: string
  logoUrl?: string
  contactName?: string
  contactPhone?: string
  status: number
  inviteCode?: string
  apiQuota?: number
  apiUsed?: number
  expireTime?: string
  shopCount: number
  userCount: number
  createdAt: string
  updatedAt?: string
}

export interface TenantDetail {
  id: number
  tenantCode: string
  tenantName: string
  logoUrl?: string
  status: number
  contactName?: string
  contactPhone?: string
  inviteCode?: string
  apiQuota?: number
  apiUsed?: number
  expireTime?: string
  createdAt: string
  updatedAt?: string
}

export interface TenantCreateForm {
  tenantName: string
  logoUrl?: string
  contactName?: string
  contactPhone?: string
  apiQuota?: number
  expireTime?: string
}

export interface TenantEditForm {
  tenantName: string
  logoUrl?: string
  contactName?: string
  contactPhone?: string
  apiQuota?: number
  expireTime?: string
  status: number
}

export interface TenantCreateResult {
  id: number
  tenantCode: string
  tenantName: string
  inviteCode: string
  status: number
}
