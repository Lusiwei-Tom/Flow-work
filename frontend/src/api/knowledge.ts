import http from './http'

export interface KnowledgeDoc {
  id: number
  projectId: number
  title: string
  fileName: string
  status: string
  chunkCount?: number
  sizeBytes: number
  errorMsg?: string
  createdAt: string
}

const knowledgeApi = {
  list: (projectId: number) =>
    http.get(`/api/projects/${projectId}/knowledge/docs`),
  upload: (projectId: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post(`/api/projects/${projectId}/knowledge/docs`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  },
  remove: (projectId: number, docId: number) =>
    http.delete(`/api/projects/${projectId}/knowledge/docs/${docId}`),
  ask: (projectId: number, question: string) =>
    http.post(`/api/projects/${projectId}/ai/knowledge/qa`, { question })
}

export default knowledgeApi
