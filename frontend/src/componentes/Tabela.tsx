import type { ReactNode } from 'react'
import { urlApi, type Parametros } from '../api/cliente'
import { formatarInteiro } from '../util/formato'
import './Tabela.css'

/** Cabeçalho que ordena a tabela; clica de novo para inverter. */
export function CabecalhoOrdenavel({
  campo,
  rotulo,
  ordenacao,
  aoOrdenar,
  numerico = false,
  padraoDecrescente = false,
}: {
  campo: string
  rotulo: string
  ordenacao: string
  aoOrdenar: (ordenacao: string) => void
  numerico?: boolean
  padraoDecrescente?: boolean
}) {
  const [campoAtual, direcao] = ordenacao.split(',')
  const ativo = campoAtual === campo
  const proxima = ativo ? (direcao === 'asc' ? 'desc' : 'asc') : padraoDecrescente ? 'desc' : 'asc'
  return (
    <th scope="col" className={numerico ? 'numero' : undefined} aria-sort={ativo ? (direcao === 'asc' ? 'ascending' : 'descending') : undefined}>
      <button type="button" className="ordenar" onClick={() => aoOrdenar(`${campo},${proxima}`)}>
        {rotulo}
        <span className="ordenar__seta" aria-hidden="true">
          {ativo ? (direcao === 'asc' ? '▲' : '▼') : ''}
        </span>
      </button>
    </th>
  )
}

export function Paginacao({ pagina, tamanho, total, aoMudar }: {
  pagina: number
  tamanho: number
  total: number
  aoMudar: (pagina: number) => void
}) {
  const paginas = Math.max(1, Math.ceil(total / tamanho))
  if (total <= tamanho) return null
  return (
    <nav className="paginacao" aria-label="Páginas">
      <span>
        Página {formatarInteiro(pagina + 1)} de {formatarInteiro(paginas)}
      </span>
      <div className="paginacao__botoes">
        <button type="button" className="botao" disabled={pagina === 0} onClick={() => aoMudar(pagina - 1)}>
          Anterior
        </button>
        <button type="button" className="botao" disabled={pagina + 1 >= paginas} onClick={() => aoMudar(pagina + 1)}>
          Próxima
        </button>
      </div>
    </nav>
  )
}

const LIMITE_PDF = 5000

/** Botões de exportação com os mesmos filtros da tela (RF05). O PDF aceita até 5.000 linhas. */
export function Exportar({ relatorio, parametros, linhas }: { relatorio: string; parametros: Parametros; linhas: number }) {
  const semPagina = { ...parametros, pagina: undefined, tamanho: undefined }
  const pdfPermitido = linhas <= LIMITE_PDF
  return (
    <div className="exportar">
      <a className="botao" href={urlApi(`/exportacoes/${relatorio}`, { ...semPagina, formato: 'csv' })} download>
        Exportar CSV
      </a>
      {pdfPermitido ? (
        <a className="botao" href={urlApi(`/exportacoes/${relatorio}`, { ...semPagina, formato: 'pdf' })} download>
          Exportar PDF
        </a>
      ) : (
        <span className="botao" aria-disabled="true">
          Exportar PDF
        </span>
      )}
      {!pdfPermitido && (
        <span className="exportar__aviso">
          O PDF aceita até {formatarInteiro(LIMITE_PDF)} linhas. Use o CSV ou refine os filtros.
        </span>
      )}
    </div>
  )
}

export function BarraTabela({ total, rotulo, children }: { total: number; rotulo: string; children?: ReactNode }) {
  return (
    <div className="barra-tabela">
      <p className="barra-tabela__total" aria-live="polite">
        <strong>{formatarInteiro(total)}</strong> {rotulo}
      </p>
      {children}
    </div>
  )
}
