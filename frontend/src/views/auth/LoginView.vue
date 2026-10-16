<template>
  <div class="auth-wrap">
    <el-card class="auth-card">
      <div class="brand">
        <el-icon size="30" color="#409eff"><Connection /></el-icon>
        <h2>FlowWork</h2>
      </div>
      <p class="sub">AI 驱动的团队项目协作平台</p>
      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码"
            size="large" show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading"
          @click="onLogin">登 录</el-button>
      </el-form>
      <div class="footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
        <span style="margin:0 8px;color:#c0c4cc">|</span>
        <router-link to="/register-application">申请注册码</router-link>
      </div>
      <el-alert type="info" :closable="false" style="margin-top:16px"
        title="演示账号：admin / 123456" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import authApi from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: 'admin', password: '123456' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res: any = await authApi.login({
      ...form,
      deviceId: localStorage.getItem('deviceId') || undefined
    })
    userStore.setAuth(res.accessToken, res.refreshToken)
    userStore.userInfo = res.userInfo
    if (!localStorage.getItem('deviceId')) {
      localStorage.setItem('deviceId', String(Date.now()))
    }
    ElMessage.success('登录成功')
    router.replace((route.query.redirect as string) || '/projects')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3a4a3c 0%, #6c9a8b 50%, #8db5a6 100%);
}
.auth-card {
  width: 400px;
  padding: 28px 20px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(60, 70, 50, 0.15);
  background: #fffdf9;
}
.brand { display: flex; align-items: center; justify-content: center; gap: 10px; }
.brand h2 { margin: 0; }
.sub { text-align: center; color: #909399; margin: 6px 0 24px; }
.footer { text-align: center; margin-top: 16px; color: #606266; }
</style>
