import { createBrowserRouter, RouterProvider } from 'react-router'
import { Layout } from './componentes/Layout'
import { Alertas } from './paginas/Alertas'
import { Movimentacao } from './paginas/Movimentacao'
import { NaoEncontrada } from './paginas/NaoEncontrada'
import { Panorama } from './paginas/Panorama'
import { Salarios } from './paginas/Salarios'
import { Servidores } from './paginas/Servidores'

const rotas = createBrowserRouter([
  {
    element: <Layout />,
    children: [
      { index: true, element: <Panorama /> },
      { path: 'servidores', element: <Servidores /> },
      { path: 'salarios', element: <Salarios /> },
      { path: 'movimentacao', element: <Movimentacao /> },
      { path: 'alertas', element: <Alertas /> },
      { path: '*', element: <NaoEncontrada /> },
    ],
  },
])

export function App() {
  return <RouterProvider router={rotas} />
}
