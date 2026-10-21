import type { OpcoesFiltro, Situacao } from '../api/tipos'
import { useDados } from '../api/useDados'
import { rotuloRegime, rotuloSituacao } from '../util/formato'
import type { useFiltrosUrl } from '../util/useFiltrosUrl'
import { Alternativas } from './Alternativas'
import { CampoBusca } from './CampoBusca'
import { SeletorMultiplo } from './SeletorMultiplo'
import './Filtros.css'

type Filtros = ReturnType<typeof useFiltrosUrl>

const SITUACOES: { valor: Situacao; rotulo: string }[] = [
  { valor: 'TRABALHANDO', rotulo: rotuloSituacao('TRABALHANDO') },
  { valor: 'AFASTADO', rotulo: rotuloSituacao('AFASTADO') },
  { valor: 'DESLIGADO', rotulo: rotuloSituacao('DESLIGADO') },
]

/** Faixa de filtros da consulta de vínculos (RF02). Os mesmos filtros servem à exportação. */
export function FiltrosVinculo({ filtros, comBusca = true, comDatas = true }: {
  filtros: Filtros
  comBusca?: boolean
  comDatas?: boolean
}) {
  const opcoes = useDados<OpcoesFiltro>('/filtros/opcoes')
  const o = opcoes.dados

  return (
    <section className="filtros" aria-label="Filtros">
      {comBusca && <CampoBusca valor={filtros.valor('q')} aoMudar={(t) => filtros.definir('q', t)} />}
      <Alternativas
        legenda="Situação"
        multiplo
        opcoes={SITUACOES}
        selecionados={filtros.lista('situacao') as Situacao[]}
        aoMudar={(v) => filtros.definir('situacao', v)}
      />
      {o && (
        <>
          <SeletorMultiplo
            rotulo="Regime"
            opcoes={o.regime}
            selecionados={filtros.lista('regime')}
            aoMudar={(v) => filtros.definir('regime', v)}
            exibir={rotuloRegime}
          />
          <SeletorMultiplo
            rotulo="Secretaria"
            opcoes={o.secretaria}
            selecionados={filtros.lista('secretaria')}
            aoMudar={(v) => filtros.definir('secretaria', v)}
          />
        </>
      )}
      {o && (
        <details className="filtros__mais" open={['cargo', 'localTrabalho', 'classificacao', 'admissaoInicio', 'admissaoFim', 'desligamentoInicio', 'desligamentoFim'].some((c) => filtros.valor(c))}>
          <summary>Mais filtros: cargo, local, classificação{comDatas ? ' e datas' : ''}</summary>
          <div className="filtros__grade">
            <SeletorMultiplo
              rotulo="Cargo"
              opcoes={o.cargo}
              selecionados={filtros.lista('cargo')}
              aoMudar={(v) => filtros.definir('cargo', v)}
            />
            <SeletorMultiplo
              rotulo="Local de trabalho"
              opcoes={o.localTrabalho}
              selecionados={filtros.lista('localTrabalho')}
              aoMudar={(v) => filtros.definir('localTrabalho', v)}
            />
            <SeletorMultiplo
              rotulo="Classificação"
              opcoes={o.classificacao}
              selecionados={filtros.lista('classificacao')}
              aoMudar={(v) => filtros.definir('classificacao', v)}
            />
            {comDatas && (
              <>
                <CampoData rotulo="Admitidos desde" valor={filtros.valor('admissaoInicio')} aoMudar={(v) => filtros.definir('admissaoInicio', v)} />
                <CampoData rotulo="Admitidos até" valor={filtros.valor('admissaoFim')} aoMudar={(v) => filtros.definir('admissaoFim', v)} />
                <CampoData rotulo="Desligados desde" valor={filtros.valor('desligamentoInicio')} aoMudar={(v) => filtros.definir('desligamentoInicio', v)} />
                <CampoData rotulo="Desligados até" valor={filtros.valor('desligamentoFim')} aoMudar={(v) => filtros.definir('desligamentoFim', v)} />
              </>
            )}
          </div>
        </details>
      )}
      {filtros.ativos > 0 && (
        <button type="button" className="botao-texto" onClick={() => filtros.limpar()}>
          Limpar filtros
        </button>
      )}
    </section>
  )
}

function CampoData({ rotulo, valor, aoMudar }: { rotulo: string; valor: string; aoMudar: (v: string) => void }) {
  return (
    <label className="campo campo--curto">
      <span className="campo__rotulo">{rotulo}</span>
      <input type="date" className="campo__entrada" value={valor} onChange={(e) => aoMudar(e.target.value)} />
    </label>
  )
}
