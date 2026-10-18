import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import type { Parametros } from '../api/cliente'

/** Filtros de vínculo que aceitam vários valores (parâmetro repetido). */
export const LISTAS_VINCULO = ['situacao', 'regime', 'classificacao', 'secretaria', 'localTrabalho', 'cargo'] as const

/**
 * Filtros guardados na URL: a tela pode ser recarregada ou compartilhada com os mesmos
 * filtros. Listas usam o parâmetro repetido, igual à API.
 */
export function useFiltrosUrl(listas: readonly string[]) {
  const [busca, setBusca] = useSearchParams()

  const parametros = useMemo<Parametros>(() => {
    const resultado: Parametros = {}
    for (const chave of new Set(busca.keys())) {
      resultado[chave] = listas.includes(chave) ? busca.getAll(chave) : busca.get(chave) ?? undefined
    }
    return resultado
  }, [busca, listas])

  const lista = useCallback((chave: string) => busca.getAll(chave), [busca])
  const valor = useCallback((chave: string) => busca.get(chave) ?? '', [busca])

  /** Altera um filtro e volta para a primeira página. */
  const definir = useCallback(
    (chave: string, novo: string | string[] | null) => {
      setBusca(
        (anterior) => {
          const proximo = new URLSearchParams(anterior)
          proximo.delete(chave)
          if (Array.isArray(novo)) novo.forEach((v) => proximo.append(chave, v))
          else if (novo) proximo.set(chave, novo)
          if (chave !== 'pagina') proximo.delete('pagina')
          return proximo
        },
        { replace: true },
      )
    },
    [setBusca],
  )

  const limpar = useCallback((manter: string[] = []) => {
    setBusca((anterior) => {
      const proximo = new URLSearchParams()
      manter.forEach((chave) => anterior.getAll(chave).forEach((v) => proximo.append(chave, v)))
      return proximo
    })
  }, [setBusca])

  const ativos = [...busca.keys()].filter((c) => !['pagina', 'ordenacao', 'tamanho'].includes(c)).length

  return { parametros, lista, valor, definir, limpar, ativos }
}
