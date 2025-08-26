#!/bin/bash

echo "Replacing env constants in JS"

for file in assets/index*.js;
 do
  echo "Processing $file ..."

  sed -i -e "s|AUTH_URL_PLACEHOLDER|${AUTH_URL:-default_auth_url}|g" \
         -e "s|CLIENT_ID_PLACEHOLDER|${CLIENT_ID:-default_client_id}|g" \
         -e "s|REDIRECT_URI_PLACEHOLDER|${REDIRECT_URI:-default_redirect_uri}|g" \
         -e "s|SCOPES_PLACEHOLDER|${SCOPES:-default_scopes}|g" \
         -e "s|LOGOUT_URI_PLACEHOLDER|${LOGOUT_URI:-default_logout_uri}|g" \
         -e "s|RESOURCES_PLACEHOLDER|${API_RESOURCES:-default_resources}|g" \
         -e "s|MATOMO_HOST_PLACEHOLDER|${MATOMO_HOST:-default_matomo_host}|g" \
         -e "s|MATOMO_SITE_ID_PLACEHOLDER|${MATOMO_SITE_ID:-default_site_id}|g" \
         -e "s|I18N_LOCALE_PLACEHOLDER|${I18N_LOCALE:-false}|g" \
         -e "s|I18N_FALLBACKLOCALE_PLACEHOLDER|${I18N_FALLBACKLOCALE:-false}|g" \
         -e "s|FEATURE_FLAG_FAQPAGE_PLACEHOLDER|${FEATURE_FLAG_FAQPAGE:-false}|g" \
         -e "s|FEATURE_FLAG_NETWORKS_PLACEHOLDER|${FEATURE_FLAG_NETWORKS:-false}|g" \
         -e "s|FEATURE_FLAG_VUETOUR_PLACEHOLDER|${FEATURE_FLAG_VUETOUR:-false}|g" \
         -e "s|FEATURE_FLAG_NOTIFICATIONS|${FEATURE_FLAG_NOTIFICATIONS:-false}|g" \
         -e "s|DEV_MODE_PLACEHOLDER|${DEV_MODE:-false}|g" \
         -e "s|AUTH_MANAGEMENT_LINK_PLACEHOLDER|${AUTH_MANAGEMENT_LINK:-none}|g" "$file"
done

exec nginx -g 'daemon off;'

