<template>
  <div class="auth-wrap">
    <el-card class="auth-card">
      <h2 style="text-align:center">申请注册</h2>
      <p class="desc">填写信息提交申请，管理员审批通过后会生成邀请码发给你</p>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="applicantName">
          <el-input v-model="form.applicantName" placeholder="姓名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（可选）" :prefix-icon="Phone" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（可选）" :prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3"
            placeholder="请简述申请理由（如：XX 部门新员工）" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" @click="onSubmit">
          提交申请
        </el-button>
      </el-form>
      <div class="footer">
        <router-link to="/login">返回登录</router-link>
        <span style="margin:0 8px;color:#c0c4cc">|</span>
        <router-link to="/register">已有邀请码？去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Phone, Message } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance } from 'element-plus'
import applicationApi from '@/api/application'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({ applicantName: '', phone: '', email: '', reason: '' })
const rules = {
  applicantName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  reason: [{ required: true, min: 2, message: '请填写申请理由', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await applicationApi.submit(form)
    ElMessage.success('申请已提交，请等待管理员审批')
    router.replace('/login')
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
  width: 420px;
  padding: 28px 20px;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(60,70,50,0.15);
  background: #fffdf9;
}
.desc {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 6px 0 18px;
}
.footer { text-align: center; margin-top: 16px; }
</style>
