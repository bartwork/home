/** Ограничивает позицию/размер комнаты в % плана (как layout.ClampRect на Go). */
export function clampRect(x: number, y: number, w: number, h: number) {
  let nx = x
  let ny = y
  let nw = w
  let nh = h
  if (nw < 5) nw = 5
  if (nh < 5) nh = 5
  if (nw > 100) nw = 100
  if (nh > 100) nh = 100
  if (nx < 0) nx = 0
  if (ny < 0) ny = 0
  if (nx > 95) nx = 95
  if (ny > 95) ny = 95
  if (nx + nw > 100) nw = 100 - nx
  if (ny + nh > 100) nh = 100 - ny
  if (nw < 5) nw = 5
  if (nh < 5) nh = 5
  return { x: round1(nx), y: round1(ny), w: round1(nw), h: round1(nh) }
}

function round1(n: number) {
  return Math.round(n * 10) / 10
}
