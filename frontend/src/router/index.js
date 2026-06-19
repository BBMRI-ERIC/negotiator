import { createRouter, createWebHistory } from 'vue-router'
import OidcCallback from '@/components/OidcCallback.vue'
import HomePage from '../views/HomePage.vue'
import NegotiationCreatePage from '../views/NegotiationCreatePage.vue'
import NegotiationPage from '../views/NegotiationPage.vue'
import FaqPage from '../views/FaqPage.vue'
import NetworksPage from '../views/NetworksPage.vue'
import AdminSettingsPage from '../views/AdminSettingsPage.vue'
import AdminSettingsUiConfiguration from '@/components/AdminSettingsUiConfiguration.vue'
import UserListSection from '@/components/UserListSection.vue'
import EmailNotificationsSection from '@/components/EmailNotificationsSection.vue'
import TemplateSection from '@/components/TemplateSection.vue'
import AccessFormsSection from '@/components/AccessFormsSection.vue'
import CustomizeForm from '../views/CustomizeForm.vue'
import ElementsManagement from '@/components/ElementsManagement.vue'
import InformationRequirementsSection from '@/components/InformationRequirementsSection.vue'
import WebhooksListPage from '../views/WebhooksListPage.vue'
import WebhookCreatePage from '../views/WebhookCreatePage.vue'
import WebhookDetailPage from '../views/WebhookDetailPage.vue'
import GovernancePage from '../views/GovernancePage.vue'
import UserPage from '@/views/UserPage.vue'
import ErrorPage from '@/views/ErrorPage.vue'
import GuidePage from '@/views/GuidePage.vue'
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
      component: AdminSettingsPage,
      redirect: '/settings/ui-configuration',
      meta: { isPublic: false, middleware: [hasUser] },
      beforeEnter: checkAccess(ROLES.ADMINISTRATOR),
      children: [
        {
          path: 'ui-configuration',
          name: 'admin-ui-configuration',
          component: AdminSettingsUiConfiguration,
        },
        { path: 'users', name: 'admin-users', component: UserListSection },
        {
          path: 'email-notifications',
          name: 'admin-email-notifications',
          component: EmailNotificationsSection,
        },
        {
          path: 'email-templates',
          name: 'admin-email-templates',
          component: TemplateSection,
        },
        {
          path: 'access-forms',
          name: 'admin-access-forms',
          component: AccessFormsSection,
        },
        {
          path: 'access-forms/create',
          name: 'admin-access-form-create',
          component: CustomizeForm,
          props: { typeAccessForm: 'Create' },
        },
        {
          path: 'access-forms/edit/:accessFormId',
          name: 'admin-access-form-edit',
          component: CustomizeForm,
          props: { typeAccessForm: 'Edit' },
        },
        {
          path: 'access-forms/duplicate/:accessFormId',
          name: 'admin-access-form-duplicate',
          component: CustomizeForm,
          props: { typeAccessForm: 'Duplicate' },
        },
        {
          path: 'form-elements',
          name: 'admin-form-elements',
          component: ElementsManagement,
        },
        {
          path: 'information-requirements',
          name: 'admin-information-requirements',
          component: InformationRequirementsSection,
        },
        { path: 'webhooks', name: 'admin-webhooks', component: WebhooksListPage },
        {
          path: 'webhooks/new',
          name: 'admin-webhooks-create',
          component: WebhookCreatePage,
        },
        {
          path: 'webhooks/:webhookId',
          name: 'admin-webhooks-detail',
          component: WebhookDetailPage,
          props: true,
        },
      ],
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
      path: '/guide',
      name: 'guide',
      component: GuidePage,
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
