/** Парсит число из поля ввода; запятая как десятичный разделитель. */
export function parseNum(v: string, fallback: number): number {
  const n = Number.parseFloat(v.replace(',', '.'))
  return Number.isFinite(n) ? n : fallback
}
