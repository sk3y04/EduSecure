import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { registerUnauthorizedHandler } from './services/http'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()
const authStore = useAuthStore(pinia)

let isRedirectingAfterUnauthorized = false

registerUnauthorizedHandler(async () => {
  if (isRedirectingAfterUnauthorized) {
	return
  }

  isRedirectingAfterUnauthorized = true

  try {
	authStore.handleSessionExpired()

	const currentRoute = router.currentRoute.value
	if (currentRoute.name === 'login') {
	  return
	}

	await router.push({
	  name: 'login',
	  query: currentRoute.meta.requiresAuth === false
		? { reason: 'session-expired' }
		: { reason: 'session-expired', redirect: currentRoute.fullPath },
	})
  } finally {
	isRedirectingAfterUnauthorized = false
  }
})

app.use(pinia)
app.use(router)

app.mount('#app')
