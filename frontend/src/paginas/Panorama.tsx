import { Link } from 'react-router'
import type { MotivosRescisao, Movimentacao, PaginaAlertas, Quadro, Status } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { BarrasHorizontais } from '../componentes/graficos/BarrasHorizontais'
import { LinhasTempo } from '../componentes/graficos/LinhasTempo'
import { Regua } from '../componentes/graficos/Regua'
import {
  formatarInteiro,
  formatarMoeda,
  formatarPercentual,
  rotuloRegime,
  rotuloRegimePlural,
  textoOuNaoInformado,
} from '../util/formato'
import './Panorama.css'

const ATIVOS = { situacao: ['TRABALHANDO', 'AFASTADO'] }
const ANO_ATUAL = new Date().getFullYear()
const ANO_INICIAL_SERIE = ANO_ATUAL - 15

export function Panorama() {
  const status = useDados<Status>('/status')
  const quadro = useDados<Quadro>('/indicadores/quadro', ATIVOS)
  const movimentacao = useDados<Movimentacao>('/graficos/movimentacao', {
    granularidade: 'ANO',
    inicio: `${ANO_INICIAL_SERIE}-01-01`,
    fim: `${ANO_ATUAL}-12-31`,
  })
  const motivos = useDados<MotivosRescisao>('/graficos/motivos-rescisao', {
    inicio: `${ANO_ATUAL}-01-01`,
    fim: `${ANO_ATUAL}-12-31`,
  })
  const alertas = useDados<PaginaAlertas>('/alertas', { tamanho: 1 })

  if (status.erro) return <Erro mensagem={status.erro} />
  if (!status.dados || !quadro.dados) return <Carregando texto="Montando o panorama…" />
  if (!status.dados.ultimaAtualizacao) {
    return (
      <Vazio titulo="Ainda não há dados importados.">
        A carga automática roda todo dia às 02:00 e traz os dados de pessoal publicados pela Prefeitura.
      </Vazio>
    )
  }

  const ativos = quadro.dados.total
  const maiorRegime = quadro.dados.porRegime[0]
  const secretarias = quadro.dados.porSecretaria.slice(0, 10)

  return (
    <article className="panorama">
      <header className="panorama__abertura">
        <h1 className="panorama__manchete">
          Gravataí tem {formatarInteiro(ativos)} vínculos ativos.{' '}
          {maiorRegime?.valor && (
            <>
              {formatarInteiro(maiorRegime.quantidade)} deles, {formatarPercentual(maiorRegime.quantidade / ativos)},
              são {rotuloRegimePlural(maiorRegime.valor)}.
            </>
          )}
        </h1>
        <p className="panorama__explicacao">
          Ativo é quem está trabalhando ou afastado, o que inclui aposentados e pensionistas pagos pela Prefeitura.
          A contagem é por matrícula: a mesma pessoa pode ter mais de um vínculo.
        </p>
      </header>

      <section className="panorama__regua" aria-labelledby="titulo-regua">
        <h2 id="titulo-regua" className="visualmente-oculto">
          Vínculos ativos por regime
        </h2>
        <Regua
          descricao={`${formatarInteiro(ativos)} vínculos ativos divididos por regime`}
          partes={quadro.dados.porRegime.map((r) => ({
            chave: r.valor ?? 'NAO_INFORMADO',
            rotulo: rotuloRegime(r.valor),
            valor: r.quantidade,
          }))}
        />
      </section>

      <div className="panorama__colunas">
        <section aria-labelledby="titulo-secretarias">
          <div className="panorama__cabeca">
            <h2 id="titulo-secretarias">Onde estão</h2>
            <p>As dez secretarias e centros de custo com mais vínculos ativos.</p>
          </div>
          <BarrasHorizontais
            medida="vínculos ativos"
            descricao="Vínculos ativos nas dez secretarias com mais vínculos"
            barras={secretarias.map((s) => ({ rotulo: textoOuNaoInformado(s.valor), valor: s.quantidade }))}
          />
          <Link className="panorama__mais" to="/servidores?situacao=TRABALHANDO&situacao=AFASTADO">
            Consultar os servidores ativos
          </Link>
        </section>

        <section aria-labelledby="titulo-entradas">
          <div className="panorama__cabeca">
            <h2 id="titulo-entradas">Entradas e saídas</h2>
            <p>
              Admissões e desligamentos por ano desde {ANO_INICIAL_SERIE}. {ANO_ATUAL} ainda está em andamento.
            </p>
          </div>
          {movimentacao.erro && <Erro mensagem={movimentacao.erro} />}
          {movimentacao.dados && (
            <LinhasTempo
              dados={movimentacao.dados.periodos}
              series={[
                { chave: 'admissoes', rotulo: 'Admissões' },
                { chave: 'desligamentos', rotulo: 'Desligamentos' },
              ]}
              descricao={`Admissões e desligamentos por ano, de ${ANO_INICIAL_SERIE} a ${ANO_ATUAL}`}
              altura={240}
            />
          )}
          <Link className="panorama__mais" to="/movimentacao">
            Ver por mês e por secretaria
          </Link>
        </section>
      </div>

      <div className="panorama__colunas">
        <section aria-labelledby="titulo-motivos">
          <div className="panorama__cabeca">
            <h2 id="titulo-motivos">Por que saíram em {ANO_ATUAL}</h2>
            {motivos.dados && (
              <p>
                {formatarInteiro(motivos.dados.total)} desligamentos até agora, pelos motivos registrados pela
                Prefeitura.
              </p>
            )}
          </div>
          {motivos.erro && <Erro mensagem={motivos.erro} />}
          {motivos.dados && motivos.dados.total === 0 && <Vazio titulo={`Nenhum desligamento registrado em ${ANO_ATUAL}.`} />}
          {motivos.dados && motivos.dados.total > 0 && (
            <BarrasHorizontais
              medida="desligamentos"
              descricao={`Motivos de desligamento em ${ANO_ATUAL}`}
              barras={motivos.dados.motivos.map((m) => ({ rotulo: m.motivo, valor: m.quantidade }))}
            />
          )}
        </section>

        <section aria-labelledby="titulo-verificar" className="panorama__verificar">
          <div className="panorama__cabeca">
            <h2 id="titulo-verificar">Para verificar</h2>
          </div>
          {alertas.dados && (
            <>
              <p className="panorama__alertas-numero">
                <strong>{formatarInteiro(alertas.dados.total)}</strong>{' '}
                {alertas.dados.total === 1 ? 'vínculo sinalizado' : 'vínculos sinalizados'}
              </p>
              <p>
                Salários acima do teto municipal
                {alertas.dados.teto.valor !== null && <> ({formatarMoeda(alertas.dados.teto.valor)}, o subsídio do prefeito)</>}{' '}
                ou mais de {formatarInteiro(alertas.dados.percentualForaPadrao)}% distantes da mediana de quem tem o mesmo
                cargo, nível e carga horária. São indícios para conferir, não irregularidades confirmadas.
              </p>
              <Link className="botao" to="/alertas">
                Ver os alertas
              </Link>
            </>
          )}
        </section>
      </div>
    </article>
  )
}
