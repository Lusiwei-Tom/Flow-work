import http from './http'

const aiApi = {
  chat: (projectId: number, message: string) =>
    http.post(`/api/projects/${projectId}/ai/chat`, { message }),
  history: (projectId: number, limit = 50) =>
    http.get(`/api/projects/${projectId}/ai/chat/history`, { params: { limit } }),
  clearHistory: (projectId: number) =>
    http.delete(`/api/projects/${projectId}/ai/chat/history`),
  chatStreamUrl: (projectId: number) =>
    `/api/projects/${projectId}/ai/chat/stream`,
  knowledgeQaStreamUrl: (projectId: number) =>
    `/api/projects/${projectId}/ai/knowledge/qa/stream`,
  breakdown: (projectId: number, requirement: string) =>
    http.post(`/api/projects/${projectId}/ai/tasks/breakdown`, { requirement }),
  parseMeeting: (projectId: number, meetingText: string) =>
    http.post(`/api/projects/${projectId}/ai/meeting/parse`, { meetingText }),
  analyzeRisk: (projectId: number) =>
    http.post(`/api/projects/${projectId}/ai/risk/analyze`),
  weeklyReport: (projectId: number) =>
    http.post(`/api/projects/${projectId}/ai/reports/weekly`),
  reports: (projectId: number) =>
    http.get(`/api/projects/${projectId}/ai/reports`)
}

export default aiApi
