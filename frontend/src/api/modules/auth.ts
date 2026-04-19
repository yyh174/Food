import { request } from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  tenantId: number | null
  tenantName: string | null
  roleCode: string
  roleName: string
  shopId: number | null
  shopName: string | null
}

export interface RegisterRequest {
  tenantId: number
  username: string
  roleCode: string
}

export interface ShopManagerRegisterRequest {
  username: string
  password: string
  inviteCode: string
}

export interface UserResponse {
  id: number
  username: string
  tenantId: number
  roleCode: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await request.post<LoginResponse>('/auth/login', data)
  return res.data.data
}

export async function register(data: RegisterRequest): Promise<UserResponse> {
  const res = await request.post<UserResponse>('/auth/register', data)
  return res.data.data
}

export async function shopManagerRegister(data: ShopManagerRegisterRequest): Promise<LoginResponse> {
  const res = await request.post<LoginResponse>('/auth/register/shop-manager', data)
  return res.data.data
}

export async function resetPassword(userId: number): Promise<void> {
  await request.put(`/auth/password/reset/${userId}`, {})
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await request.put('/auth/password/change', data)
}

export async function getCurrentUser(): Promise<LoginResponse> {
  const res = await request.get<LoginResponse>('/auth/current-user')
  return res.data.data
}
