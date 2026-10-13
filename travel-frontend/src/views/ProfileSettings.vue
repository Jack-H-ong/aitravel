<script setup>
/**
 * 资料设置页（F-VIEW，F-USER-005，路由 `/profile/settings`，需登录）。
 *
 * <p>三块表单：
 * <ol>
 *   <li>昵称；</li>
 *   <li>头像 URL（含实时预览）；</li>
 *   <li>修改密码（旧密码 + 新密码 + 确认新密码）。</li>
 * </ol>
 * 均调用 `PUT /api/auth/update`（仅提交非空字段）。改密成功后提示并退出登录，
 * 要求以新密码重新登录。
 *
 * <p>后端错误（如「原密码错误」400）由 `request` 拦截器统一展示。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { updateProfile } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const infoFormRef = ref(null)
const pwdFormRef = ref(null)
const infoLoading = ref(false)
const pwdLoading = ref(false)

/** 基本资料表单（昵称 + 头像 URL）。 */
const infoForm = reactive({
  nickname: '',
  avatar: ''
})

/** 修改密码表单。 */
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

/** 头像预览地址。 */
const avatarPreview = computed(() => infoForm.avatar || '')

/** 头像 URL：可选，非空时须为 http(s) 链接且不超过 255 字符（后端约束）。 */
function validateAvatar(_rule, value, callback) {
  if (!value) {
    callback()
  } else if (value.length > 255) {
    callback(new Error('头像地址长度不能超过 255 位'))
  } else if (!/^https?:\/\/.+/i.test(value)) {
    callback(new Error('请输入以 http(s):// 开头的图片地址'))
  } else {
    callback()
  }
}

/** 新密码：非空且 6-20 位。 */
function validateNewPassword(_rule, value, callback) {
  if (!value) {
    callback(new Error('请输入新密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('新密码长度需为 6-20 位'))
  } else {
    callback()
  }
}

/** 旧密码：填写新密码时必填（后端要求）。 */
function validateOldPassword(_rule, value, callback) {
  if (pwdForm.newPassword && !value) {
    callback(new Error('请输入原密码'))
  } else {
    callback()
  }
}

/** 确认新密码：须与新密码一致。 */
function validateConfirmPassword(_rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const infoRules = {
  nickname: [{ max: 50, message: '昵称长度不能超过 50 位', trigger: 'blur' }],
  avatar: [{ validator: validateAvatar, trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ validator: validateOldPassword, trigger: 'blur' }],
  newPassword: [{ validator: validateNewPassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

/** 用 store 中的用户信息回填表单。 */
function syncFromStore() {
  const info = userStore.userInfo || {}
  infoForm.nickname = info.nickname || ''
  infoForm.avatar = info.avatar || ''
}

onMounted(async () => {
  syncFromStore()
  // 拉取最新用户信息（失败由 request 拦截器处理）
  try {
    await userStore.fetchMe()
    syncFromStore()
  } catch (e) {
    // 忽略：401 已由拦截器跳转登录页
  }
})

/**
 * 保存基本资料（昵称 / 头像）。
 */
async function handleSaveInfo() {
  if (!infoFormRef.value) {
    return
  }
  try {
    await infoFormRef.value.validate()
  } catch (e) {
    return
  }

  const payload = {}
  const nickname = infoForm.nickname.trim()
  const avatar = infoForm.avatar.trim()
  if (nickname) {
    payload.nickname = nickname
  }
  if (avatar) {
    payload.avatar = avatar
  }
  if (Object.keys(payload).length === 0) {
    ElMessage.warning('请先填写昵称或头像地址')
    return
  }

  infoLoading.value = true
  try {
    await updateProfile(payload)
    userStore.setUser(payload)
    ElMessage.success('资料已更新')
  } catch (e) {
    // 错误提示由 request 拦截器统一展示
  } finally {
    infoLoading.value = false
  }
}

/**
 * 修改密码：成功后需退出登录并跳转登录页。
 */
async function handleChangePassword() {
  if (!pwdFormRef.value) {
    return
  }
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }

  pwdLoading.value = true
  try {
    await updateProfile({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    userStore.logout()
    router.push('/login')
  } catch (e) {
    // 错误提示由 request 拦截器统一展示（如「原密码错误」）
  } finally {
    pwdLoading.value = false
  }
}
</script>

<template>
  <div class="settings page">
    <h2 class="settings__title">资料设置</h2>

    <el-card class="settings__card">
      <template #header>
        <span>基本资料</span>
      </template>

      <el-form
        ref="infoFormRef"
        :model="infoForm"
        :rules="infoRules"
        label-width="90px"
      >
        <el-form-item label="头像">
          <div class="settings__avatar">
            <el-avatar :size="64" :src="avatarPreview">
              {{ (userStore.displayName || 'U').charAt(0) }}
            </el-avatar>
            <span class="settings__avatar-hint">头像 URL 预览</span>
          </div>
        </el-form-item>

        <el-form-item label="头像 URL" prop="avatar">
          <el-input
            v-model="infoForm.avatar"
            placeholder="https://example.com/avatar.png"
            clearable
          />
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="infoForm.nickname"
            placeholder="请输入昵称（不超过 50 位）"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="infoLoading"
            @click="handleSaveInfo"
          >
            保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="settings__card">
      <template #header>
        <span>修改密码</span>
      </template>

      <el-form
        ref="pwdFormRef"
        :model="pwdForm"
        :rules="pwdRules"
        label-width="90px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="pwdForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            placeholder="6-20 位新密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="pwdForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="pwdLoading"
            @click="handleChangePassword"
          >
            确认修改
          </el-button>
        </el-form-item>
      </el-form>

      <div class="settings__tip">修改密码成功后需使用新密码重新登录。</div>
    </el-card>
  </div>
</template>

<style scoped>
.settings {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px 16px;
}

.settings__title {
  margin: 0 0 16px;
  font-size: 20px;
}

.settings__card {
  margin-bottom: 20px;
}

.settings__avatar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.settings__avatar-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.settings__tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
