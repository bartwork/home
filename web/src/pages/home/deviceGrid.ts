import type { Device } from '../../shared/api/types'

/** Общая сетка квадратных плиток устройств. */
export const deviceGridSx = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(132px, 1fr))',
  gap: '12px',
  alignContent: 'start',
  alignItems: 'stretch',
  justifyItems: 'stretch',
} as const

const WIDE_TYPES = new Set(['climate', 'led', 'floor'])

/** Широкие виджеты (climate, led, floor) занимают две колонки сетки. */
export function wideDeviceGridItemSx(deviceType: string): { gridColumn: 'span 2' } | undefined {
  return WIDE_TYPES.has(deviceType) ? { gridColumn: 'span 2' } : undefined
}

/** Сортировка плиток: y, затем x (после DnD — y=0, x=индекс). */
export function sortDevicesByLayout(devices: Device[]): Device[] {
  return [...devices].sort((a, b) => a.y - b.y || a.x - b.x || a.id - b.id)
}

/** Линейные ключи порядка — не зависят от числа колонок CSS grid. */
export function withLinearOrder(devices: Device[]): Device[] {
  return devices.map((d, i) => ({ ...d, x: i, y: 0 }))
}
