import http from './http'

export default {
  profile: () => http.get('/api/my/profile')
}
