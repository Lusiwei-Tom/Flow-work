import http from './http'

const analyticsApi = {
  dashboard: (projectId: number) =>
    http.get(`/api/projects/${projectId}/analytics/dashboard`),
  members: (projectId: number) =>
    http.get(`/api/projects/${projectId}/analytics/members`)
}

export default analyticsApi
