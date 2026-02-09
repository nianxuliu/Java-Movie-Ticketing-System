import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const role = ref(localStorage.getItem('role') || '')

  function setRole(newRole) {
    role.value = newRole
    localStorage.setItem('role', newRole)
  }

  // 退出登录
  function clearUserSession() {
    role.value = ''
    localStorage.removeItem('role')
    localStorage.removeItem('token')
  }

  return { role, setRole, clearUserSession }
})
