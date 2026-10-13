<script setup>
/**
 * 生成攻略页（F-VIEW，路由 `/trip/generate`，需登录）。
 *
 * <p>表单：城市（必填，可下拉建议或自定义输入）、天数（1~7，数字）、预算（文本，
 * 如 `2000-3000`）、偏好（多选标签）、消费水平（单选 经济/舒适/高端）。
 *
 * <p>提交流程：
 * <ol>
 *   <li>本地校验通过后置 `generating`（按钮禁用 + 文案「正在生成攻略…」，防重复提交）；</li>
 *   <li>调用 `generateTrip`，成功写入 store 并跳转 `/trip/:id`（id 为字符串）；</li>
 *   <li>失败时仅结束 loading —— 400「未找到该城市的景点数据」/5001「生成失败，请稍后重试」
 *       等文案已由 `request` 拦截器统一 `ElMessage.error`，页面**不重复弹窗**。</li>
 * </ol>
 */
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { generateTrip } from '@/api/trip'
import { useTripStore } from '@/store/trip'

const router = useRouter()
const tripStore = useTripStore()

/** 城市下拉建议（仍允许自定义输入）。 */
const CITIES = ['北京', '上海', '杭州', '成都']

/** 偏好标签选项。 */
const PREFERENCE_OPTIONS = ['美食', '自然', '历史', '拍照', '亲子', '购物', '夜生活']

/** 消费水平选项。 */
const SPEND_MODES = ['经济', '舒适', '高端']

/** 表单引用（用于校验）。 */
const formRef = ref()

/** 生成参数（对应后端 GenerateTripRequest）。 */
const form = reactive({
  city: tripStore.draft.city || '',
  days: tripStore.draft.days || 2,
  budget: tripStore.draft.budget || '',
  preferences: [...(tripStore.draft.preferences || [])],
  spendMode: tripStore.draft.spendMode || '舒适'
})

/** 校验规则：城市必填、天数必填。 */
const rules = {
  city: [{ required: true, message: '请选择或输入目的地城市', trigger: 'change' }],
  days: [{ required: true, message: '请选择旅行天数', trigger: 'change' }]
}

/**
 * 提交生成：校验 → 置 loading → 调用接口 → 跳详情。
 */
async function handleSubmit() {
  // 防重复点击：生成进行中直接忽略
  if (tripStore.generating) {
    return
  }

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  // 记住本次参数（供编辑/回填）
  tripStore.setDraft({
    city: form.city,
    days: form.days,
    budget: form.budget,
    preferences: [...form.preferences],
    spendMode: form.spendMode
  })

  tripStore.generating = true
  try {
    const detail = await generateTrip({
      city: String(form.city).trim(),
      days: Number(form.days),
      budget: form.budget ? String(form.budget).trim() : undefined,
      preferences: form.preferences,
      spendMode: form.spendMode || undefined
    })

    tripStore.setCurrent(detail)
    ElMessage.success('攻略生成成功')
    router.push(`/trip/${detail.id}`)
  } catch (e) {
    // 失败文案（400/5001/网络）由 request 拦截器统一展示，此处仅结束 loading
  } finally {
    tripStore.generating = false
  }
}
</script>

<template>
  <div class="generate page">
    <header class="generate__hero">
      <h1 class="generate__title">AI 生成旅游攻略</h1>
      <p class="generate__subtitle text-secondary">
        填写偏好，AI 为你规划逐日行程（最长约 15 秒）
      </p>
    </header>

    <el-card class="generate__card" shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="88px"
        @submit.prevent
      >
        <el-form-item label="目的地" prop="city">
          <el-select
            v-model="form.city"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入城市"
            class="generate__control"
          >
            <el-option v-for="c in CITIES" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>

        <el-form-item label="旅行天数" prop="days">
          <el-input-number v-model="form.days" :min="1" :max="7" />
          <span class="generate__hint text-secondary">1~7 天</span>
        </el-form-item>

        <el-form-item label="预算">
          <el-input
            v-model="form.budget"
            placeholder="如 2000-3000（选填）"
            maxlength="50"
            clearable
            class="generate__control"
          />
        </el-form-item>

        <el-form-item label="旅行偏好">
          <el-checkbox-group v-model="form.preferences">
            <el-checkbox-button
              v-for="p in PREFERENCE_OPTIONS"
              :key="p"
              :value="p"
            >
              {{ p }}
            </el-checkbox-button>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="消费水平">
          <el-radio-group v-model="form.spendMode">
            <el-radio-button v-for="m in SPEND_MODES" :key="m" :value="m">
              {{ m }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="tripStore.generating"
            :disabled="tripStore.generating"
            @click="handleSubmit"
          >
            {{ tripStore.generating ? '正在生成攻略…' : '生成攻略' }}
          </el-button>
          <el-button
            :disabled="tripStore.generating"
            @click="router.push('/trip/list')"
          >
            我的行程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.generate {
  padding-top: 8px;
  max-width: 720px;
}

.generate__hero {
  text-align: center;
  padding: 16px 0 12px;
}

.generate__title {
  font-size: 24px;
  margin: 0 0 6px;
}

.generate__subtitle {
  font-size: 13px;
}

.generate__card {
  margin-top: 8px;
}

.generate__control {
  width: 260px;
}

.generate__hint {
  margin-left: 12px;
  font-size: 12px;
}
</style>
