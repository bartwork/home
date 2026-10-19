/** Sibling-топик: …/controls/<якорь> → …/controls/<control>. */
export function siblingControlTopic(mqttTopic: string, control: string): string {
  const topic = mqttTopic.trim()
  const marker = '/controls/'
  const idx = topic.lastIndexOf(marker)
  if (idx < 0 || !control) return ''
  return topic.slice(0, idx + marker.length) + control
}

export function parseControlNum(raw: string | undefined): number | null {
  if (raw === undefined || raw === '' || raw === '—') return null
  const n = Number(String(raw).replace(',', '.'))
  return Number.isFinite(n) ? n : null
}
