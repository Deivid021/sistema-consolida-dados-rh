import type { GrupoSalarial, Pagina } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Cabecalho } from '../componentes/Cabecalho'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { FiltrosVinculo } from '../componentes/FiltrosVinculo'
import { BarraTabela, CabecalhoOrdenavel, Exportar, Paginacao } from '../componentes/Tabela'
import { formatarInteiro, formatarMoeda, textoOuNaoInformado } from '../util/formato'
import { LISTAS_VINCULO, useFiltrosUrl } from '../util/useFiltrosUrl'

const ORDENACAO_PADRAO = 'soma,desc'

export function Salarios() {
  const filtros = useFiltrosUrl(LISTAS_VINCULO)
  const ordenacao = filtros.valor('ordenacao') || ORDENACAO_PADRAO
  const pagina = Number(filtros.valor('pagina') || 0)
  const consulta = useDados<Pagina<GrupoSalarial>>('/indicadores/salarios', {
    ...filtros.parametros,
    ordenacao,
    pagina,
    tamanho: 25,
  })
  const dados = consulta.dados
  const soAtivos = filtros.lista('situacao').length === 0
  const ordenar = (nova: string) => filtros.definir('ordenacao', nova)

  return (
    <>
      <Cabecalho titulo="Salários">
        <p>
          Quanto soma e quanto é, em média, o salário base de cada cargo e nível. Salário base é o valor do cargo
          publicado pela Prefeitura, sem gratificações nem descontos.
        </p>
        {soAtivos && (
          <p>
            Sem filtro de situação, a conta considera só vínculos ativos. Salários zerados ou ausentes ficam fora da
            média e aparecem na coluna "Sem salário".
          </p>
        )}
      </Cabecalho>

      <FiltrosVinculo filtros={filtros} comDatas={false} />

      {consulta.erro && <Erro mensagem={consulta.erro} />}
      {!dados && !consulta.erro && <Carregando texto="Somando os salários…" />}

      {dados && (
        <>
          <BarraTabela total={dados.total} rotulo={dados.total === 1 ? 'combinação de cargo e nível' : 'combinações de cargo e nível'}>
            {dados.total > 0 && <Exportar relatorio="salarios" parametros={{ ...filtros.parametros, ordenacao }} linhas={dados.total} />}
          </BarraTabela>

          {dados.total === 0 ? (
            <Vazio titulo="Nenhum cargo com esses filtros.">Remova um dos filtros para ver mais cargos.</Vazio>
          ) : (
            <div className="rolagem-tabela" aria-busy={consulta.carregando}>
              <table className="tabela">
                <caption className="visualmente-oculto">Salário base por cargo e nível</caption>
                <thead>
                  <tr>
                    <CabecalhoOrdenavel campo="cargo" rotulo="Cargo" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="nivel" rotulo="Nível" ordenacao={ordenacao} aoOrdenar={ordenar} />
                    <CabecalhoOrdenavel campo="quantidade" rotulo="Vínculos" ordenacao={ordenacao} aoOrdenar={ordenar} numerico padraoDecrescente />
                    <th scope="col" className="numero">Sem salário</th>
                    <CabecalhoOrdenavel campo="soma" rotulo="Soma" ordenacao={ordenacao} aoOrdenar={ordenar} numerico padraoDecrescente />
                    <CabecalhoOrdenavel campo="media" rotulo="Média" ordenacao={ordenacao} aoOrdenar={ordenar} numerico padraoDecrescente />
                  </tr>
                </thead>
                <tbody>
                  {dados.itens.map((g) => (
                    <tr key={`${g.cargo}|${g.nivel}`}>
                      <td className="tabela__principal">{textoOuNaoInformado(g.cargo)}</td>
                      <td>{textoOuNaoInformado(g.nivel)}</td>
                      <td className="numero">{formatarInteiro(g.quantidade)}</td>
                      <td className="numero">{g.quantidadeSemSalario > 0 ? formatarInteiro(g.quantidadeSemSalario) : '—'}</td>
                      <td className="numero">{formatarMoeda(g.soma)}</td>
                      <td className="numero">{formatarMoeda(g.media)}</td>
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
