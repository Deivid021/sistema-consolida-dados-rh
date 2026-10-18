import { useId, useState } from 'react'
import type { Opcao } from '../api/tipos'
import { formatarInteiro, NAO_INFORMADO } from '../util/formato'
import './Filtros.css'

const VALOR_NAO_INFORMADO = 'NAO_INFORMADO'

interface Props {
  rotulo: string
  opcoes: Opcao[]
  selecionados: string[]
  aoMudar: (valores: string[]) => void
  /** Como exibir cada valor (ex.: regime "ESTATUTARIO" -> "Estatutário"). */
  exibir?: (valor: string) => string
}

/**
 * Escolha de vários valores de uma lista longa (secretarias, cargos): digita, escolhe na
 * lista e o valor vira uma etiqueta removível. Vários valores do mesmo filtro somam (OU).
 */
export function SeletorMultiplo({ rotulo, opcoes, selecionados, aoMudar, exibir = (v) => v }: Props) {
  const id = useId()
  const [texto, setTexto] = useState('')

  const rotular = (valor: string) => (valor === VALOR_NAO_INFORMADO ? NAO_INFORMADO : exibir(valor))
  const disponiveis = opcoes
    .map((o) => ({ valor: o.valor ?? VALOR_NAO_INFORMADO, quantidade: o.quantidade }))
    .filter((o) => !selecionados.includes(o.valor))

  function escolher(entrada: string) {
    const achada = disponiveis.find((o) => rotular(o.valor).toLowerCase() === entrada.trim().toLowerCase())
    if (achada) {
      aoMudar([...selecionados, achada.valor])
      setTexto('')
    }
  }

  return (
    <div className="campo">
      <label className="campo__rotulo" htmlFor={id}>
        {rotulo}
      </label>
      <input
        id={id}
        className="campo__entrada"
        list={`${id}-lista`}
        value={texto}
        placeholder={selecionados.length ? 'Somar outro…' : 'Todos'}
        onChange={(e) => {
          setTexto(e.target.value)
          // ao escolher na lista, o navegador preenche o valor exato: aplica na hora
          if (disponiveis.some((o) => rotular(o.valor) === e.target.value)) escolher(e.target.value)
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault()
            escolher(texto)
          }
        }}
        autoComplete="off"
      />
      {selecionados.length > 0 && (
        <ul className="etiquetas" aria-label={`${rotulo} escolhidos`}>
          {selecionados.map((v) => (
            <li key={v}>
              <button
                type="button"
                className="etiqueta"
                onClick={() => aoMudar(selecionados.filter((s) => s !== v))}
                aria-label={`Remover ${rotular(v)}`}
              >
                {rotular(v)} <span aria-hidden="true">×</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <datalist id={`${id}-lista`}>
        {disponiveis.map((o) => (
          <option key={o.valor} value={rotular(o.valor)}>
            {formatarInteiro(o.quantidade)} vínculos
          </option>
        ))}
      </datalist>
    </div>
  )
}
