import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import router from '@/router'

const TOKEN_KEY = 'auth_token'
const USER_INFO_KEY = 'user_info'

export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUserInfo(): Record<string, unknown> | null {
  const userInfo = localStorage.getItem(USER_INFO_KEY)
  return userInfo ? JSON.parse(userInfo) : null
}

export function setUserInfo(userInfo: Record<string, unknown>): void {
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export function removeUserInfo(): void {
  localStorage.removeItem(USER_INFO_KEY)
}

function createRequest(): AxiosInstance {
  const instance = axios.create({
    baseURL: '/api',
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  })

  instance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = getToken()
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => {
      return Promise.reject(error)
    }
  )

  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { code, message: msg } = response.data

      if (code === 200 || code === 0) {
        return response
      }

      if (code === 401) {
        message.error(msg || '登录已过期，请重新登录')
        removeToken()
        removeUserInfo()
        router.push('/login')
        return Promise.reject(new Error(msg || '未登录'))
      }

      if (code === 403) {
        message.error(msg || '无权限访问')
        return Promise.reject(new Error(msg || '无权限访问'))
      }

      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    },
    (error) => {
      if (error.response) {
        const { status, data } = error.response
        if (status === 401) {
          message.error('登录已过期，请重新登录')
          removeToken()
          removeUserInfo()
          router.push('/login')
        } else if (status === 403) {
          message.error('无权限访问')
        } else if (status === 404) {
          message.error('资源不存在')
        } else if (status >= 500) {
          message.error('服务器错误')
        } else {
          message.error(data?.message || '请求失败')
        }
      } else if (error.request) {
        message.error('网络错误，请检查网络连接')
      } else {
        message.error('请求配置错误')
      }
      return Promise.reject(error)
    }
  )

  return instance
}

export const request = createRequest()

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return request.get(url, config)
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return request.post(url, data, config)
}

export function put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return request.put(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return request.delete(url, config)
}
