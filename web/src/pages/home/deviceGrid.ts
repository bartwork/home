/** Общая сетка квадратных плиток устройств. */
export const deviceGridSx = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fill, minmax(118px, 1fr))',
  gap: '10px',
  alignContent: 'start',
  justifyItems: 'stretch',
} as const
