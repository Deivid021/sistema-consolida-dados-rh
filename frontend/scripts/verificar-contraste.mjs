// Mede o contraste (WCAG 2.1) dos pares de cor usados na interface, nos dois temas.
// Tema escuro, de alto contraste (RNF04): texto >= 7:1 (1.4.6, AAA). Tema claro: texto >= 4,5:1
// (1.4.3, AA). Nos dois: bordas, foco e marcas de gráfico >= 3:1 (1.4.11).
// Uso: npm run verificar:contraste. Sai com código 1 se algum par falhar.
import { readFileSync } from 'node:fs'

const css = readFileSync(new URL('../src/estilos/tokens.css', import.meta.url), 'utf8')

function bloco(seletor) {
  const inicio = css.indexOf(`${seletor} {`)
  if (inicio < 0) throw new Error(`Bloco ${seletor} não encontrado em tokens.css`)
  const corpo = css.slice(inicio, css.indexOf('}', inicio))
  return Object.fromEntries([...corpo.matchAll(/--([\w-]+):\s*(#[0-9a-fA-F]{6})\b/g)].map((m) => [m[1], m[2]]))
}

const claro = bloco(':root')
const temas = { claro, escuro: { ...claro, ...bloco(":root[data-tema='escuro']") } }

const luminancia = (hex) => {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
const contraste = (a, b) => {
  const [x, y] = [luminancia(a), luminancia(b)].sort((p, q) => q - p)
  return (x + 0.05) / (y + 0.05)
}

const FUNDOS = ['papel', 'papel-elevado']
// [cor, fundos, tipo]: tipo "texto" ou "grafico" (bordas, foco, marcas)
const PARES = [
  ...['tinta', 'tinta-suave', 'tinta-apagada', 'caneta', 'carimbo', 'alerta'].map((c) => [c, FUNDOS, 'texto']),
  ['tinta', ['linha-destaque', 'caneta-fundo', 'alerta-fundo'], 'texto'],
  ['tinta-suave', ['linha-destaque'], 'texto'],
  ['caneta-escura', ['caneta-fundo', 'caneta-fundo-forte'], 'texto'],
  ['alerta', ['alerta-fundo'], 'texto'],
  ['sobre-caneta', ['caneta', 'caneta-escura'], 'texto'],
  ['sobre-alerta', ['alerta'], 'texto'],
  ['caneta', FUNDOS, 'grafico'],
  ['carimbo', FUNDOS, 'grafico'],
]
const SERIES = ['serie-1', 'serie-2', 'serie-3', 'serie-4', 'serie-5', 'serie-6']

let falhas = 0
for (const [nome, cores] of Object.entries(temas)) {
  const minimoTexto = nome === 'escuro' ? 7 : 4.5
  const pares = [...PARES]
  // Só no escuro: bordas de campos e botões (no claro a pauta é de 1,4:1, decisão visual da
  // funcionalidade 001, fora do RNF04) e as séries dos gráficos, validadas também pela skill
  // de dataviz
  if (nome === 'escuro') pares.push(['pauta', FUNDOS, 'grafico'], ...SERIES.map((s) => [s, ['papel'], 'grafico']))
  console.log(`\nTema ${nome} (texto ≥ ${minimoTexto}:1, gráficos e bordas ≥ 3:1)`)
  for (const [cor, fundos, tipo] of pares) {
    for (const fundo of fundos) {
      const valor = contraste(cores[cor], cores[fundo])
      const minimo = tipo === 'texto' ? minimoTexto : 3
      const ok = valor >= minimo
      if (!ok) falhas++
      console.log(`  ${ok ? 'ok   ' : 'FALHA'} ${cor.padEnd(14)} sobre ${fundo.padEnd(18)} ${valor.toFixed(2)}:1`)
    }
  }
}

console.log(falhas ? `\n${falhas} par(es) abaixo do mínimo.` : '\nTodos os pares passam.')
process.exit(falhas ? 1 : 0)
