// Ordem fixa das séries, validada pela skill de dataviz (scripts/validate_palette.js, modo claro,
// superfície #F5F7F6): todas as verificações passam. A cor segue a entidade, nunca a posição.
/** Mesmos valores das variáveis --serie-* do tokens.css, em hex para o Recharts e para calcular contraste. */
export const SERIES_HEX = ['#2447a6', '#c8741a', '#0f9480', '#6b3fa0', '#c44e7a', '#7a8b1f']
export const OUTROS_HEX = '#8a949b'
export const PAUTA_HEX = '#e3e8eb'
export const TINTA_APAGADA_HEX = '#6b7883'
export const PAPEL_HEX = '#f5f7f6'

/** Branco ou tinta sobre uma cor de preenchimento, pelo contraste maior (WCAG). */
export function corDoTextoSobre(hex: string): string {
  const canal = (i: number) => {
    const c = parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  }
  const luminancia = 0.2126 * canal(0) + 0.7152 * canal(1) + 0.0722 * canal(2)
  const contrasteBranco = 1.05 / (luminancia + 0.05)
  const contrasteTinta = (luminancia + 0.05) / 0.0718 // luminância da tinta #1d2a36 + 0,05
  return contrasteBranco >= contrasteTinta ? '#ffffff' : '#1d2a36'
}
