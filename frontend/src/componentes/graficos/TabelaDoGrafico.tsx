import type { ReactNode } from 'react'

interface Props {
  resumo: string
  colunas: string[]
  linhas: ReactNode[][]
  /** Índices das colunas numéricas, alinhadas à direita. */
  numericas?: number[]
}

/** Visão em tabela do gráfico (acessibilidade): todo valor fica legível sem cor e sem mouse. */
export function TabelaDoGrafico({ resumo, colunas, linhas, numericas = [] }: Props) {
  return (
    <details className="ver-tabela">
      <summary>Ver os números em tabela</summary>
      <table>
        <caption className="visualmente-oculto">{resumo}</caption>
        <thead>
          <tr>
            {colunas.map((c, i) => (
              <th key={c} scope="col" className={numericas.includes(i) ? 'numero' : undefined}>
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {linhas.map((linha, i) => (
            <tr key={i}>
              {linha.map((celula, j) => (
                <td key={j} className={numericas.includes(j) ? 'numero' : undefined}>
                  {celula}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </details>
  )
}
