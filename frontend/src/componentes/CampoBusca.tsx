import { useEffect, useId, useState } from 'react'
import './Filtros.css'

/** Busca por texto, aplicada 300 ms depois de parar de digitar. */
export function CampoBusca({ valor, aoMudar, rotulo = 'Buscar por nome, cargo ou matrícula' }: {
  valor: string
  aoMudar: (texto: string) => void
  rotulo?: string
}) {
  const id = useId()
  const [texto, setTexto] = useState(valor)
  const [valorAnterior, setValorAnterior] = useState(valor)

  // A URL mudou por fora (ex.: "Limpar filtros"): o campo acompanha
  if (valor !== valorAnterior) {
    setValorAnterior(valor)
    setTexto(valor)
  }

  useEffect(() => {
    if (texto === valor) return
    const espera = setTimeout(() => {
      if (texto.trim().length === 0 || texto.trim().length >= 2) aoMudar(texto.trim())
    }, 300)
    return () => clearTimeout(espera)
  }, [texto, valor, aoMudar])

  return (
    <div className="campo campo--largo">
      <label className="campo__rotulo" htmlFor={id}>
        {rotulo}
      </label>
      <input
        id={id}
        type="search"
        className="campo__entrada"
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        placeholder="Ex.: professor, Silva, 1315-01"
      />
    </div>
  )
}
