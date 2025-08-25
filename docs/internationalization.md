# 🌐 Internationalization (i18n) in Vue.js

**⚙️ Configuration**

Create an i18n.js file to define your translations or use existing one:

```
export default {
  locale: 'en-canSERV',
  fallbackLocale: 'en',
  messages: {
    en: {
      navbar: {
        admin: 'Review requests',
        researcher: 'Your negotiations',
        biobanker: 'Your biobank',
        FAQ: 'Support',
        doc: 'Documentation',
      },
      negotiationPage: {
        resources: 'RESOURCES',
        organisations: 'ORGANIZATIONS',
      },
    },
    'en-canSERV': {
      navbar: {
        biobanker: 'Your organisation',
      },
      negotiationPage: {
        resources: 'Service',
        organisations: 'Service provider',
      },
    },
  },
}

```

**🗣 Usage in Components**

Access translations using the $t function:

```
<template>
  <div>
    <h1>{{ $t('welcome') }}</h1>
    <p>{{ $t('goodbye') }}</p>
  </div>
</template>
```

**🔄 Switching Languages**

/frontend/start.sh

```
locale: "I18N_LOCALE_PLACEHOLDER",
fallbackLocale: "I18N_FALLBACKLOCALE_PLACEHOLDER"
```

**🛠 Tips**

Use fallbackLocale to prevent missing translation errors.

Keep keys descriptive and consistent.
