# Internationalization (i18n)

The Negotiator provides internationalization support to adapt the user interface for different languages and regions. This feature allows you to customize the application's language settings and add project-specific terminology.

## Current Support Level

Currently, the Negotiator has **limited support** for internationalization. The i18n functionality is primarily available for the frontend application and can be configured using environment variables.

## Configuration

Internationalization is configured through environment variables in the Docker deployment. The following settings are available for customizing the language and locale behavior:

### Frontend i18n Configuration

The frontend application supports the following internationalization environment variables:

| Environment Variable | Description | Default Value | Example |
|---------------------|-------------|---------------|---------|
| `i18n_LOCALE` | Primary UI locale code (ISO 639-1 language codes) | `false` | `en`, `de`, `fr`, `it` |
| `I18N_FALLBACKLOCALE` | Fallback locale when the primary locale is not available | `false` | `en` |

### Configuration Example

To configure internationalization in your Docker Compose setup:

```yaml
services:
  negotiator-frontend:
    image: bbmrieric/negotiator-frontend:latest
    environment:
      - i18n_LOCALE=de
      - I18N_FALLBACKLOCALE=en
    # ... other configuration
```

## Supported Features

With the current internationalization implementation, you can:

- **Set UI Language**: Configure the primary language for the user interface
- **Define Fallback Language**: Specify a fallback locale when translations are missing
- **Add Project-Specific Terminology**: Customize terminology to match your organization's vocabulary

## Deployment Configuration

For detailed information about configuring these environment variables in your deployment, please refer to the [Deployment](/deployment) documentation, specifically the "Custom properties for the frontend Docker Container" section.

## Limitations

Please note the following limitations with the current i18n implementation:

- **Limited Language Support**: The extent of available translations may be limited
- **Frontend Only**: Internationalization is primarily implemented for the frontend application
- **Basic Configuration**: Advanced i18n features may not be fully supported

## Getting Help

If you need assistance with internationalization configuration or encounter issues with language settings, please refer to the deployment documentation or contact the development team.
