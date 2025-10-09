import { createRouter, createWebHistory } from 'vue-router'
import OidcCallback from '@/components/OidcCallback.vue'
import HomePage from '../views/HomePage.vue'
import NegotiationCreatePage from '../views/NegotiationCreatePage.vue'
import NegotiationPage from '../views/NegotiationPage.vue'
import FaqPage from '../views/FaqPage.vue'
import NetworksPage from '../views/NetworksPage.vue'
import AdminSettingsPage from '../views/AdminSettingsPage.vue'
import AdminUiConfigurationPage from '../views/AdminUiConfigurationPage.vue'
import GovernancePage from '../views/GovernancePage.vue'
import UserPage from '@/views/UserPage.vue'
import ErrorPage from '@/views/ErrorPage.vue'
import CustomizeForm from '@/views/CustomizeForm.vue'
import { ROLES } from '@/config/consts'
import { useUserStore } from '../store/user.js'
import hasUser from '@/middlewares/hasUser.js'
import middlewarePipeline from '@/middlewares/middleware-pipeline.js'
import { useNotificationsStore } from '@/store/notifications'

const checkAccess = (allowedRoles) => {
  return async (to, from, next) => {
    const isAllowed = await isAllowedToAccess(allowedRoles)
    if (isAllowed) {
      next() // Allow access
    } else {
      next('/error-page') // Redirect to the error page
    }
  }
}

async function isAllowedToAccess(role) {
  const userStore = useUserStore()
  const notifications = useNotificationsStore()
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  if (!Array.isArray(role)) {
    role = [role]
  }

  if (!role.some((r) => userStore.userInfo.roles.includes(r))) {
    notifications.setNotification('You are not allowed to access this page.')
    return false
  }
  return true
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
      meta: { isPublic: true },
    },
    {
      path: '/logged-in',
      name: 'oidcCallback',
      component: OidcCallback,
      meta: { isPublic: true },
    },
    {
      path: '/requests/:requestId',
      name: 'request',
      props: { isEditForm: false },
      component: NegotiationCreatePage,
    },
    {
      path: '/edit/requests/:requestId',
      name: 'editWithStep',
      props: { isEditForm: true },
      component: NegotiationCreatePage,
    },
    {
      path: '/researcher',
      name: 'researcher',
      component: UserPage,
      props: { userRole: ROLES.RESEARCHER },
      meta: { isPublic: false },
    },
    {
      path: '/biobanker',
      name: 'biobanker',
      component: UserPage,
      props: { userRole: ROLES.REPRESENTATIVE },
      meta: { isPublic: false },
      beforeEnter: checkAccess(ROLES.REPRESENTATIVE),
    },
    {
      path: '/governance',
      name: 'governance',
      component: GovernancePage,
      meta: { isPublic: false, middleware: [hasUser] },
      beforeEnter: checkAccess([ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR]),
    },
    {
      path: '/admin',
      name: 'admin',
      component: UserPage,
      props: { userRole: ROLES.ADMINISTRATOR },
      meta: { isPublic: false },
      beforeEnter: checkAccess(ROLES.ADMINISTRATOR),
    },
    {
      path: '/FAQ',
      name: 'FAQ',
      component: FaqPage,
      meta: { isPublic: true, middleware: [hasUser] },
    },
    {
      path: '/settings',
      name: 'settings',
      component: AdminSettingsPage,
      meta: { isPublic: false, middleware: [hasUser] },
      beforeEnter: checkAccess(ROLES.ADMINISTRATOR),
    },
    {
      path: '/ui-configuration',
      name: 'ui-configuration',
      component: AdminUiConfigurationPage,
      meta: { isPublic: false, middleware: [hasUser] },
      beforeEnter: checkAccess(ROLES.ADMINISTRATOR),
    },
    {
      path: '/negotiations/:negotiationId/:userRole?',
      name: 'negotiation-page',
      component: NegotiationPage,
      props: true,
      meta: { middleware: [hasUser] },
    },
    {
      path: '/networks/:networkId',
      name: 'networks-page',
      component: NetworksPage,
      props: true,
      meta: { isPublic: false },
    },
    {
      path: '/createAccessForm',
      name: 'create-Access-Form',
      component: CustomizeForm,
      props: { typeAccessForm: 'Create' },
      meta: { isPublic: false },
    },
    {
      path: '/editAccessForm/:accessFormId?',
      name: 'edit-access-Form',
      component: CustomizeForm,
      props: { typeAccessForm: 'Edit' },
      meta: { isPublic: false },
    },
    {
      path: '/duplicateAccessForm/:accessFormId?',
      name: 'duplicate-access-Form',
      component: CustomizeForm,
      props: { typeAccessForm: 'Duplicate' },
      meta: { isPublic: false },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'error-page',
      component: ErrorPage,
      props: true,
      meta: { isPublic: false },
    },
  ],
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (!to.meta.middleware) {
    return next()
  }

  const middleware = to.meta.middleware
  const context = {
    to,
    from,
    next,
    userStore,
  }

  return middleware[0]({
    ...context,
    next: middlewarePipeline(context, middleware, 1),
  })
})
export default router
