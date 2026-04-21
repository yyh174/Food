export interface UserItem {
  id: number
  username: string
  nickname?: string
  realName: string
  phone?: string
  email?: string
  roleCode: string
  roleName: string
  shopId?: number
  shopName?: string
  tenantName?: string
  status: number
  lastLoginTime?: string
  createdAt: string
}

export interface UserDetail extends UserItem {
  updatedAt: string
}

export interface UserCreateForm {
  username: string
  realName: string
  phone?: string
  roleCode: string
  shopId?: number
  password?: string
}

export interface UserEditForm {
  nickname?: string
  realName?: string
  phone?: string
  email?: string
  roleCode?: string
  shopId?: number
  status?: number
}

export interface UserListParams {
  page?: number
  pageSize?: number
  keyword?: string
  roleCode?: string
  status?: number
}

export interface UserCreateResponse {
  id: number
  username: string
  realName: string
  roleCode: string
  status: number
  createdAt: string
}

export interface UserPasswordResetResponse {
  newPassword: string
}
