import http from './http'

export interface RegApplicationVO {
  id: number
  applicantName: string
  phone: string | null
  email: string | null
  reason: string
  status: string
  reviewNote: string | null
  inviteCode: string | null
  reviewedBy: number | null
  reviewedAt: string | null
  createdAt: string
}

export default {
  submit: (data: { applicantName: string; phone?: string; email?: string; reason: string }) =>
    http.post('/api/registration-applications', data),
  list: (status?: string) =>
    http.get('/api/registration-applications', { params: status ? { status } : {} }),
  approve: (id: number, reviewNote?: string) =>
    http.put(`/api/registration-applications/${id}/approve`, { reviewNote }),
  reject: (id: number, reviewNote?: string) =>
    http.put(`/api/registration-applications/${id}/reject`, { reviewNote })
}
