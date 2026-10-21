import type { Status } from '../api/tipos'
import { formatarDataHora } from '../util/formato'
import './Carimbo.css'

/**
 * Data da última atualização (FR-013), no formato de um carimbo de protocolo. Aparece em
 * todas as telas, porque toda leitura dos números depende de saber de quando eles são.
 */
export function Carimbo({ status, carregando }: { status: Status | null; carregando: boolean }) {
  if (carregando && !status) {
    return <div className="carimbo carimbo--vazio" aria-busy="true">Consultando a última atualização…</div>
  }
  if (!status?.ultimaAtualizacao) {
    return (
      <div className="carimbo carimbo--vazio">
        Ainda não há dados importados. A primeira carga roda às 02:00.
      </div>
    )
  }
  return (
    <div className="carimbo">
      <span className="carimbo__rotulo">Dados atualizados em</span>
      <time className="carimbo__data" dateTime={status.ultimaAtualizacao}>
        {formatarDataHora(status.ultimaAtualizacao)}
      </time>
      <span className="carimbo__fonte">Fonte: dados abertos de pessoal da Prefeitura de Gravataí</span>
    </div>
  )
}
