import type { PaginaAlertas, RegraAlerta } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Alternativas } from '../componentes/Alternativas'
import { Cabecalho } from '../componentes/Cabecalho'
import { CampoBusca } from '../componentes/CampoBusca'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { BarraTabela, Exportar, Paginacao } from '../componentes/Tabela'
import {
  formatarDiferencaPercentual,
  formatarInteiro,
  formatarMoeda,
  rotuloNivel,
  rotuloRegra,
  textoOuNaoInformado,
} from '../util/formato'
import { useFiltrosUrl } from '../util/useFiltrosUrl'
import '../componentes/Filtros.css'
import './Alertas.css'

type Escolha = RegraAlerta | 'TODAS'

export function Alertas() {
  const filtros = useFiltrosUrl([])
  const regra = (filtros.valor('regra') || 'TODAS') as Escolha
  const pagina = Number(filtros.valor('pagina') || 0)
  const consulta = useDados<PaginaAlertas>('/alertas', {
    regra: regra === 'TODAS' ? undefined : regra,
    q: filtros.valor('q') || undefined,
    pagina,
    tamanho: 25,
  })
  const dados = consulta.dados

  return (
    <>
      <Cabecalho titulo="Alertas">
        <p>
          Vínculos cujo salário base merece uma conferência. Um alerta é um indício, não uma irregularidade: pode
          haver decisão judicial, incorporação ou erro de cadastro na origem.
        </p>
      </Cabecalho>

      {dados && (
        <dl className="alertas__regras">
          <div>
            <dt>Acima do teto</dt>
            <dd>
              {dados.teto.valor === null ? (
                <>Regra suspensa: não há prefeito ativo na fonte nem valor configurado.</>
              ) : (
                <>
                  Salário base maior que {formatarMoeda(dados.teto.valor)},{' '}
                  {dados.teto.origem === 'FONTE' ? 'o salário base do prefeito publicado pela fonte' : textoOuNaoInformado(dados.teto.fundamento)}.
                </>
              )}
            </dd>
          </div>
          <div>
            <dt>Fora do padrão</dt>
            <dd>
              Salário base mais de {formatarInteiro(dados.percentualForaPadrao)}% acima ou abaixo da mediana dos
              vínculos ativos com o mesmo cargo, nível e carga horária, em grupos de pelo menos 5.
            </dd>
          </div>
        </dl>
      )}

      <section className="filtros" aria-label="Filtros">
        <Alternativas
          legenda="Regra"
          opcoes={[
            { valor: 'TODAS' as Escolha, rotulo: 'Todas' },
            { valor: 'TETO' as Escolha, rotulo: rotuloRegra('TETO') },
            { valor: 'FORA_PADRAO' as Escolha, rotulo: rotuloRegra('FORA_PADRAO') },
          ]}
          selecionados={[regra]}
          aoMudar={([r]) => filtros.definir('regra', r === 'TODAS' ? null : r)}
        />
        <CampoBusca valor={filtros.valor('q')} aoMudar={(t) => filtros.definir('q', t)} />
      </section>

      {consulta.erro && <Erro mensagem={consulta.erro} />}
      {!dados && !consulta.erro && <Carregando texto="Consultando os alertas…" />}

      {dados && (
        <>
          <BarraTabela total={dados.total} rotulo={dados.total === 1 ? 'vínculo sinalizado' : 'vínculos sinalizados'}>
            {dados.total > 0 && (
              <Exportar
                relatorio="alertas"
                parametros={{ regra: regra === 'TODAS' ? undefined : regra, q: filtros.valor('q') || undefined }}
                linhas={dados.total}
              />
            )}
          </BarraTabela>

          {dados.total === 0 ? (
            <Vazio titulo="Nenhum vínculo sinalizado com esses filtros.">
              Os alertas são recalculados a cada carga diária.
            </Vazio>
          ) : (
            <ol className="alertas__lista">
              {dados.itens.map((a) => (
                <li key={`${a.regra}-${a.vinculo.matricula}`} className="alerta">
                  <div className="alerta__quem">
                    <span className="alerta__regra">{rotuloRegra(a.regra)}</span>
                    <span className="tabela__principal">{textoOuNaoInformado(a.vinculo.nome)}</span>
                    <span className="tabela__secundario">
                      {textoOuNaoInformado(a.vinculo.cargo)}
                      {a.vinculo.nivel && `, ${rotuloNivel(a.vinculo.nivel)?.toLowerCase()}`}. Matrícula {a.vinculo.matricula}.
                    </span>
                    <span className="tabela__secundario">{textoOuNaoInformado(a.vinculo.secretaria)}</span>
                  </div>
                  <dl className="alerta__valores">
                    <div>
                      <dt>Salário base</dt>
                      <dd>{formatarMoeda(a.valorVinculo)}</dd>
                    </div>
                    <div>
                      <dt>{a.regra === 'TETO' ? 'Teto' : 'Mediana do grupo'}</dt>
                      <dd>{formatarMoeda(a.valorReferencia)}</dd>
                    </div>
                    <div>
                      <dt>Diferença</dt>
                      <dd className="alerta__diferenca">{formatarDiferencaPercentual(a.diferencaPercentual)}</dd>
                    </div>
                  </dl>
                  <p className="alerta__criterio">{a.descricaoRegra}</p>
                </li>
              ))}
            </ol>
          )}
          <Paginacao pagina={dados.pagina} tamanho={dados.tamanho} total={dados.total} aoMudar={(p) => filtros.definir('pagina', String(p))} />
        </>
      )}
    </>
  )
}
