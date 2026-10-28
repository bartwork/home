/** Типы, которые «Принять полезные» принимает массово. */
export const USEFUL_DEVICE_TYPES = ['switch', 'range', 'sensor', 'climate', 'led', 'floor', 'air'] as const

export const DEVICE_TYPE_OPTIONS = [
  { value: 'switch', label: 'выключатель' },
  { value: 'range', label: 'диммер / range' },
  { value: 'sensor', label: 'сенсор' },
  { value: 'climate', label: 'кондиционер' },
  { value: 'led', label: 'LED RGB' },
  { value: 'floor', label: 'тёплый пол' },
  { value: 'air', label: 'климат-датчик (MSW)' },
  { value: 'raw', label: 'raw' },
] as const

export function isUsefulDeviceType(typ: string): boolean {
  return (USEFUL_DEVICE_TYPES as readonly string[]).includes(typ)
}
