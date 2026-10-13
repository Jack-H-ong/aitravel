<script setup>
/**
 * 注册页（F-VIEW，F-USER-001）。
 *
 * <p>用户名(3-20) / 密码(6-20) / 确认密码 + 前端校验（长度、两次一致）；
 * 提交调用 `authApi.register`，成功后 `ElMessage` 提示并跳转登录页（预填用户名）。
 *
 * <p>后端错误（如「用户名已注册」400）由 `request` 拦截器统一
 * `ElMessage.error` 展示，本页不再重复提示。
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

/**
 * 确认密码校验：须与密码一致。
 *
 * @param {object} _rule 校验规则
 * @param {string} value 当前值
 * @param {(error?:Error)=>void} callback 回调
 */
function validateConfirm(_rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

/** 校验规则：与后端 3-20 / 6-20 约束一致，前端先行拦截。 */
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ required: true, validator: validateConfirm, trigger: 'blur' }]
}

/**
 * 提交注册。
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
    await register({
      username: form.username,
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success('注册成功，请登录')
    router.push({ path: '/login', query: { username: form.username } })
  } catch (e) {
    // 错误提示已由 request 拦截器统一展示（如「用户名已注册」）
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth page">
    <el-card class="auth__card">
      <template #header>
        <span>注册</span>
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
            placeholder="3-20 位用户名"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6-20 位密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
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
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="auth__footer">
        已有账号？
        <el-button link type="primary" @click="router.push('/login')">
          去登录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.auth {
  display: flex;
  justify-content: center;
  padding-top: 48px;
}

.auth__card {
  width: 380px;
}

.auth__submit {
  width: 100%;
}

.auth__footer {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  text-align: center;
}
</style>
