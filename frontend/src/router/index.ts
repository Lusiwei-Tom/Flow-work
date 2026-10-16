import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { public: true, title: '登录' }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { public: true, title: '注册' }
    },
    {
      path: '/register-application',
      name: 'RegisterApplication',
      component: () => import('@/views/auth/RegisterApplicationView.vue'),
      meta: { public: true, title: '申请注册' }
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      redirect: '/projects',
      children: [
        {
          path: 'projects',
          name: 'ProjectList',
          component: () => import('@/views/project/ProjectListView.vue'),
          meta: { title: '我的项目' }
        },
        {
          path: 'my-tasks',
          name: 'MyTasks',
          component: () => import('@/views/task/MyTasksView.vue'),
          meta: { title: '我的任务' }
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '我的角色' }
        },
        {
          path: 'admin/invite-codes',
          name: 'InviteCodes',
          component: () => import('@/views/admin/InviteCodeView.vue'),
          meta: { title: '邀请码管理', adminOnly: true }
        },
        {
          path: 'admin/applications',
          name: 'Applications',
          component: () => import('@/views/admin/ApplicationReviewView.vue'),
          meta: { title: '注册审批', adminOnly: true }
        },
        {
          path: 'projects/:id/board',
          name: 'Board',
          component: () => import('@/views/board/BoardView.vue'),
          meta: { title: '任务看板' }
        },
        {
          path: 'projects/:id/ai',
          name: 'AiAssistant',
          component: () => import('@/views/ai/AiAssistantView.vue'),
          meta: { title: 'AI 项目管家' }
        },
        {
          path: 'projects/:id/ai-tools',
          name: 'AiTools',
          component: () => import('@/views/ai/AiToolsView.vue'),
          meta: { title: 'AI 智能工具' }
        },
        {
          path: 'projects/:id/knowledge',
          name: 'Knowledge',
          component: () => import('@/views/knowledge/KnowledgeView.vue'),
          meta: { title: '项目知识库' }
        },
        {
          path: 'projects/:id/analytics',
          name: 'Analytics',
          component: () => import('@/views/analytics/AnalyticsView.vue'),
          meta: { title: '数据看板' }
        }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/projects' }
  ]
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  if (to.meta.public) return true
  if (!userStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      return { path: '/login' }
    }
  }
  if (to.meta.adminOnly && !userStore.roles.includes('admin')) {
    return { path: '/projects' }
  }
  document.title = `${to.meta.title || ''} · FlowWork`
  return true
})

export default router
