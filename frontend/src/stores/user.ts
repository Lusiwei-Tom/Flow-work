import { defineStore } from 'pinia'
import authApi from '@/api/auth'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}

interface UserState {
  token: string
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('accessToken') || '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (s) => !!s.token,
    permissions: (s) => s.userInfo?.permissions || [],
    roles: (s) => s.userInfo?.roles || []
  },

  actions: {
    setAuth(accessToken: string, refreshToken: string) {
      this.token = accessToken
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
    },
    async fetchUserInfo() {
      const info = await authApi.me()
      this.userInfo = info
      return info
    },
    hasPermission(code: string): boolean {
      if (!this.userInfo) return false
      return this.roles.includes('admin') || this.userInfo.permissions.includes(code)
    },
    hasAnyPermission(codes: string[]): boolean {
      if (this.roles.includes('admin')) return true
      return codes.some((c) => this.permissions.includes(c))
    },
    clearAuth() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
    }
  }
})
