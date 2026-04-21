import { get, post, put } from '@/utils/request'
import type { PageResult } from '@/utils/request'

export interface InviteCodeResponse {
  inviteCode: string
  expireTime: string
  status: string
}

export interface InviteCodeDetailResponse {
  id: number
  inviteCode: string
  expireTime: string
  status: string
  createdAt: string
  usedTime: string | null
  usedUserId: number | null
  usedUsername: string | null
}

export interface InviteCodeListParams {
  status?: string
  page?: number
  pageSize?: number
}

export async function generateInviteCode(): Promise<InviteCodeResponse> {
  return post<InviteCodeResponse>('/invite-code/generate', {})
}

export async function getInviteCodeList(params: InviteCodeListParams): Promise<PageResult<InviteCodeDetailResponse>> {
  return get<PageResult<InviteCodeDetailResponse>>('/invite-code/list', { params })
}

export async function invalidateInviteCode(id: number): Promise<void> {
  await put(`/invite-code/invalidate/${id}`, {})
}

export async function getLatestInviteCode(): Promise<InviteCodeDetailResponse | null> {
  return get<InviteCodeDetailResponse | null>('/invite-code/latest')
}

export async function regenerateInviteCode(): Promise<InviteCodeResponse> {
  return post<InviteCodeResponse>('/invite-code/regenerate', {})
}
