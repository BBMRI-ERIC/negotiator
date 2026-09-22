export const getNegotiationTitle = (payload) => {
  if (!payload) return ''
  const section = Object.values(payload).find((section) => section && 'title' in section)
  return section?.title ?? ''
}
