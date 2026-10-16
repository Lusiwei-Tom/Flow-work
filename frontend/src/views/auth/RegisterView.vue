<template>
  <div class="auth-wrap">
    <el-card class="auth-card">
      <h2 style="text-align:center">注册 FlowWork</h2>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（字母数字下划线）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（6-32位）"
            show-password :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称" :prefix-icon="Avatar" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（可选）" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="inviteCode">
          <el-input v-model="form.inviteCode" placeholder="邀请码（由管理员发放）" :prefix-icon="Key" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="onRegister">注 册</el-button>
      </el-form>
      <div class="footer">
        <router-link to="/login">已有账号？返回登录</router-link>
        <span style="margin:0 8px;color:#c0c4cc">|</span>
        <router-link to="/register-application">没有邀请码？申请注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Avatar, Message, Key } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import authApi from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ username: '', password: '', nickname: '', email: '', inviteCode: '' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '长度 3-32', trigger: 'blur' }
  ],
  password: [{ required: true, min: 6, max: 32, message: '6-32位密码', trigger: 'blur' }]
}

async function onRegister() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const res: any = await authApi.register(form)
    userStore.setAuth(res.accessToken, res.refreshToken)
    userStore.userInfo = res.userInfo
    ElMessage.success('注册成功')
    router.replace('/projects')
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
.auth-card { width: 400px; padding: 28px 20px; border-radius: 16px; box-shadow: 0 8px 32px rgba(60,70,50,0.15); background: #fffdf9; }
.footer { text-align: center; margin-top: 16px; }
</style>
