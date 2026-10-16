import http from './http'

export interface InviteCodeVO {
  id: number
  code: string
  presetRole: string
  presetProjectId: number | null
  presetProjectRole: string | null
  maxUses: number
  usedCount: number
  expiresAt: string | null
  createdBy: number
  status: string
  createdAt: string
}

export interface CreateInviteCodeReq {
  presetRole: string
  presetProjectId?: number | null
  presetProjectRole?: string | null
  maxUses?: number
  expiresAt?: string | null
}

export default {
  create: (data: CreateInviteCodeReq) => http.post('/api/invite-codes', data),
  list: () => http.get('/api/invite-codes'),
  disable: (id: number) => http.put(`/api/invite-codes/${id}/disable`),
  delete: (id: number) => http.delete(`/api/invite-codes/${id}`)
}
