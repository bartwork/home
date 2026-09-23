/** Название продукта — для UI и document.title */
export const APP_NAME = 'Мой дом'

export function pageTitle(section?: string) {
  return section ? `${section} · ${APP_NAME}` : APP_NAME
}
