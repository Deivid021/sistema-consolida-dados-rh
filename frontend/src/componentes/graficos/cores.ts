import { useMemo } from 'react'
import { useTema } from '../../util/tema'

/**
 * Cores dos gráficos no tema em uso, lidas dos tokens --serie-* e afins do tokens.css (a única
 * fonte das cores). Ordem fixa das séries, validada pela skill de dataviz nos dois temas
 * (scripts/validate_palette.js: claro sobre #F5F7F6, escuro sobre #0F1724). A cor segue a
 * entidade, nunca a posição; na troca de tema só muda o passo de luminosidade.
 */
export interface Paleta {
  series: string[]
  outros: string
  /** Linhas de grade: discretas, não carregam informação. */
  grade: string
  /** Linha do eixo. */
  eixo: string
  textoEixo: string
  fundo: string
  tinta: string
  /** Contraste mínimo de texto no tema: 7:1 no escuro de alto contraste (RNF04), 4,5:1 no claro. */
  minimoTexto: number
}

function token(nome: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(`--${nome}`).trim()
}

export function usePaleta(): Paleta {
  const tema = useTema()
  return useMemo(() => {
    const escuro = tema === 'escuro'
    return {
      series: [1, 2, 3, 4, 5, 6].map((i) => token(`serie-${i}`)),
      outros: token('serie-outros'),
      grade: token('pauta-leve'),
      eixo: token(escuro ? 'pauta' : 'pauta-leve'),
      textoEixo: token('tinta-apagada'),
      fundo: token('papel'),
      tinta: token('tinta'),
      minimoTexto: escuro ? 7 : 4.5,
    }
  }, [tema])
}

function luminancia(hex: string): number {
  const canal = (i: number) => {
    const c = parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16) / 255
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4
  }
  return 0.2126 * canal(0) + 0.7152 * canal(1) + 0.0722 * canal(2)
}

function contraste(a: string, b: string): number {
  const [maior, menor] = [luminancia(a), luminancia(b)].sort((x, y) => y - x)
  return (maior + 0.05) / (menor + 0.05)
}

/**
 * Como escrever um rótulo dentro de uma marca colorida: em branco ou na tinta do tema, pelo
 * contraste maior. Se nenhum dos dois chegar ao mínimo do tema, o rótulo ganha uma etiqueta
 * de papel por baixo (no escuro isso vale para todas as séries; research.md §2).
 */
export function rotuloSobre(fundo: string, paleta: Paleta): { cor: string; comFundo: boolean } {
  const branco = contraste('#ffffff', fundo)
  const tinta = contraste(paleta.tinta, fundo)
  const melhor = Math.max(branco, tinta)
  return { cor: branco >= tinta ? '#ffffff' : paleta.tinta, comFundo: melhor < paleta.minimoTexto }
}
