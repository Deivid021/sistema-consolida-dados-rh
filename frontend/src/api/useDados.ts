import { useEffect, useState } from 'react'
import { buscar, type Parametros } from './cliente'

export interface Estado<T> {
  dados: T | null
  erro: string | null
  carregando: boolean
}

/**
 * Busca dados da API e refaz a busca quando o caminho ou os parâmetros mudam. Cancela a
 * requisição anterior para uma resposta antiga nunca sobrescrever a mais nova.
 */
export function useDados<T>(caminho: string | null, parametros?: Parametros): Estado<T> {
  const chave = caminho === null ? null : caminho + JSON.stringify(parametros ?? {})
  const [estado, setEstado] = useState<Estado<T>>({ dados: null, erro: null, carregando: caminho !== null })

  useEffect(() => {
    if (caminho === null) return
    const controle = new AbortController()
    setEstado((anterior) => ({ dados: anterior.dados, erro: null, carregando: true }))
    buscar<T>(caminho, parametros, controle.signal)
      .then((dados) => setEstado({ dados, erro: null, carregando: false }))
      .catch((erro: unknown) => {
        if (erro instanceof DOMException && erro.name === 'AbortError') return
        setEstado({ dados: null, erro: erro instanceof Error ? erro.message : String(erro), carregando: false })
      })
    return () => controle.abort()
    // a chave já resume caminho e parâmetros
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chave])

  return estado
}
