import { useSyncExternalStore } from 'react'

/** Aparências do sistema: o ofício claro e o Modo Escuro de Alto Contraste (RNF04). */
export type Tema = 'claro' | 'escuro'

/**
 * Chave da escolha no navegador. O script do index.html lê a mesma chave para aplicar o
 * tema antes da primeira pintura, sem clarão (FR-007).
 */
const CHAVE = 'tema'

const preferenciaEscura = window.matchMedia('(prefers-color-scheme: dark)')
const ouvintes = new Set<() => void>()

/** Escolha feita nesta visita, para quando o navegador não deixa guardar (janela anônima). */
let escolhaDaVisita: Tema | null = null

function escolhaGuardada(): Tema | null {
  try {
    const valor = localStorage.getItem(CHAVE)
    if (valor === 'claro' || valor === 'escuro') return valor
  } catch {
    // Armazenamento bloqueado: vale a escolha desta visita ou a do sistema operacional
  }
  return escolhaDaVisita
}

function aplicar(tema: Tema) {
  document.documentElement.dataset.tema = tema
  ouvintes.forEach((avisar) => avisar())
}

/** Enquanto a pessoa não escolheu, o tema acompanha o sistema operacional (FR-005). */
preferenciaEscura.addEventListener('change', (evento) => {
  if (!escolhaGuardada()) aplicar(evento.matches ? 'escuro' : 'claro')
})

function temaAtual(): Tema {
  return document.documentElement.dataset.tema === 'escuro' ? 'escuro' : 'claro'
}

function inscrever(avisar: () => void) {
  ouvintes.add(avisar)
  return () => ouvintes.delete(avisar)
}

/** Troca o tema na hora e guarda a escolha para as próximas visitas (FR-003, FR-006). */
export function escolherTema(tema: Tema) {
  escolhaDaVisita = tema
  try {
    localStorage.setItem(CHAVE, tema)
  } catch {
    // Sem armazenamento a escolha vale só até fechar a página
  }
  aplicar(tema)
}

/** Tema em uso; o componente se redesenha quando ele muda. */
export function useTema(): Tema {
  return useSyncExternalStore(inscrever, temaAtual)
}
