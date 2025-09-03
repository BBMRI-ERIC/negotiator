import { createApp } from 'vue'
import App from './App.vue'
import VueMatomo from 'vue-matomo'
import router from './router'
import { createPinia } from 'pinia'
import { library } from '@fortawesome/fontawesome-svg-core'
import { faDownload, faPencil, faSpinner, faTrash } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import 'bootstrap'
import 'bootstrap-icons/font/bootstrap-icons.css'
import matomo from './config/matomo.js'
import Vue3Tour from 'vue3-tour'
import 'vue3-tour/dist/vue3-tour.css'
import { useOidcStore } from './store/oidc'
import { piniaOidcCreateRouterMiddleware } from 'pinia-oidc'
import VueDOMPurifyHTML from 'vue-dompurify-html'
import { createI18n } from 'vue-i18n'
import i18nConfig from './config/i18n.js'
import('./assets/scss/theme.scss')

library.add(faSpinner)
library.add(faPencil)
library.add(faTrash)
library.add(faDownload)

const pinia = createPinia()
const app = createApp(App)

if (matomo.matomoHost !== 'MATOMO_HOST_PLACEHOLDER') {
  app.use(VueMatomo, {
    host: matomo.matomoHost,
    siteId: matomo.matomoId,
  })
}
if (import.meta.env.DEV) {
  i18nConfig.locale = 'en'
  i18nConfig.fallbackLocale = 'en'
}
const i18n = createI18n(i18nConfig)

app.use(i18n)
app.use(router)
app.use(pinia)
app.use(Vue3Tour)
app.use(VueDOMPurifyHTML, {
  default: {
    USE_PROFILES: { html: false },
  },
})

router.beforeEach(piniaOidcCreateRouterMiddleware(useOidcStore()))

app.component('FontAwesomeIcon', FontAwesomeIcon)

app.mount('#app')

if (matomo.matomoHost !== 'MATOMO_HOST_PLACEHOLDER') {
  window._paq.push(['trackPageView']) // To track a page view
}
