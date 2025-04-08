const devSettings = {
  faqPage: true,
  vueTour: true,
  notifications: true,
  networks: true,
  add_acess_form: true,
  dev_mode: true,
}

const prodSettings = {
  faqPage: 'FEATURE_FLAG_FAQPAGE_PLACEHOLDER',
  vueTour: 'FEATURE_FLAG_VUETOUR_PLACEHOLDER',
  notifications: 'FEATURE_FLAG_NOTIFICATIONS',
  networks: 'FEATURE_FLAG_NETWORKS',
  add_acess_form: 'FEATURE_FLAG_ADD_ACCESS_FORM_PLACEHOLDER',
  dev_mode: 'DEV_MODE_PLACEHOLDER',
}

let allFeatureFlags

if (import.meta.env.DEV) {
  allFeatureFlags = devSettings
} else {
  allFeatureFlags = prodSettings
}

export default allFeatureFlags
