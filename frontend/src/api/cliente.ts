// Acesso à API de consulta. Em desenvolvimento o Vite repassa /api para o Spring Boot.

const BASE = '/api/v1'

/** Parâmetros de consulta; listas viram o parâmetro repetido (regime=A&regime=B), como a API espera. */
export type Parametros = Record<string, string | number | string[] | undefined | null>

export class ErroApi extends Error {
  readonly status: number

  constructor(status: number, mensagem: string) {
    super(mensagem)
    this.status = status
  }
}

export function montarQuery(parametros: Parametros = {}): string {
  const busca = new URLSearchParams()
  for (const [chave, valor] of Object.entries(parametros)) {
    if (valor === undefined || valor === null || valor === '') continue
    if (Array.isArray(valor)) {
      valor.filter((v) => v !== '').forEach((v) => busca.append(chave, v))
    } else {
      busca.append(chave, String(valor))
    }
  }
  const texto = busca.toString()
  return texto ? `?${texto}` : ''
}

export function urlApi(caminho: string, parametros?: Parametros): string {
  return `${BASE}${caminho}${montarQuery(parametros)}`
}

/** Faz um GET e devolve o JSON; erros da API (problem+json) viram ErroApi com a mensagem em português. */
export async function buscar<T>(caminho: string, parametros?: Parametros, sinal?: AbortSignal): Promise<T> {
  let resposta: Response
  try {
    resposta = await fetch(urlApi(caminho, parametros), { signal: sinal, headers: { Accept: 'application/json' } })
  } catch (erro) {
    if (erro instanceof DOMException && erro.name === 'AbortError') throw erro
    throw new ErroApi(0, 'Não foi possível falar com o servidor. Verifique se o back-end está rodando.')
  }
  if (!resposta.ok) {
    let mensagem = `O servidor respondeu com erro ${resposta.status}.`
    try {
      const corpo = await resposta.json()
      if (typeof corpo?.detail === 'string') mensagem = corpo.detail
    } catch {
      // corpo sem JSON: fica a mensagem genérica
    }
    throw new ErroApi(resposta.status, mensagem)
  }
  return (await resposta.json()) as T
}
