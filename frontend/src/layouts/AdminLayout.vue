<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  HomeOutlined,
  ApartmentOutlined,
  ShopOutlined,
  UserOutlined,
  MessageOutlined,
  FileTextOutlined,
  RobotOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)

const selectedKeys = computed(() => {
  // 根据角色确定可见的路由
  let routes: string[] = ['/dashboard']
  if (authStore.isSuperAdmin) {
    routes = ['/tenants']
  } else if (authStore.isTenantAdmin) {
    routes = ['/dashboard', '/tickets', '/shops', '/users', '/reviews', '/ai-assistant']
  } else if (authStore.isShopOwner) {
    routes = ['/dashboard', '/tickets', '/reviews', '/ai-assistant']
  }
  const matched = routes.find(key => route.path.startsWith(key))
  return matched ? [matched] : [routes[0]]
})

const username = computed(() => authStore.userInfo?.username || '管理员')
const roleName = computed(() => authStore.userInfo?.roleName || '')

const onMenuClick = ({ key }: { key: string }) => {
  router.push(key)
}

const goToProfile = () => {
  router.push('/profile')
}

const logout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <a-layout class="admin-layout">
    <a-layout-header class="admin-header">
      <div class="header-left">
        <div class="logo">餐饮智能分析系统</div>
        <a-button type="text" @click="collapsed = !collapsed">
          <MenuFoldOutlined v-if="!collapsed" />
          <MenuUnfoldOutlined v-else />
        </a-button>
      </div>
      <div class="header-right">
        <a-button type="text" class="header-btn" @click="goToProfile">{{ username }}</a-button>
        <span v-if="roleName" class="role-name">({{ roleName }})</span>
        <a-button v-if="authStore.isSuperAdmin" type="text" class="header-btn" @click="logout">退出</a-button>
      </div>
    </a-layout-header>
    <a-layout class="admin-body">
      <a-layout-sider v-model:collapsed="collapsed" collapsible :trigger="null" theme="light" width="220">
        <a-menu mode="inline" :selected-keys="selectedKeys" @click="onMenuClick">
          <!-- 超级管理员菜单 -->
          <a-menu-item v-if="authStore.isSuperAdmin" key="/tenants">
            <ApartmentOutlined />
            <span>租户管理</span>
          </a-menu-item>
          <!-- 租户管理员/门店管理员菜单 -->
          <template v-else>
            <a-menu-item key="/dashboard">
              <HomeOutlined />
              <span>首页</span>
            </a-menu-item>
            <a-menu-item v-if="authStore.isTenantAdmin" key="/shops">
              <ShopOutlined />
              <span>门店管理</span>
            </a-menu-item>
            <a-menu-item v-if="authStore.isTenantAdmin" key="/users">
              <UserOutlined />
              <span>用户管理</span>
            </a-menu-item>
            <a-menu-item v-if="authStore.isTenantAdmin || authStore.isShopOwner" key="/reviews">
              <MessageOutlined />
              <span>评价管理</span>
            </a-menu-item>
            <a-menu-item v-if="authStore.isTenantAdmin || authStore.isShopOwner" key="/tickets">
              <FileTextOutlined />
              <span>工单管理</span>
            </a-menu-item>
            <a-menu-item v-if="authStore.isTenantAdmin || authStore.isShopOwner" key="/ai-assistant">
              <RobotOutlined />
              <span>AI助手</span>
            </a-menu-item>
          </template>
        </a-menu>
      </a-layout-sider>
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.admin-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  height: 56px;
  padding: 0 16px;
  background: #ffffff;
  border-bottom: 1px solid #d0d7de;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo {
  font-size: 16px;
  font-weight: 600;
}

.header-btn {
  color: inherit;
  padding: 4px 12px;
  height: auto;
  font-size: 14px;
}

.role-name {
  margin-right: 12px;
  color: #666;
  font-size: 12px;
}

.admin-body {
  height: 100vh;
  padding-top: 56px;
  overflow: hidden;
}

:deep(.ant-layout-sider) {
  position: fixed;
  left: 0;
  top: 56px;
  bottom: 0;
  overflow-y: auto;
  z-index: 9;
}

.admin-content {
  margin-left: 220px;
  height: calc(100vh - 56px);
  overflow: auto;
  background: #f6f8fa;
  padding: 24px;
  box-sizing: border-box;
}

:deep(.admin-content > *) {
  max-width: 1600px;
  margin: 0 auto;
}

:deep(.ant-layout-sider-collapsed) + .admin-content {
  margin-left: 80px;
}
</style>
