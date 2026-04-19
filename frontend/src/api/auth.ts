export interface LoginPayload {
  username: string
  password: string
}

export const loginApi = async (payload: LoginPayload) => {
  await Promise.resolve()
  return {
    code: 200,
    message: '登录成功',
    data: {
      token: `mock-token-${Date.now()}`,
      username: payload.username,
    },
  }
}
