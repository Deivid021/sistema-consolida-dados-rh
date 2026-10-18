import { Link } from 'react-router'
import { Cabecalho } from '../componentes/Cabecalho'

export function NaoEncontrada() {
  return (
    <Cabecalho titulo="Página não encontrada">
      <p>
        O endereço não corresponde a nenhuma tela. <Link to="/">Voltar ao panorama</Link>.
      </p>
    </Cabecalho>
  )
}
