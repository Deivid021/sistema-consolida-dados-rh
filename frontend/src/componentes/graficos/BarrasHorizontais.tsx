import { formatarInteiro } from '../../util/formato'
import { TabelaDoGrafico } from './TabelaDoGrafico'
import './Graficos.css'
import './BarrasHorizontais.css'

export interface Barra {
  rotulo: string
  valor: number
}

interface Props {
  barras: Barra[]
  /** Nome do que está sendo contado (ex.: "vínculos ativos"), para a dica e a tabela. */
  medida: string
  descricao: string
}

/**
 * Ranking em uma série só, desenhado como um livro-razão: o nome numa linha inteira e a barra
 * logo abaixo, usando toda a largura, com o valor na ponta. Sem legenda (uma série só).
 */
export function BarrasHorizontais({ barras, medida, descricao }: Props) {
  const maior = Math.max(...barras.map((b) => b.valor), 1)

  return (
    <div className="grafico">
      <ol className="barras" aria-label={descricao}>
        {barras.map((b) => (
          <li key={b.rotulo} className="barras__item" title={`${b.rotulo}: ${formatarInteiro(b.valor)} ${medida}`}>
            <span className="barras__rotulo">{b.rotulo}</span>
            <span className="barras__trilho">
              <span className="barras__barra" style={{ width: `${(b.valor / maior) * 100}%` }} />
              <span className="barras__valor">{formatarInteiro(b.valor)}</span>
            </span>
          </li>
        ))}
      </ol>
      <TabelaDoGrafico
        resumo={descricao}
        colunas={['', medida.charAt(0).toUpperCase() + medida.slice(1)]}
        numericas={[1]}
        linhas={barras.map((b) => [b.rotulo, formatarInteiro(b.valor)])}
      />
    </div>
  )
}
