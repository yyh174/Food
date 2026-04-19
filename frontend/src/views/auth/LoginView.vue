<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

console.log('LoginView mounted')

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const goRegister = () => {
  console.log('goRegister called')
  import('ant-design-vue').then(({ Modal }) => {
    Modal.confirm({
      title: '选择注册类型',
      content: '请选择您要注册的类型',
      okText: '租户注册',
      cancelText: '员工注册',
      onOk: () => {
        router.push({ path: '/register', query: { type: 'tenant' } })
      },
      onCancel: () => {
        router.push({ path: '/register', query: { type: 'manager' } })
      },
    })
  })
}

const formState = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
}

const submit = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    await authStore.login(formState.username, formState.password)
    // 根据角色跳转到对应页面
    if (authStore.isSuperAdmin) {
      router.push('/tenants')
    } else {
      router.push('/dashboard')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <a-card title="系统登录" class="login-card">
      <a-form ref="formRef" :model="formState" :rules="rules" :label-col="{ style: { width: '100px' } }">
        <a-form-item label="用户名" name="username">
          <template #label>
            <span class="required-label">用户名</span>
          </template>
          <a-input v-model:value="formState.username" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <template #label>
            <span class="required-label">密码</span>
          </template>
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item>
          <a-button block type="primary" :loading="loading" @click="submit">登录</a-button>
        </a-form-item>
        <a-form-item>
          <div class="register-link">
            还没有账号？<a @click="goRegister">立即注册</a>
          </div>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.login-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f8fa;
}

.login-card {
  width: 420px;
}

.register-link {
  width: 100%;
  text-align: center;
  color: #666;
}

.link-btn {
  color: #1890ff;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}
</style>
