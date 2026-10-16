import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const http: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 60000
})

// 请求拦截：附加 JWT
http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing = false
let pendingQueue: Array<(token: string) => void> = []

// 响应拦截：统一解包 + 401 刷新
http.interceptors.response.use(
  (response) => {
    const body = response.data
    // SSE / blob 直接放行
    if (response.config.responseType === 'blob' || typeof body === 'string') {
      return response
    }
    if (body.code === 0) {
      return body.data
    }
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message || 'biz error'))
  },
  async (error) => {
    const { response, config } = error
    if (response?.status === 401 && !config.url.includes('/auth/refresh') && !config._retried) {
      config._retried = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        redirectLogin()
        return Promise.reject(error)
      }
      if (refreshing) {
        return new Promise((resolve) => {
          pendingQueue.push((token) => {
            config.headers.Authorization = `Bearer ${token}`
            resolve(http(config))
          })
        })
      }
      refreshing = true
      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const data = res.data.data
        localStorage.setItem('accessToken', data.accessToken)
        localStorage.setItem('refreshToken', data.refreshToken)
        pendingQueue.forEach((cb) => cb(data.accessToken))
        pendingQueue = []
        config.headers.Authorization = `Bearer ${data.accessToken}`
        return http(config)
      } catch (e) {
        pendingQueue = []
        redirectLogin()
        return Promise.reject(e)
      } finally {
        refreshing = false
      }
    }
    const msg = response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

function redirectLogin() {
  const userStore = useUserStore()
  userStore.clearAuth()
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

/**
 * 响应拦截器已统一解包 {code,message,data} 并返回 data，
 * 因此对外类型按「直接返回业务数据」声明，调用处无需再 .data
 */
interface Http {
  get<T = any>(url: string, config?: any): Promise<T>
  post<T = any>(url: string, data?: any, config?: any): Promise<T>
  put<T = any>(url: string, data?: any, config?: any): Promise<T>
  patch<T = any>(url: string, data?: any, config?: any): Promise<T>
  delete<T = any>(url: string, config?: any): Promise<T>
  request<T = any>(config: any): Promise<T>
}

export default http as unknown as Http
