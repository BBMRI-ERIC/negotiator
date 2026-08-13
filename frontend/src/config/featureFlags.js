const devSettings = {
  faqPage: true,
  notifications: true,
  networks: true,
  dev_mode: true,
  orgResourceStateOverride: true,
  pdfExportEnabled: true,
}

const prodSettings = {
  faqPage: 'FEATURE_FLAG_FAQPAGE_PLACEHOLDER',
  notifications: 'FEATURE_FLAG_NOTIFICATIONS',
  networks: 'FEATURE_FLAG_NETWORKS',
  dev_mode: 'DEV_MODE_PLACEHOLDER',
  orgResourceStateOverride: 'FEATURE_FLAG_ORG_RESOURCE_STATE_OVERRIDE_PLACEHOLDER',
  pdfExportEnabled: 'FEATURE_FLAG_PDF_EXPORT_ENABLED_PLACEHOLDER',
}

let allFeatureFlags

if (import.meta.env.DEV) {
  allFeatureFlags = devSettings
} else {
  allFeatureFlags = prodSettings
}

export default allFeatureFlags
