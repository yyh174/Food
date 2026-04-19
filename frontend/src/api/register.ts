import { post } from '@/utils/request'

export interface RegisterPayload {
  username: string
  password: string
  inviteCode?: string
}

export const registerApi = async (payload: RegisterPayload) => {
  const res = await post('/auth/register', payload)
  return res.data.data
}
