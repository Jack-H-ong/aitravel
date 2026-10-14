<script setup>
/**
 * 登录页（F-VIEW，F-USER-001）。
 *
 * <p>用户名/密码表单 + 前端必填校验；提交调用 `userStore.login` 持久化 token
 * 与用户信息，成功后跳转 `route.query.redirect`（无则回首页）。
 *
 * <p>失败文案（「用户名或密码错误」400 /「账号已被禁用」401）由 `request`
 * 拦截器依据后端返回的 `message` 直接 `ElMessage.error` 展示，本页不再重复提示，
 * 以免出现双重弹出。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

/** 若由注册页跳转而来，带 `username` 预填用户名。 */
const form = reactive({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: ''
})

/** 校验规则：登录仅需非空（长度约束由后端裁定）。 */
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

/**
 * 提交登录。
 */
async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    await userStore.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : '/')
  } catch (e) {
    // 错误提示已由 request 拦截器统一展示（后端 message 区分两种失败）
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth page">
    <el-card class="auth__card">
      <template #header>
        <div class="auth__head">
          <h1 class="auth__title">登录</h1>
          <p class="auth__subtitle">欢迎回来，继续你的旅程</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="auth__submit"
            :loading="loading"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth__footer">
        还没有账号？
        <el-button link type="primary" @click="router.push('/register')">
          去注册
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth {
  display: flex;
  justify-content: center;
  padding-top: 56px;
}

.auth__card {
  width: 400px;
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow);
}

.auth__head {
  text-align: center;
}

.auth__title {
  font-family: var(--app-font-display);
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
}

.auth__subtitle {
  color: var(--app-ink-soft);
  font-size: 13px;
  margin: 0;
}

.auth__submit {
  width: 100%;
}

.auth__footer {
  color: var(--app-ink-soft);
  font-size: 13px;
  text-align: center;
}
</style>
