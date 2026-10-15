/** Сравнение MQTT payload для optimistic confirm (switch/range). */
export function samePayload(a: string, b: string): boolean {
  const left = a.trim()
  const right = b.trim()
  if (left === right) return true
  const n1 = Number.parseFloat(left)
  const n2 = Number.parseFloat(right)
  if (Number.isFinite(n1) && Number.isFinite(n2)) {
    return Math.round(n1) === Math.round(n2)
  }
  return false
}
