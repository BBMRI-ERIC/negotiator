export const compareByFields = (a, b, fields) => {
  const normalize = (v) => String(v ?? '').toLowerCase()
  for (const f of fields) {
    const aVal = normalize(a?.[f])
    const bVal = normalize(b?.[f])
    if (aVal || bVal) {
      const cmp = aVal.localeCompare(bVal, undefined, { numeric: true })
      if (cmp !== 0) return cmp
    }
  }
  return 0
}

export const sortActiveWithdrawn = (items, { isWithdrawn, idFields }) => {
  if (!Array.isArray(items) || items.length === 0) return []
  const active = items.filter((x) => !isWithdrawn(x)).sort((a, b) => compareByFields(a, b, idFields))
  const withdrawn = items.filter((x) => isWithdrawn(x)).sort((a, b) => compareByFields(a, b, idFields))
  return [...active, ...withdrawn]
}

export const sortOrganizations = (orgs) =>
  sortActiveWithdrawn(orgs, {
    isWithdrawn: (org) => !!org.withdrawn,
    idFields: ['id', 'externalId'],
  })

export const sortResources = (resources) =>
  sortActiveWithdrawn(resources, {
    isWithdrawn: (r) => !!r.withdrawn || r.status === 'withdrawn',
    idFields: ['id', 'sourceId'],
  })