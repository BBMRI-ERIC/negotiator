#!/bin/bash

echo "Replacing env constants in JS"

for file in assets/*.js;
 do
  echo "Processing $file ..."

  sed -i -e "s|AUTH_URL_PLACEHOLDER|${AUTH_URL:-default_auth_url}|g" \
         -e "s|CLIENT_ID_PLACEHOLDER|${CLIENT_ID:-default_client_id}|g" \
         -e "s|REDIRECT_URI_PLACEHOLDER|${REDIRECT_URI:-default_redirect_uri}|g" \
         -e "s|SCOPES_PLACEHOLDER|${SCOPES:-default_scopes}|g" \
         -e "s|LOGOUT_URI_PLACEHOLDER|${LOGOUT_URI:-default_logout_uri}|g" \
         -e "s|SILENT_REDIRECT_URI_PLACEHOLDER|${SILENT_REDIRECT_URI:-default_silent_redirect_uri}|g" \
         -e "s|RESOURCES_PLACEHOLDER|${API_RESOURCES:-default_resources}|g" \
         -e "s|MATOMO_HOST_PLACEHOLDER|${MATOMO_HOST:-default_matomo_host}|g" \
         -e "s|MATOMO_SITE_ID_PLACEHOLDER|${MATOMO_SITE_ID:-default_site_id}|g" \
         -e "s|I18N_LOCALE_PLACEHOLDER|${I18N_LOCALE:-en}|g" \
         -e "s|I18N_FALLBACKLOCALE_PLACEHOLDER|${I18N_FALLBACKLOCALE:-en}|g" \
         -e "s|FEATURE_FLAG_FAQPAGE_PLACEHOLDER|${FEATURE_FLAG_FAQPAGE:-false}|g" \
         -e "s|FEATURE_FLAG_NETWORKS_PLACEHOLDER|${FEATURE_FLAG_NETWORKS:-false}|g" \
         -e "s|FEATURE_FLAG_NOTIFICATIONS|${FEATURE_FLAG_NOTIFICATIONS:-false}|g" \
         -e "s|DEV_MODE_PLACEHOLDER|${DEV_MODE:-false}|g" \
         -e "s|AUTH_MANAGEMENT_LINK_PLACEHOLDER|${AUTH_MANAGEMENT_LINK:-none}|g" \
         -e "s|GOVERNANCE_CLIENT_PLACEHOLDER|${GOVERNANCE_CLIENT:-default-governance_client}|g" \
         -e "s|OIDC_API_PLACEHOLDER|${OIDC_API:-default-oidc_api}|g" \
         -e "s|VIRTUAL_ORGANIZATION_PLACEHOLDER|${VIRTUAL_ORGANIZATION:-default-virtual_organization}|g" \
         -e "s|ORGANIZATION_ID_ATTR_PLACEHOLDER|${ORGANIZATION_ID_ATTR:-default-organization_id_attr}|g" \
         -e "s|RESOURCE_ID_ATTR_PLACEHOLDER|${RESOURCE_ID_ATTR:-default-resource_id_attr}|g" \
         -e "s|ADMIN_ORGANIZATION_ID_ATTR_PLACEHOLDER|${ADMIN_ORGANIZATION_ID_ATTR:-default-admin_organization_id_attr}|g" \
         -e "s|ADMIN_RESOURCE_ID_ATTR_PLACEHOLDER|${ADMIN_RESOURCE_ID_ATTR:-default-admin_resource_id_attr}|g" \
         -e "s|GROUP_ATTR_BASE_PLACEHOLDER|${GROUP_ATTR_BASE:-default-group_attr_base}|g" \
         -e "s|USER_ATTR_BASE_PLACEHOLDER|${USER_ATTR_BASE:-default-user_attr_base}|g" \
         -e "s|EMAIL_ATTR_PLACEHOLDER|${EMAIL_ATTR:-default-email_attr}|g" "$file"
done

exec nginx -g 'daemon off;'

