/** Типы, которые «Принять полезные» принимает массово. */
export const USEFUL_DEVICE_TYPES = ['switch', 'range', 'sensor'] as const

export function isUsefulDeviceType(typ: string): boolean {
  return (USEFUL_DEVICE_TYPES as readonly string[]).includes(typ)
}
