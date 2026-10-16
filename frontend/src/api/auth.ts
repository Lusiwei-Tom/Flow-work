import http from './http'

export interface LoginPayload {
  username: string
  password: string
  deviceId?: string
}

const authApi = {
  login: (data: LoginPayload) => http.post('/api/auth/login', data),
  register: (data: any) => http.post('/api/auth/register', data),
  refresh: (refreshToken: string) =>
    http.post('/api/auth/refresh', { refreshToken }),
  logout: (refreshToken: string) => http.post('/api/auth/logout', { refreshToken }),
  me: () => http.get('/api/auth/me'),
  sendSms: (phone: string) => http.post('/api/auth/sms/code', { phone }),
  phoneLogin: (data: { phone: string; code: string }) =>
    http.post('/api/auth/login/phone', data)
}

export default authApi
