import { get, post, put } from '@/utils/request'

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
  return post<LoginResponse>('/auth/login', data)
}

export async function register(data: RegisterRequest): Promise<UserResponse> {
  return post<UserResponse>('/auth/register', data)
}

export async function shopManagerRegister(data: ShopManagerRegisterRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/auth/register/shop-manager', data)
}

export async function resetPassword(userId: number): Promise<void> {
  await put(`/auth/password/reset/${userId}`, {})
}

export async function changePassword(data: ChangePasswordRequest): Promise<void> {
  await put('/auth/password/change', data)
}

export async function getCurrentUser(): Promise<LoginResponse> {
  return get<LoginResponse>('/auth/current-user')
}
