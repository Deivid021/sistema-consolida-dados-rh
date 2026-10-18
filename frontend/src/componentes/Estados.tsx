import type { ReactNode } from 'react'
import './Estados.css'

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <p className="estado estado--carregando" role="status" aria-live="polite">
      {texto}
    </p>
  )
}

/** Erros dizem o que aconteceu e o que fazer, sem pedir desculpas. */
export function Erro({ mensagem }: { mensagem: string }) {
  return (
    <div className="estado estado--erro" role="alert">
      <strong>Não foi possível carregar.</strong> {mensagem}
    </div>
  )
}

/** Tela vazia é um convite a agir. */
export function Vazio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <div className="estado estado--vazio">
      <strong>{titulo}</strong>
      {children && <div>{children}</div>}
    </div>
  )
}
