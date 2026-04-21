import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, type LoginResponse } from '@/api/modules/auth'
import { setToken, removeToken, getUserInfo, setUserInfo as saveUserInfo, removeUserInfo } from '@/utils/request'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const userInfo = ref<LoginResponse | null>(getUserInfo() as LoginResponse | null)

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => userInfo.value?.roleCode === 'super_admin')
  const isTenantAdmin = computed(() => userInfo.value?.roleCode === 'tenant_admin')
  const isShopOwner = computed(() => userInfo.value?.roleCode === 'shop_owner')

  async function login(username: string, password: string): Promise<void> {
    const data = await apiLogin({ username, password })

    token.value = data.token
    userInfo.value = data
    setToken(data.token)
    saveUserInfo(data as unknown as Record<string, unknown>)
  }

  function logout(): void {
    token.value = null
    userInfo.value = null
    removeToken()
    removeUserInfo()
  }

  function setUserInfo(info: LoginResponse): void {
    userInfo.value = info
    saveUserInfo(info as unknown as Record<string, unknown>)
  }

  function updateUserInfo(info: { nickname?: string }): void {
    if (userInfo.value) {
      userInfo.value = { ...userInfo.value, ...info }
      saveUserInfo(userInfo.value as unknown as Record<string, unknown>)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isSuperAdmin,
    isTenantAdmin,
    isShopOwner,
    login,
    logout,
    setUserInfo,
    updateUserInfo,
  }
})

