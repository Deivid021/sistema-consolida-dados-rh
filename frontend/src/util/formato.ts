// Formatação no padrão brasileiro. A API envia números e datas ISO; a tela mostra R$ 3.905,17 e 15/10/2026.

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const moedaCompacta = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
  notation: 'compact',
  maximumFractionDigits: 1,
})
const inteiro = new Intl.NumberFormat('pt-BR')
const compacto = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 })
const percentual = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 0 })
const percentual1 = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1, minimumFractionDigits: 1 })

export const NAO_INFORMADO = 'Não informado'

export function formatarMoeda(valor: number | null | undefined): string {
  return valor === null || valor === undefined ? '—' : moeda.format(valor)
}

export function formatarMoedaCompacta(valor: number): string {
  return moedaCompacta.format(valor)
}

export function formatarInteiro(valor: number): string {
  return inteiro.format(valor)
}

export function formatarCompacto(valor: number): string {
  return compacto.format(valor)
}

export function formatarPercentual(fracao: number): string {
  return percentual.format(fracao)
}

/** Diferença em pontos percentuais com sinal: +25,3% ou −21,0%. */
export function formatarDiferencaPercentual(valor: number): string {
  const sinal = valor > 0 ? '+' : valor < 0 ? '−' : ''
  return `${sinal}${percentual1.format(Math.abs(valor))}%`
}

/** "2026-03-15" -> "15/03/2026", sem conversão de fuso. */
export function formatarData(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [ano, mes, dia] = iso.slice(0, 10).split('-')
  return `${dia}/${mes}/${ano}`
}

/** Instante ISO com fuso -> "16/10/2026, 03:16" no horário de Brasília. */
export function formatarDataHora(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(iso))
}

export function textoOuNaoInformado(valor: string | null | undefined): string {
  return valor === null || valor === undefined || valor === '' ? NAO_INFORMADO : valor
}

const SITUACOES: Record<string, string> = {
  TRABALHANDO: 'Trabalhando',
  AFASTADO: 'Afastado',
  DESLIGADO: 'Desligado',
}

export function rotuloSituacao(situacao: string | null): string {
  return situacao ? (SITUACOES[situacao] ?? situacao) : NAO_INFORMADO
}

// A fonte publica os regimes em maiúsculas e sem acento; aqui ganham grafia de leitura.
const REGIMES: Record<string, string> = {
  ESTATUTARIO: 'Estatutário',
  APOSENTADO: 'Aposentado',
  'CONTRATO EMERGENCIAL': 'Contrato emergencial',
  CLT: 'CLT',
  'CARGO COMISSAO': 'Cargo em comissão',
  PENSIONISTA: 'Pensionista',
  INATIVO: 'Inativo',
  'INATIVOS PDV': 'Inativo (PDV)',
  ELETIVO: 'Eletivo',
  ADIDOS: 'Adido',
  ENQUADRADO: 'Enquadrado',
  ESTAGIARIO: 'Estagiário',
}

export function rotuloRegime(regime: string | null): string {
  if (!regime) return NAO_INFORMADO
  return REGIMES[regime] ?? regime.charAt(0) + regime.slice(1).toLowerCase()
}

/** Plural do regime para frases: "estatutários", "aposentados". */
export function rotuloRegimePlural(regime: string): string {
  const rotulo = rotuloRegime(regime).toLowerCase()
  if (rotulo === 'clt') return 'contratados pela CLT'
  if (rotulo.includes(' ')) return `em ${rotulo}`
  return rotulo.endsWith('o') ? `${rotulo}s` : rotulo
}

export function rotuloRegra(regra: string): string {
  return regra === 'TETO' ? 'Acima do teto' : 'Fora do padrão'
}

/** "218" -> "Nível 218"; quando a fonte já escreve "Nível III magistério", mantém como veio. */
export function rotuloNivel(nivel: string | null): string | null {
  if (!nivel) return null
  return /^n[ií]vel\b/i.test(nivel) ? nivel : `Nível ${nivel}`
}
