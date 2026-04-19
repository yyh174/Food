<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { getCurrentUserApi, updateUserApi, changePasswordApi } from '@/api/user'
import { getLatestInviteCode, regenerateInviteCode } from '@/api/modules/inviteCode'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const saving = ref(false)
const showPasswordModal = ref(false)
const passwordLoading = ref(false)
const inviteCodeLoading = ref(false)

const passwordFormRef = ref()

const formState = ref({
  username: '',
  nickname: '',
  phone: '',
  email: '',
  roleName: '',
  shopName: '',
  tenantName: '',
})

const inviteCodeData = ref<{
  inviteCode: string
  expireTime: string
} | null>(null)

const passwordFormState = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordFormRules = {
  oldPassword: [{ required: true, message: '请输入原密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码长度不能少于6位' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    {
      validator: (_rule: any, value: string) => {
        if (value !== passwordFormState.value.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
    },
  ],
}

const formRules = {
  nickname: [{ required: true, message: '请输入昵称' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号' }],
  email: [{ type: 'email', message: '请输入正确的邮箱' }],
}

const loadUserDetail = async () => {
  loading.value = true
  try {
    const res = await getCurrentUserApi()
    if (res) {
      formState.value = {
        username: res.username || '',
        nickname: res.nickname || res.realName || '',
        phone: res.phone || '',
        email: res.email || '',
        roleName: res.roleName || '',
        shopName: res.shopName || '-',
        tenantName: res.tenantName || '-',
      }
    }
  } catch {
    message.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

const loadInviteCode = async () => {
  if (!authStore.isTenantAdmin) return
  try {
    const res = await getLatestInviteCode()
    if (res) {
      inviteCodeData.value = {
        inviteCode: res.inviteCode,
        expireTime: res.expireTime,
      }
    }
  } catch {
    // 忽略错误
  }
}

const handleRegenerate = async () => {
  inviteCodeLoading.value = true
  try {
    const res = await regenerateInviteCode()
    message.success('邀请码重新生成成功')
    inviteCodeData.value = {
      inviteCode: res.inviteCode,
      expireTime: res.expireTime,
    }
  } catch (e: any) {
    message.error(e.message || '重新生成失败')
  } finally {
    inviteCodeLoading.value = false
  }
}

const handleCopy = () => {
  if (inviteCodeData.value?.inviteCode) {
    navigator.clipboard.writeText(inviteCodeData.value.inviteCode)
    message.success('邀请码已复制到剪贴板')
  }
}

const handleSave = async () => {
  try {
    await Promise.resolve()
    saving.value = true
    await updateUserApi({
      nickname: formState.value.nickname,
      phone: formState.value.phone,
      email: formState.value.email,
    })
    message.success('保存成功')
    authStore.updateUserInfo({ nickname: formState.value.nickname })
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    await changePasswordApi({
      oldPassword: passwordFormState.value.oldPassword,
      newPassword: passwordFormState.value.newPassword,
    })
    message.success('密码修改成功')
    showPasswordModal.value = false
    passwordFormState.value = {
      oldPassword: '',
      newPassword: '',
      confirmPassword: '',
    }
  } catch (e: any) {
    message.error(e.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadUserDetail()
  loadInviteCode()
})
</script>

<template>
  <div class="profile-container">
    <a-spin :spinning="loading">
      <a-card title="个人信息" class="profile-card">
        <a-form
          :model="formState"
          :rules="formRules"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="用户名">
            <span>{{ formState.username }}</span>
          </a-form-item>
          <a-form-item label="角色">
            <a-tag color="blue">{{ formState.roleName }}</a-tag>
          </a-form-item>
          <a-form-item label="所属租户">
            <span>{{ formState.tenantName }}</span>
          </a-form-item>
          <a-form-item label="所属门店">
            <span>{{ formState.shopName }}</span>
          </a-form-item>
          <a-form-item label="昵称" name="nickname">
            <a-input v-model:value="formState.nickname" placeholder="请输入昵称" />
          </a-form-item>
          <a-form-item label="手机号" name="phone">
            <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
          </a-form-item>
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
          </a-form-item>
          <a-form-item :wrapper-col="{ offset: 4 }">
            <a-space>
              <a-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </a-button>
              <a-button @click="showPasswordModal = true">
                修改密码
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>

      <!-- 租户管理员邀请码卡片 -->
      <a-card v-if="authStore.isTenantAdmin" title="注册邀请码" class="profile-card" style="margin-top: 16px;">
        <div class="invite-code-content">
          <div class="invite-code-display">
            <div class="invite-code-value">
              {{ inviteCodeData?.inviteCode || '暂无邀请码' }}
            </div>
            <div v-if="inviteCodeData" class="invite-code-expire">
              有效期至：{{ inviteCodeData.expireTime }}
            </div>
          </div>
          <a-space>
            <a-button
              type="primary"
              :loading="inviteCodeLoading"
              @click="handleRegenerate"
            >
              重新生成
            </a-button>
            <a-button
              :disabled="!inviteCodeData"
              @click="handleCopy"
            >
              复制
            </a-button>
          </a-space>
        </div>
      </a-card>
    </a-spin>

    <!-- 右下角退出按钮 -->
    <div class="logout-btn-wrapper">
      <a-button type="primary" danger size="large" @click="handleLogout">
        退出登录
      </a-button>
    </div>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:open="showPasswordModal"
      title="修改密码"
      :confirm-loading="passwordLoading"
      @ok="handleChangePassword"
      ok-text="确认修改"
      cancel-text="取消"
    >
      <a-form
        ref="passwordFormRef"
        :model="passwordFormState"
        :rules="passwordFormRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="原密码" name="oldPassword">
          <a-input-password v-model:value="passwordFormState.oldPassword" placeholder="请输入原密码" />
        </a-form-item>
        <a-form-item label="新密码" name="newPassword">
          <a-input-password v-model:value="passwordFormState.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" name="confirmPassword">
          <a-input-password v-model:value="passwordFormState.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 24px;
  position: relative;
  min-height: calc(100vh - 56px);
}

.profile-card {
  max-width: 800px;
  margin: 0 auto;
}

.invite-code-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.invite-code-display {
  flex: 1;
  min-width: 200px;
}

.invite-code-value {
  font-size: 24px;
  font-weight: 600;
  color: #1890ff;
  font-family: monospace;
  letter-spacing: 2px;
}

.invite-code-expire,
.invite-code-shop {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

.logout-btn-wrapper {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 100;
}
</style>
