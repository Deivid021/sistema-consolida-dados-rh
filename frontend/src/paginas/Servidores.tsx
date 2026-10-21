import type { Pagina, Vinculo } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Cabecalho } from '../componentes/Cabecalho'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { FiltrosVinculo } from '../componentes/FiltrosVinculo'
import { BarraTabela, CabecalhoOrdenavel, Exportar, Paginacao } from '../componentes/Tabela'
import { formatarData, formatarMoeda, rotuloNivel, rotuloRegime, rotuloSituacao, textoOuNaoInformado } from '../util/formato'
import { LISTAS_VINCULO, useFiltrosUrl } from '../util/useFiltrosUrl'

const ORDENACAO_PADRAO = 'nome,asc'

export function Servidores() {
  const filtros = useFiltrosUrl(LISTAS_VINCULO)
  const ordenacao = filtros.valor('ordenacao') || ORDENACAO_PADRAO
  const pagina = Number(filtros.valor('pagina') || 0)
  const consulta = useDados<Pagina<Vinculo>>('/vinculos', { ...filtros.parametros, ordenacao, pagina, tamanho: 25 })
  const dados = consulta.dados

  const ordenar = (nova: string) => filtros.definir('ordenacao', nova)

  return (
    <>
      <Cabecalho titulo="Servidores">
        <p>
          Os vínculos publicados pela Prefeitura, inclusive os desligados. Use a situação para ver só quem está na
          ativa. A mesma seleção vale para a exportação.
        </p>
      </Cabecalho>

      <FiltrosVinculo filtros={filtros} />

      {consulta.erro && <Erro mensagem={consulta.erro} />}
      {!dados && !consulta.erro && <Carregando texto="Consultando os servidores…" />}

      {dados && (
        <>
          <BarraTabela total={dados.total} rotulo={dados.total === 1 ? 'vínculo encontrado' : 'vínculos encontrados'}>
            {dados.total > 0 && <Exportar relatorio="vinculos" parametros={{ ...filtros.parametros, ordenacao }} linhas={dados.total} />}
          </BarraTabela>

          {dados.total === 0 ? (
            <Vazio titulo="Nenhum vínculo com esses filtros.">
              Remova um dos filtros ou{' '}
              <button type="button" className="botao-texto" onClick={() => filtros.limpar()}>
                limpe todos
              </button>
              .
            </Vazio>
          ) : (
            <div className="rolagem-tabela" aria-busy={consulta.carregando}>
              <table className="tabela">
                <caption className="visualmente-oculto">Vínculos encontrados, ordenados por {ordenacao.split(',')[0]}</caption>
                <thead>
                  <tr>
                    <CabecalhoOrdenavel campo="nome" rotulo="Servidor" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="situacao" rotulo="Situação" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="regime" rotulo="Regime" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="cargo" rotulo="Cargo e nível" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="secretaria" rotulo="Secretaria" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="salarioBase" rotulo="Salário base" ordenacao={ordenacao} aoOrdenar={ordenar} numerico padraoDecrescente />
                    <CabecalhoOrdenavel campo="dataAdmissao" rotulo="Admissão" ordenacao={ordenacao} aoOrdenar={ordenar} numerico padraoDecrescente />
                  </tr>
                </thead>
                <tbody>
                  {dados.itens.map((v) => (
                    <tr key={v.matricula}>
                      <td>
                        <span className="tabela__principal">{textoOuNaoInformado(v.nome)}</span>
                        <span className="tabela__secundario">Matrícula {v.matricula}</span>
                      </td>
                      <td>
                        <span>{rotuloSituacao(v.situacao)}</span>
                        {v.situacao === 'DESLIGADO' && (
                          <span className="tabela__secundario">
                            em {formatarData(v.dataDesligamento)}
                            {v.motivoDesligamento && <>, {v.motivoDesligamento}</>}
                          </span>
                        )}
                        {!v.ativoNaFonte && <span className="tabela__secundario">Não veio na última carga</span>}
                      </td>
                      <td>{rotuloRegime(v.regime)}</td>
                      <td>
                        <span>{textoOuNaoInformado(v.cargo)}</span>
                        {v.nivel && <span className="tabela__secundario">{rotuloNivel(v.nivel)}</span>}
                      </td>
                      <td>
                        <span>{textoOuNaoInformado(v.secretaria)}</span>
                        {v.localTrabalho && <span className="tabela__secundario">{v.localTrabalho}</span>}
                      </td>
                      <td className="numero">
                        {formatarMoeda(v.salarioBase)}
                        {v.horasMensais !== null && <span className="tabela__secundario">{v.horasMensais.toLocaleString('pt-BR')} h/mês</span>}
                      </td>
                      <td className="numero">{formatarData(v.dataAdmissao)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <Paginacao pagina={dados.pagina} tamanho={dados.tamanho} total={dados.total} aoMudar={(p) => filtros.definir('pagina', String(p))} />
        </>
      )}
    </>
  )
}
