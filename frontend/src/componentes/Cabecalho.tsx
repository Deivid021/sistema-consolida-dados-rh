import type { ReactNode } from 'react'
import './Cabecalho.css'

/** Título da tela e uma frase dizendo o que ela responde. */
export function Cabecalho({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return (
    <header className="cabecalho">
      <h1>{titulo}</h1>
      {children && <div className="cabecalho__texto">{children}</div>}
    </header>
  )
}
