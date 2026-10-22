// Varredura de acessibilidade com axe-core nas 6 telas, nos dois temas (RNF04, SC-003).
// Abre o Google Chrome instalado na máquina (puppeteer-core) contra o front rodando.
// Tema claro: regras WCAG 2.1 A e AA. Tema escuro: as mesmas mais o contraste AAA (7:1).
//
// Uso: npm run dev (em outro terminal) e npm run verificar:acessibilidade
//   ENDERECO=http://localhost:8080   para conferir o build servido pelo Spring Boot
//   CHROME=/caminho/do/chrome        se o Chrome não estiver em /usr/bin/google-chrome
//   CAPTURAS=/pasta                  para salvar uma captura de cada tela (computador e 360 px)
// Sai com código 1 se houver alguma violação.
import { mkdirSync, readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import puppeteer from 'puppeteer-core'

const ENDERECO = process.env.ENDERECO ?? 'http://localhost:5173'
const CHROME = process.env.CHROME ?? '/usr/bin/google-chrome'
const CAPTURAS = process.env.CAPTURAS
const axe = readFileSync(createRequire(import.meta.url).resolve('axe-core/axe.min.js'), 'utf8')

const TELAS = [
  ['panorama', '/'],
  ['servidores', '/servidores?situacao=TRABALHANDO&situacao=AFASTADO'],
  ['salarios', '/salarios'],
  ['movimentacao', '/movimentacao?granularidade=MES'],
  ['alertas', '/alertas'],
  ['nao-encontrada', '/pagina-que-nao-existe'],
]
const REGRAS = {
  claro: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
  escuro: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag2aaa'],
}
const TAMANHOS = { computador: { width: 1280, height: 900 }, celular: { width: 360, height: 780 } }

if (CAPTURAS) mkdirSync(CAPTURAS, { recursive: true })
const navegador = await puppeteer.launch({ executablePath: CHROME, headless: true, args: ['--no-sandbox'] })
let violacoes = 0

try {
  for (const tema of ['claro', 'escuro']) {
    console.log(`\nTema ${tema}`)
    for (const [nome, caminho] of TELAS) {
      const pagina = await navegador.newPage()
      await pagina.evaluateOnNewDocument((t) => localStorage.setItem('tema', t), tema)
      await pagina.setViewport(TAMANHOS.computador)
      await pagina.goto(ENDERECO + caminho, { waitUntil: 'networkidle0' })
      // Espera os dados e a animação de entrada da régua (700 ms) terminarem
      await pagina.waitForSelector('main h1')
      await new Promise((r) => setTimeout(r, 1200))

      const temaAplicado = await pagina.evaluate(() => document.documentElement.dataset.tema)
      if (temaAplicado !== tema) {
        violacoes++
        console.log(`  FALHA ${nome}: tema aplicado "${temaAplicado}", esperado "${tema}"`)
      }

      await pagina.evaluate(axe)
      const resultado = await pagina.evaluate(
        (tags) => window.axe.run(document, { runOnly: { type: 'tag', values: tags }, resultTypes: ['violations'] }),
        REGRAS[tema],
      )
      const encontradas = resultado.violations
      violacoes += encontradas.length
      console.log(`  ${encontradas.length ? 'FALHA' : 'ok   '} ${nome}${encontradas.length ? '' : ': nenhuma violação'}`)
      for (const v of encontradas) {
        console.log(`        ${v.id} (${v.impact}): ${v.help}`)
        for (const no of v.nodes.slice(0, 5)) console.log(`          ${no.target.join(' ')}  ${no.failureSummary?.split('\n')[1]?.trim() ?? ''}`)
        if (v.nodes.length > 5) console.log(`          … e mais ${v.nodes.length - 5}`)
      }

      if (CAPTURAS) {
        await pagina.screenshot({ path: `${CAPTURAS}/${tema}-${nome}-computador.png`, fullPage: true })
        await pagina.setViewport(TAMANHOS.celular)
        await new Promise((r) => setTimeout(r, 400))
        const largura = await pagina.evaluate(() => document.documentElement.scrollWidth)
        if (largura > TAMANHOS.celular.width) {
          violacoes++
          console.log(`  FALHA ${nome}: rolagem horizontal em 360 px (${largura} px de largura)`)
        }
        await pagina.screenshot({ path: `${CAPTURAS}/${tema}-${nome}-celular.png`, fullPage: true })
      }
      await pagina.close()
    }
  }
} finally {
  await navegador.close()
}

console.log(violacoes ? `\n${violacoes} problema(s) encontrado(s).` : '\nNenhuma violação nas 6 telas, nos dois temas.')
process.exit(violacoes ? 1 : 0)
