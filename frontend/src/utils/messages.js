export const getNoResultsMessage = (loading, hasSearchFilters, statusFilter) => {
  if (loading) return 'Loading organizations...'
  if (hasSearchFilters) return 'No organizations found matching your search criteria.'
  if (statusFilter === 'withdrawn') return 'No withdrawn organizations found.'
  if (statusFilter === 'active') return 'No active organizations found.'
  return 'No organizations found.'
}