import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/auth/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/auth/RegisterView.vue'),
      meta: { title: '注册' },
    },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/dashboard/HomeView.vue'),
          meta: { title: '首页', menuKey: '/dashboard' },
        },
        {
          path: 'tenants',
          name: 'tenants',
          component: () => import('../views/tenant/TenantListView.vue'),
          meta: { title: '租户管理', menuKey: '/tenants' },
        },
        {
          path: 'shops',
          name: 'shops',
          component: () => import('../views/shop/ShopListView.vue'),
          meta: { title: '门店管理', menuKey: '/shops' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('../views/user/UserListView.vue'),
          meta: { title: '用户管理', menuKey: '/users' },
        },
        {
          path: 'reviews',
          name: 'reviews',
          component: () => import('../views/review/ReviewListView.vue'),
          meta: { title: '评价管理', menuKey: '/reviews', roles: ['tenant_admin', 'shop_owner'] },
        },
        {
          path: 'tickets',
          name: 'tickets',
          component: () => import('../views/ticket/TicketListView.vue'),
          meta: { title: '工单管理', menuKey: '/tickets', roles: ['tenant_admin', 'shop_owner'] },
        },
        {
          path: 'tickets/:id',
          name: 'ticket-detail',
          component: () => import('../views/ticket/TicketDetailView.vue'),
          meta: { title: '工单详情', roles: ['tenant_admin', 'shop_owner'] },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('../views/profile/ProfileView.vue'),
          meta: { title: '个人中心' },
        },
        {
          path: 'ai-assistant',
          name: 'ai-assistant',
          component: () => import('../views/ai/AiAssistantView.vue'),
          meta: { title: 'AI助手', menuKey: '/ai-assistant', roles: ['tenant_admin', 'shop_owner'] },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.path === '/login' || to.path === '/register') {
    return true
  }
  const token = localStorage.getItem('auth_token')
  if (!token) {
    return '/login'
  }

  // 根据角色限制访问
  const userInfo = localStorage.getItem('user_info')
  if (userInfo) {
    const user = JSON.parse(userInfo)
    // 超级管理员只能访问租户管理相关页面
    if (user.roleCode === 'super_admin') {
      const allowedPaths = ['/tenants', '/login']
      if (!allowedPaths.some(path => to.path.startsWith(path))) {
        return '/tenants'
      }
    }
    // 租户管理员和门店店长只能访问评价管理页面
    if (user.roleCode === 'tenant_admin' || user.roleCode === 'shop_owner') {
      const meta = to.meta as { roles?: string[] }
      if (meta?.roles && !meta.roles.includes(user.roleCode)) {
        return '/dashboard'
      }
    }
  }

  return true
})

export default router
