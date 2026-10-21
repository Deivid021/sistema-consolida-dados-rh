import type { Granularidade, MotivosRescisao, Movimentacao as DadosMovimentacao, OpcoesFiltro } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Alternativas } from '../componentes/Alternativas'
import { Cabecalho } from '../componentes/Cabecalho'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { BarrasHorizontais } from '../componentes/graficos/BarrasHorizontais'
import { LinhasTempo } from '../componentes/graficos/LinhasTempo'
import { SeletorMultiplo } from '../componentes/SeletorMultiplo'
import { formatarInteiro, rotuloRegime } from '../util/formato'
import { useFiltrosUrl } from '../util/useFiltrosUrl'
import '../componentes/Filtros.css'
import './Movimentacao.css'

const LISTAS = ['regime', 'secretaria', 'cargo'] as const
const ANO_ATUAL = new Date().getFullYear()
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

/** "2026-03" -> "mar/2026"; "2026" fica como está. */
function rotuloPeriodo(periodo: string): string {
  if (periodo.length !== 7) return periodo
  const [ano, mes] = periodo.split('-')
  return `${MESES[Number(mes) - 1]}/${ano}`
}

export function Movimentacao() {
  const filtros = useFiltrosUrl(LISTAS)
  const granularidade = (filtros.valor('granularidade') || 'ANO') as Granularidade
  const inicio = filtros.valor('inicio') || (granularidade === 'ANO' ? `${ANO_ATUAL - 15}-01-01` : `${ANO_ATUAL - 2}-01-01`)
  const fim = filtros.valor('fim') || `${ANO_ATUAL}-12-31`
  const recorte = { regime: filtros.lista('regime'), secretaria: filtros.lista('secretaria'), cargo: filtros.lista('cargo') }

  const opcoes = useDados<OpcoesFiltro>('/filtros/opcoes')
  const movimentacao = useDados<DadosMovimentacao>('/graficos/movimentacao', { granularidade, inicio, fim, ...recorte })
  const motivos = useDados<MotivosRescisao>('/graficos/motivos-rescisao', { inicio, fim, ...recorte })

  const totais = movimentacao.dados?.periodos.reduce(
    (t, p) => ({ admissoes: t.admissoes + p.admissoes, desligamentos: t.desligamentos + p.desligamentos }),
    { admissoes: 0, desligamentos: 0 },
  )

  return (
    <>
      <Cabecalho titulo="Entradas e saídas">
        <p>
          Quantas pessoas foram admitidas e desligadas em cada período, e por que saíram. Desligamento inclui
          exoneração, aposentadoria, falecimento e fim de contrato.
        </p>
      </Cabecalho>

      <section className="filtros" aria-label="Período e recorte">
        <Alternativas
          legenda="Agrupar"
          opcoes={[
            { valor: 'ANO' as Granularidade, rotulo: 'Por ano' },
            { valor: 'MES' as Granularidade, rotulo: 'Por mês' },
          ]}
          selecionados={[granularidade]}
          aoMudar={([g]) => filtros.definir({ granularidade: g, inicio: null })}
        />
        <label className="campo campo--curto">
          <span className="campo__rotulo">De</span>
          <input type="date" className="campo__entrada" value={inicio} onChange={(e) => filtros.definir('inicio', e.target.value)} />
        </label>
        <label className="campo campo--curto">
          <span className="campo__rotulo">Até</span>
          <input type="date" className="campo__entrada" value={fim} onChange={(e) => filtros.definir('fim', e.target.value)} />
        </label>
        {opcoes.dados && (
          <>
            <SeletorMultiplo
              rotulo="Regime"
              opcoes={opcoes.dados.regime}
              selecionados={filtros.lista('regime')}
              aoMudar={(v) => filtros.definir('regime', v)}
              exibir={rotuloRegime}
            />
            <SeletorMultiplo
              rotulo="Secretaria"
              opcoes={opcoes.dados.secretaria}
              selecionados={filtros.lista('secretaria')}
              aoMudar={(v) => filtros.definir('secretaria', v)}
            />
          </>
        )}
        {filtros.ativos > 0 && (
          <button type="button" className="botao-texto" onClick={() => filtros.limpar()}>
            Voltar ao padrão
          </button>
        )}
      </section>

      {movimentacao.erro && <Erro mensagem={movimentacao.erro} />}
      {!movimentacao.dados && !movimentacao.erro && <Carregando />}

      {movimentacao.dados && totais && (
        <section className="movimentacao__grafico" aria-labelledby="titulo-mov">
          <h2 id="titulo-mov" className="movimentacao__resumo">
            {formatarInteiro(totais.admissoes)} admissões e {formatarInteiro(totais.desligamentos)} desligamentos no
            período.
          </h2>
          <LinhasTempo
            dados={movimentacao.dados.periodos}
            series={[
              { chave: 'admissoes', rotulo: 'Admissões' },
              { chave: 'desligamentos', rotulo: 'Desligamentos' },
            ]}
            descricao={`Admissões e desligamentos ${granularidade === 'ANO' ? 'por ano' : 'por mês'}`}
            rotuloPeriodo={rotuloPeriodo}
            altura={320}
          />
        </section>
      )}

      {motivos.dados && (
        <section className="movimentacao__motivos" aria-labelledby="titulo-motivos-mov">
          <h2 id="titulo-motivos-mov">Por que saíram</h2>
          {motivos.dados.total === 0 ? (
            <Vazio titulo="Nenhum desligamento no período escolhido.">Amplie o período ou remova um filtro.</Vazio>
          ) : (
            <BarrasHorizontais
              medida="desligamentos"
              descricao="Motivos de desligamento no período"
              barras={motivos.dados.motivos.map((m) => ({ rotulo: m.motivo, valor: m.quantidade }))}
            />
          )}
        </section>
      )}
    </>
  )
}
