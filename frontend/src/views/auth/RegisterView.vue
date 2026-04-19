<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { registerApi } from '@/api/register'
import { post } from '@/utils/request'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const registerType = computed(() => route.query.type as string || 'tenant')
const isManager = computed(() => registerType.value === 'manager')
const cardTitle = computed(() => isManager.value ? '员工注册' : '租户注册')

const goLogin = () => {
  router.push('/login')
}

const formState = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  inviteCode: '',
})

const validateConfirmPassword = async (_rule: unknown, value: string) => {
  if (value !== formState.password) {
    throw new Error('两次输入的密码不一致')
  }
}

const inviteCodeRule = computed(() => {
  if (isManager.value) {
    return [{ required: true, message: '请输入邀请码' }]
  }
  return []
})

const rules = computed(() => ({
  username: [{ required: true, message: '请输入用户名' }],
  password: [
    { required: true, message: '请输入密码' },
    { min: 6, message: '密码至少6位' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码' },
    { validator: validateConfirmPassword, trigger: 'change' },
  ],
  inviteCode: inviteCodeRule.value,
}))

const submit = async () => {
  console.log('submit clicked')
  try {
    await formRef.value?.validate()
    console.log('validation passed')
    loading.value = true
    
    if (isManager.value) {
      await post('/auth/register/shop-manager', {
        username: formState.username,
        password: formState.password,
        inviteCode: formState.inviteCode,
      })
      message.success('注册成功，请联系租户管理员分配门店后登录')
    } else {
      await registerApi({
        username: formState.username,
        password: formState.password,
      })
      message.success('注册成功，请登录')
    }
    router.push('/login')
  } catch (e: any) {
    console.log('submit error:', e)
    message.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 旧版本通过原生事件监听，现已改用 @click 绑定
})
</script>

<template>
  <div class="register-page">
    <a-card :title="cardTitle" class="register-card">
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        :label-col="{ style: { width: '100px' } }"
      >
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
          <a-input-password v-model:value="formState.password" placeholder="请输入密码（至少6位）" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <template #label>
            <span class="required-label">确认密码</span>
          </template>
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="请再次输入密码"
          />
        </a-form-item>
        <a-form-item v-if="isManager" label="邀请码" name="inviteCode">
          <template #label>
            <span class="required-label">邀请码</span>
          </template>
          <a-input v-model:value="formState.inviteCode" placeholder="请输入邀请码" />
        </a-form-item>
        <a-form-item>
          <a-button id="register-btn" block type="primary" :loading="loading" @click="submit">注册</a-button>
        </a-form-item>
        <a-form-item>
          <div class="login-link">
            已有账号？<span class="link-btn" @click="goLogin">立即登录</span>
          </div>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.register-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6f8fa;
}

.register-card {
  width: 420px;
}

.login-link {
  width: 100%;
  text-align: center;
  color: #666;
}

.login-link a,
.link-btn {
  color: #1890ff;
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
