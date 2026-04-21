import type { PageResult } from '../utils/request'
import type { UserItem, UserDetail, UserCreateForm, UserEditForm, UserListParams, UserCreateResponse, UserPasswordResetResponse } from '../types/user'
import { get, post, put } from '../utils/request'

export const getUserListApi = async (params: UserListParams): Promise<PageResult<UserItem>> => {
  return get<PageResult<UserItem>>('/users', { params })
}

export const getUserDetailApi = async (id?: number): Promise<UserDetail> => {
  if (id) {
    return get<UserDetail>(`/users/${id}`)
  }
  return get<UserDetail>('/auth/current-user')
}

export const getCurrentUserApi = async (): Promise<UserDetail> => {
  return get<UserDetail>('/auth/current-user')
}

export const createUserApi = async (data: UserCreateForm): Promise<UserCreateResponse> => {
  return post<UserCreateResponse>('/users', data)
}

export const updateUserApi = async (id: number, data: Partial<UserEditForm>): Promise<void> => {
  await put(`/users/${id}`, data)
}

export const deleteUserApi = async (id: number): Promise<void> => {
  await put(`/users/${id}`, {})
}

export const resetUserPasswordApi = async (id: number, newPassword?: string): Promise<UserPasswordResetResponse> => {
  const data = newPassword ? { newPassword } : {}
  return post<UserPasswordResetResponse>(`/users/${id}/password/reset`, data)
}

export const changePasswordApi = async (params: { oldPassword: string; newPassword: string }): Promise<void> => {
  await put('/auth/password/change', params)
}

export const updateUserShopsApi = async (id: number, shopId?: number): Promise<void> => {
  await put(`/users/${id}/shops`, { shopId })
}
