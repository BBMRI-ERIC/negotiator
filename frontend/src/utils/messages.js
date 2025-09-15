import i18nConfig from '../config/i18n'

const getOrganisationLabel = (locale = i18nConfig.locale) => {
  const messages = i18nConfig.messages[locale]?.negotiationPage?.organisations;
  return messages.split(' | ')[1] || messages;
}

export const getNoResultsMessage = (loading, hasSearchFilters, statusFilter, locale = i18nConfig.locale) => {
  const orgLabel = getOrganisationLabel(locale);
  if (loading) return `Loading ${orgLabel.toLowerCase()}...`;
  if (hasSearchFilters) return `No ${orgLabel.toLowerCase()} found matching your search criteria.`;
  if (statusFilter === 'withdrawn') return `No withdrawn ${orgLabel.toLowerCase()} found.`;
  if (statusFilter === 'active') return `No active ${orgLabel.toLowerCase()} found.`;
  return `No ${orgLabel.toLowerCase()} found.`;
}