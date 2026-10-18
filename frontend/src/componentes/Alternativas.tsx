import { useId } from 'react'
import './Filtros.css'

interface Opcao<T extends string> {
  valor: T
  rotulo: string
}

/**
 * Grupo de alternativas como botões encostados. Com {@code multiplo}, cada opção liga e
 * desliga (situação); sem ele, é uma escolha só (regra, granularidade).
 */
export function Alternativas<T extends string>({
  legenda,
  opcoes,
  selecionados,
  aoMudar,
  multiplo = false,
}: {
  legenda: string
  opcoes: Opcao<T>[]
  selecionados: T[]
  aoMudar: (valores: T[]) => void
  multiplo?: boolean
}) {
  const nome = useId()
  return (
    <fieldset className="alternativas">
      <legend>{legenda}</legend>
      <div className="alternativas__opcoes">
        {opcoes.map((o) => {
          const marcado = selecionados.includes(o.valor)
          return (
            <label key={o.valor} className="alternativas__opcao">
              <input
                type={multiplo ? 'checkbox' : 'radio'}
                name={nome}
                checked={marcado}
                onChange={() => {
                  if (!multiplo) aoMudar([o.valor])
                  else aoMudar(marcado ? selecionados.filter((s) => s !== o.valor) : [...selecionados, o.valor])
                }}
              />
              <span>{o.rotulo}</span>
            </label>
          )
        })}
      </div>
    </fieldset>
  )
}
