import { createApp } from 'vue'
import App from './App.vue'
import VueMatomo from 'vue-matomo'
import router from './router'
import { createPinia } from 'pinia'
import { library } from '@fortawesome/fontawesome-svg-core'
import { faDownload, faPencil, faSpinner, faTrash } from '@fortawesome/free-solid-svg-icons'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue-next/dist/bootstrap-vue-next.css'
import 'bootstrap'
import 'bootstrap-icons/font/bootstrap-icons.css'
import matomo from './config/matomo.js'
import Vue3Tour from 'vue3-tour'
import 'vue3-tour/dist/vue3-tour.css'
import { useOidcStore } from './store/oidc'
import { piniaOidcCreateRouterMiddleware } from 'pinia-oidc'

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

app.use(router)
app.use(pinia)
app.use(Vue3Tour)

router.beforeEach(piniaOidcCreateRouterMiddleware(useOidcStore()))

app.component('FontAwesomeIcon', FontAwesomeIcon)

app.mount('#app')

if (matomo.matomoHost !== 'MATOMO_HOST_PLACEHOLDER') {
  window._paq.push(['trackPageView']) // To track a page view
}

import('./assets/scss/theme.scss')
