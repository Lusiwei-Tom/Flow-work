import type { Directive } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 按钮级权限指令：
 * v-permission="'project:task:create'"
 * v-permission="['a','b']"  （满足其一）
 */
const permission: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const userStore = useUserStore()
    const value = binding.value
    const codes = Array.isArray(value) ? value : [value]
    if (!userStore.hasAnyPermission(codes)) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permission
