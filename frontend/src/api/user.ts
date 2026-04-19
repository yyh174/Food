import type { PageResult } from '../types/common'
import type { UserItem, UserDetail, UserCreateForm, UserEditForm, UserListParams, UserCreateResponse, UserPasswordResetResponse } from '../types/user'
import { request } from '../utils/request'

export const getUserListApi = async (params: UserListParams): Promise<PageResult<UserItem>> => {
  const res = await request.get<PageResult<UserItem>>('/users', { params })
  return res.data.data
}

export const getUserDetailApi = async (id?: number): Promise<UserDetail> => {
  if (id) {
    const res = await request.get<UserDetail>(`/users/${id}`)
    return res.data.data
  }
  const res = await request.get<UserDetail>('/auth/current-user')
  return res.data.data
}

export const getCurrentUserApi = async (): Promise<UserDetail> => {
  const res = await request.get<UserDetail>('/auth/current-user')
  return res.data.data
}

export const createUserApi = async (data: UserCreateForm): Promise<UserCreateResponse> => {
  const res = await request.post<UserCreateResponse>('/users', data)
  return res.data.data
}

export const updateUserApi = async (id: number, data: Partial<UserEditForm>): Promise<void> => {
  await request.put(`/users/${id}`, data)
}

export const deleteUserApi = async (id: number): Promise<void> => {
  await request.delete(`/users/${id}`)
}

export const resetUserPasswordApi = async (id: number, newPassword?: string): Promise<UserPasswordResetResponse> => {
  const data = newPassword ? { newPassword } : {}
  const res = await request.post<UserPasswordResetResponse>(`/users/${id}/password/reset`, data)
  return res.data.data
}

export const changePasswordApi = async (params: { oldPassword: string; newPassword: string }): Promise<void> => {
  await request.put('/auth/password/change', params)
}

export const updateUserShopsApi = async (id: number, shopId?: number): Promise<void> => {
  await request.put(`/users/${id}/shops`, { shopId })
}
