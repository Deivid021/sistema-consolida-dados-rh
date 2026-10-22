import { NavLink, Outlet } from 'react-router'
import type { PaginaAlertas, Status } from '../api/tipos'
import { useDados } from '../api/useDados'
import { Carimbo } from './Carimbo'
import { SeletorTema } from './SeletorTema'
import './Layout.css'

const SECOES = [
  { caminho: '/', rotulo: 'Panorama', fim: true },
  // Pelo índice, a consulta abre com quem está na ativa; o filtro fica visível e sai com um clique
  { caminho: '/servidores?situacao=TRABALHANDO&situacao=AFASTADO', rotulo: 'Servidores' },
  { caminho: '/salarios', rotulo: 'Salários' },
  { caminho: '/movimentacao', rotulo: 'Entradas e saídas' },
  { caminho: '/alertas', rotulo: 'Alertas' },
]

/** Estrutura de documento: índice fixo à esquerda, como o sumário de um processo, e a leitura à direita. */
export function Layout() {
  const status = useDados<Status>('/status')
  const alertas = useDados<PaginaAlertas>('/alertas', { tamanho: 1 })

  return (
    <div className="layout">
      <a className="pular" href="#conteudo">Pular para o conteúdo</a>
      <aside className="indice">
        <div className="indice__marca">
          <span className="indice__orgao">Prefeitura de Gravataí</span>
          <span className="indice__titulo">Quadro de pessoal</span>
        </div>
        <nav aria-label="Seções">
          <ul className="indice__lista">
            {SECOES.map((s) => (
              <li key={s.caminho}>
                <NavLink to={s.caminho} end={s.fim} className="indice__link">
                  {s.rotulo}
                  {s.caminho === '/alertas' && alertas.dados && alertas.dados.total > 0 && (
                    <span className="indice__contador" aria-label={`${alertas.dados.total} alertas`}>
                      {alertas.dados.total}
                    </span>
                  )}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <Carimbo status={status.dados} carregando={status.carregando} />
        {/* Logo abaixo do carimbo, no pé do índice: o mesmo lugar em todas as telas (RNF04) */}
        <SeletorTema />
      </aside>
      <main id="conteudo" className="conteudo">
        <Outlet />
      </main>
    </div>
  )
}
