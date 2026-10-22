import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { TooltipContentProps } from 'recharts'
import { formatarCompacto, formatarInteiro } from '../../util/formato'
import { usePaleta } from './cores'
import { TabelaDoGrafico } from './TabelaDoGrafico'
import './Graficos.css'

export interface SerieTempo {
  chave: string
  rotulo: string
}

interface Props<T extends { periodo: string }> {
  dados: T[]
  series: SerieTempo[]
  descricao: string
  rotuloPeriodo?: (periodo: string) => string
  altura?: number
}

/**
 * Duas ou mais séries ao longo do tempo, no mesmo eixo (nunca dois eixos). Legenda acima,
 * linha de 2px, dica com todas as séries no ponto e tabela com os números.
 */
export function LinhasTempo<T extends { periodo: string }>({
  dados,
  series,
  descricao,
  rotuloPeriodo = (p) => p,
  altura = 280,
}: Props<T>) {
  const paleta = usePaleta()
  const cores = series.map((_, i) => paleta.series[i])
  const maximo = Math.max(1, ...dados.flatMap((d) => series.map((s) => Number((d as Record<string, unknown>)[s.chave] ?? 0))))
  const marcas = marcasRedondas(maximo)

  return (
    <div className="grafico">
      <ul className="legenda" aria-hidden="true">
        {series.map((s, i) => (
          <li key={s.chave} className="legenda__item">
            <span className="legenda__marca legenda__marca--linha" style={{ background: cores[i] }} />
            {s.rotulo}
          </li>
        ))}
      </ul>
      <div style={{ width: '100%', height: altura }} role="img" aria-label={descricao}>
        <ResponsiveContainer>
          <LineChart data={dados} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} stroke={paleta.grade} />
            <XAxis
              dataKey="periodo"
              tickFormatter={rotuloPeriodo}
              tickLine={false}
              axisLine={{ stroke: paleta.eixo }}
              tick={{ fill: paleta.textoEixo, fontSize: 12 }}
              minTickGap={16}
            />
            <YAxis
              domain={[0, marcas[marcas.length - 1]]}
              ticks={marcas}
              tickFormatter={(v: number) => formatarCompacto(v)}
              tickLine={false}
              axisLine={false}
              width={44}
              tick={{ fill: paleta.textoEixo, fontSize: 12 }}
              allowDecimals={false}
            />
            <Tooltip
              cursor={{ stroke: paleta.textoEixo, strokeWidth: 1 }}
              content={(props) => <Dica {...props} series={series} cores={cores} rotuloPeriodo={rotuloPeriodo} />}
            />
            {series.map((s, i) => (
              <Line
                key={s.chave}
                type="linear"
                dataKey={s.chave}
                name={s.rotulo}
                stroke={cores[i]}
                strokeWidth={2}
                strokeLinecap="round"
                strokeLinejoin="round"
                dot={false}
                activeDot={{ r: 5, stroke: paleta.fundo, strokeWidth: 2 }}
                isAnimationActive={false}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
      <TabelaDoGrafico
        resumo={descricao}
        colunas={['Período', ...series.map((s) => s.rotulo)]}
        numericas={series.map((_, i) => i + 1)}
        linhas={dados.map((d) => [
          rotuloPeriodo(d.periodo),
          ...series.map((s) => formatarInteiro(Number((d as Record<string, unknown>)[s.chave] ?? 0))),
        ])}
      />
    </div>
  )
}

/** Até 5 marcas em valores redondos (1, 2 ou 5 vezes uma potência de 10), começando em zero. */
function marcasRedondas(maximo: number): number[] {
  const bruto = maximo / 4
  const potencia = 10 ** Math.floor(Math.log10(bruto))
  const passo = [1, 2, 2.5, 5, 10].map((m) => m * potencia).find((p) => p >= bruto) ?? 10 * potencia
  const quantidade = Math.ceil(maximo / passo)
  return Array.from({ length: quantidade + 1 }, (_, i) => i * passo)
}

function Dica({
  active,
  payload,
  label,
  series,
  cores,
  rotuloPeriodo,
}: TooltipContentProps & { series: SerieTempo[]; cores: string[]; rotuloPeriodo: (p: string) => string }) {
  if (!active || !payload?.length) return null
  const ponto = payload[0].payload as Record<string, number>
  return (
    <div className="dica">
      <div className="dica__titulo">{rotuloPeriodo(String(label))}</div>
      {series.map((s, i) => (
        <div key={s.chave} className="dica__linha">
          <span className="dica__chave" style={{ background: cores[i] }} />
          <span className="dica__valor">{formatarInteiro(ponto[s.chave] ?? 0)}</span> {s.rotulo.toLowerCase()}
        </div>
      ))}
    </div>
  )
}
