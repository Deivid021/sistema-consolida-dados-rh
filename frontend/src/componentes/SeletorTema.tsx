import { escolherTema, useTema, type Tema } from '../util/tema'
import { Alternativas } from './Alternativas'

const OPCOES: { valor: Tema; rotulo: React.ReactNode }[] = [
  { valor: 'claro', rotulo: 'Claro' },
  {
    valor: 'escuro',
    rotulo: (
      <>
        Escuro<span className="visualmente-oculto"> de alto contraste</span>
      </>
    ),
  },
]

/**
 * Opção de aparência (RNF04). Botões de rádio: o teclado troca com as setas e o leitor de
 * tela anuncia "Aparência" e o tema marcado (FR-004).
 */
export function SeletorTema() {
  const tema = useTema()
  return <Alternativas legenda="Aparência" opcoes={OPCOES} selecionados={[tema]} aoMudar={([t]) => escolherTema(t)} />
}
