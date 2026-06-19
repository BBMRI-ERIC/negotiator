import { computed, unref } from 'vue'

const WEBHOOK_URL_PATTERN = /^https?:\/\/.+/
const WEBHOOK_SECRET_PREFIX = 'whsec_'
const BASE64_KEY_MATERIAL_PATTERN = /^[A-Za-z0-9+/=]+$/
const MIN_SECRET_BYTES = 24
const MAX_SECRET_BYTES = 64

export const useWebhookFormValidation = ({ url, secretInput, showSecretInput }) => {
  const isSecretInputVisible = computed(() => {
    if (showSecretInput === undefined) {
      return true
    }

    return Boolean(unref(showSecretInput))
  })

  const normalizedSecretInput = computed(() => {
    const value = unref(secretInput)
    return typeof value === 'string' ? value : ''
  })

  const urlIsValid = computed(() => validateWebhookUrl(unref(url)))
  const secretValidation = computed(() =>
    validateWebhookSecret(normalizedSecretInput.value, isSecretInputVisible.value),
  )
  const secretValidationMessage = computed(() => secretValidation.value.message)
  const showSecretValidationError = computed(
    () => isSecretInputVisible.value && secretValidationMessage.value.length > 0,
  )
  const secretIsValid = computed(() => secretValidation.value.isValid)

  return {
    urlIsValid,
    secretValidationMessage,
    showSecretValidationError,
    secretIsValid,
  }
}

const validateWebhookUrl = (url) => {
  return typeof url === 'string' && WEBHOOK_URL_PATTERN.test(url)
}

const validateWebhookSecret = (secretInput, showSecretInput = true) => {
  if (!showSecretInput) {
    return { isValid: true, message: '' }
  }

  if (secretInput.length === 0) {
    return { isValid: true, message: '' }
  }

  if (!secretInput.startsWith(WEBHOOK_SECRET_PREFIX)) {
    return {
      isValid: false,
      message: `Secret must start with ${WEBHOOK_SECRET_PREFIX}`,
    }
  }

  const encodedKeyMaterial = secretInput.slice(WEBHOOK_SECRET_PREFIX.length)
  if (encodedKeyMaterial.length === 0) {
    return {
      isValid: false,
      message: 'Secret must include base64 key material after whsec_',
    }
  }

  let decodedKeyMaterial
  try {
    decodedKeyMaterial = decodeBase64KeyMaterial(encodedKeyMaterial)
  } catch {
    return {
      isValid: false,
      message: 'Secret key material must be valid base64',
    }
  }

  if (
    decodedKeyMaterial.length < MIN_SECRET_BYTES ||
    decodedKeyMaterial.length > MAX_SECRET_BYTES
  ) {
    return {
      isValid: false,
      message: 'Secret key material must decode to between 24 and 64 bytes',
    }
  }

  return { isValid: true, message: '' }
}

const decodeBase64KeyMaterial = (encodedKeyMaterial) => {
  if (!BASE64_KEY_MATERIAL_PATTERN.test(encodedKeyMaterial)) {
    throw new Error('Invalid base64 characters')
  }

  const decodedBinary = atob(encodedKeyMaterial)
  return Uint8Array.from(decodedBinary, (char) => char.charCodeAt(0))
}

export const setSecretChangeMode = (changeSecretMode, secretInput, enabled) => {
  changeSecretMode.value = enabled
  secretInput.value = ''
}

export const buildWebhookPayload = ({
  form,
  secretInput,
  secretIsValid,
  clearExistingSecret = false,
}) => {
  const payload = {
    url: form.url,
    sslVerification: form.sslVerification,
    active: form.active,
  }

  if (secretInput.length > 0 && secretIsValid) {
    payload.secret = secretInput
  } else if (clearExistingSecret) {
    payload.secret = null
  }

  return payload
}
