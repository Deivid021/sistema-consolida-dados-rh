import { useState } from 'react'
import { formatarInteiro, formatarPercentual } from '../../util/formato'
import { rotuloSobre, usePaleta } from './cores'
import { TabelaDoGrafico } from './TabelaDoGrafico'
import './Regua.css'

export interface ParteRegua {
  chave: string
  rotulo: string
  valor: number
}

interface Props {
  partes: ParteRegua[]
  /** Descrição do todo, para leitores de tela e para a tabela. */
  descricao: string
  /** Até quantas partes ganham cor própria; o resto vai para "Outros". */
  maximoCores?: number
}

/**
 * A "régua do quadro": uma única barra proporcional com o todo dividido em partes. É o
 * elemento de destaque do Panorama; o resto da página fica quieto ao redor dela.
 */
export function Regua({ partes, descricao, maximoCores = 6 }: Props) {
  const [ativa, setAtiva] = useState<string | null>(null)
  const paleta = usePaleta()
  const total = partes.reduce((soma, p) => soma + p.valor, 0)
  if (total === 0) return null

  const ordenadas = [...partes].sort((a, b) => b.valor - a.valor)
  const principais = ordenadas.slice(0, maximoCores).map((p, i) => ({ ...p, cor: paleta.series[i] }))
  const resto = ordenadas.slice(maximoCores)
  const segmentos = resto.length
    ? [...principais, { chave: '__outros', rotulo: `Outros (${resto.length})`, valor: resto.reduce((s, p) => s + p.valor, 0), cor: paleta.outros }]
    : principais

  const destaque = segmentos.find((s) => s.chave === ativa)

  return (
    <figure className="regua">
      <div className="regua__barra" role="group" aria-label={descricao} onMouseLeave={() => setAtiva(null)}>
        {segmentos.map((s, i) => {
          const fracao = s.valor / total
          const rotulo = rotuloSobre(s.cor, paleta)
          return (
            <div
              key={s.chave}
              role="img"
              className="regua__segmento"
              style={{ flexGrow: s.valor, background: s.cor, animationDelay: `${i * 60}ms` }}
              onMouseEnter={() => setAtiva(s.chave)}
              onFocus={() => setAtiva(s.chave)}
              onBlur={() => setAtiva(null)}
              tabIndex={0}
              aria-label={`${s.rotulo}: ${formatarInteiro(s.valor)} (${formatarPercentual(fracao)})`}
              data-ativo={ativa === s.chave || undefined}
            >
              {fracao >= 0.09 && (
                <span
                  className={rotulo.comFundo ? 'regua__rotulo regua__rotulo--com-fundo' : 'regua__rotulo'}
                  style={rotulo.comFundo ? undefined : { color: rotulo.cor }}
                  aria-hidden="true"
                >
                  {formatarPercentual(fracao)}
                </span>
              )}
            </div>
          )
        })}
      </div>

      <figcaption className="regua__legenda" aria-live="polite">
        {destaque ? (
          <span className="regua__destaque">
            {destaque.rotulo}: <strong>{formatarInteiro(destaque.valor)}</strong>, {formatarPercentual(destaque.valor / total)} do
            total
          </span>
        ) : (
          <ul className="legenda">
            {segmentos.map((s) => (
              <li key={s.chave} className="legenda__item">
                <span className="legenda__marca" style={{ background: s.cor }} aria-hidden="true" />
                {s.rotulo} <span className="numero">{formatarInteiro(s.valor)}</span>
              </li>
            ))}
          </ul>
        )}
      </figcaption>

      <TabelaDoGrafico
        resumo={descricao}
        colunas={['Parte', 'Vínculos', '% do total']}
        numericas={[1, 2]}
        linhas={ordenadas.map((p) => [p.rotulo, formatarInteiro(p.valor), formatarPercentual(p.valor / total)])}
      />
    </figure>
  )
}
