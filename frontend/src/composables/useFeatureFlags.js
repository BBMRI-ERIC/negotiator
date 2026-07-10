import allFeatureFlags from '@/config/featureFlags.js'

// Normalizes a raw feature flag value to a boolean.
// Accepts real booleans as well as their string representations
function normalize(value) {
  return value === true || value === 'true'
}

// Pre-normalize all configured flags into a plain object.
const featureFlags = Object.fromEntries(
  Object.entries(allFeatureFlags).map(([key, value]) => [key, normalize(value)]),
)

// Composable providing normalized (boolean) feature flags.
// Usage: const { myFeatureFlag } = useFeatureFlags()
export function useFeatureFlags() {
  return featureFlags
}
