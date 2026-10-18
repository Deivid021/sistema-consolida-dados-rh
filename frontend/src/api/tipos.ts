// Tipos espelhando specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml

export type Situacao = 'TRABALHANDO' | 'AFASTADO' | 'DESLIGADO'
export type RegraAlerta = 'TETO' | 'FORA_PADRAO'
export type OrigemTeto = 'FONTE' | 'MANUAL' | 'SUSPENSA'
export type Granularidade = 'MES' | 'ANO'

export interface Teto {
  valor: number | null
  origem: OrigemTeto
  fundamento: string | null
}

export interface Status {
  ultimaAtualizacao: string | null
  totalVinculos: number
  totalAtivos: number
  fonte: string
  teto: Teto
}

export interface Vinculo {
  matricula: string
  nome: string | null
  situacao: Situacao
  regime: string | null
  classificacao: string | null
  secretaria: string | null
  localTrabalho: string | null
  cargo: string | null
  funcao: string | null
  nivel: string | null
  salarioBase: number | null
  horasMensais: number | null
  dataAdmissao: string | null
  dataDesligamento: string | null
  motivoDesligamento: string | null
  ativoNaFonte: boolean
}

export interface Pagina<T> {
  itens: T[]
  total: number
  pagina: number
  tamanho: number
  ultimaAtualizacao: string | null
}

export interface Opcao {
  valor: string | null
  quantidade: number
}

export interface OpcoesFiltro {
  situacao: Opcao[]
  regime: Opcao[]
  classificacao: Opcao[]
  secretaria: Opcao[]
  localTrabalho: Opcao[]
  cargo: Opcao[]
}

export interface Contagem {
  valor: string | null
  quantidade: number
}

export interface Quadro {
  total: number
  porSituacao: Contagem[]
  porRegime: Contagem[]
  porSecretaria: Contagem[]
  ultimaAtualizacao: string | null
}

export interface GrupoSalarial {
  cargo: string | null
  nivel: string | null
  quantidade: number
  quantidadeSemSalario: number
  soma: number
  media: number | null
}

export interface Periodo {
  periodo: string
  admissoes: number
  desligamentos: number
}

export interface Movimentacao {
  granularidade: Granularidade
  periodos: Periodo[]
  ultimaAtualizacao: string | null
}

export interface Motivo {
  motivo: string
  quantidade: number
}

export interface MotivosRescisao {
  total: number
  motivos: Motivo[]
  ultimaAtualizacao: string | null
}

export interface Alerta {
  regra: RegraAlerta
  vinculo: Vinculo
  valorVinculo: number
  valorReferencia: number
  diferenca: number
  diferencaPercentual: number
  grupoTamanho: number | null
  descricaoRegra: string
}

export interface PaginaAlertas extends Pagina<Alerta> {
  teto: Teto
  percentualForaPadrao: number
}
