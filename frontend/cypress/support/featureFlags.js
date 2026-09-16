// Flags arrive from cypress.config.js as booleans, or as strings when overridden by a CYPRESS_
// prefixed variable, so accept both. Mirrors normalize() in src/composables/useFeatureFlags.js.
function normalize(value) {
  return value === true || value === 'true'
}

function isFeatureEnabled(name) {
  const flags = Cypress.env()
  // An unknown name would normalize to false and skip the suite, so a typo would drop coverage
  // while the run stayed green.
  if (!(name in flags)) {
    const known = Object.keys(flags).join(', ') || '(none)'
    throw new Error(
      `Unknown feature flag "${name}". Add it to the env block in cypress.config.js. ` +
        `Known flags: ${known}.`,
    )
  }
  return normalize(flags[name])
}

// Registers the suite as pending when the flag is off, so the run output names the flag.
export function describeWithFeature(name, title, fn) {
  return isFeatureEnabled(name)
    ? describe(title, fn)
    : describe.skip(`${title} [skipped: ${name} is not enabled]`, fn)
}
