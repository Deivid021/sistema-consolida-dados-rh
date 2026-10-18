# Front-end — Quadro de pessoal de Gravataí

React 19, TypeScript, Vite, React Router e Recharts. Consome a API do back-end
(`/api/v1`, contrato em `specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml`).

## Rodar

Com o back-end rodando em `localhost:8080` (ver o README da raiz):

```bash
npm install
npm run dev      # http://localhost:5173 (o Vite repassa /api para o 8080)
npm run build    # gera dist/
npm run lint
```

## Telas

| Rota | Conteúdo | Requisito |
|------|----------|-----------|
| `/` | Panorama: manchete, régua do quadro por regime, secretarias, entradas e saídas, motivos, alertas | RF03, RF04 |
| `/servidores` | Consulta com filtros, tabela ordenável e exportação | RF02, RF05 |
| `/salarios` | Soma e média do salário base por cargo e nível | RF03, RF05 |
| `/movimentacao` | Admissões e desligamentos por mês ou ano, e motivos | RF04 |
| `/alertas` | Salários acima do teto e fora do padrão | RF06, RF05 |

Os filtros ficam na URL, então um link já filtrado pode ser compartilhado.

## Direção de design

Feita com a skill `frontend-design` (plugin oficial `anthropics/claude-code`) e a skill de
visualização de dados.

- **Ideia:** a papelada do serviço público, com ofício, contracheque, carimbo de protocolo e
  caneta azul. O elemento de destaque é a "régua do quadro" no Panorama; o resto fica quieto.
- **Cores** (`src/estilos/tokens.css`):
  - papel `#F5F7F6`
  - tinta `#1D2A36`
  - caneta azul `#2447A6`
  - carimbo `#6B3FA0`
  - alerta `#B42318`, só para alertas e erros
- **Paleta dos gráficos:** validada para daltonismo com o script da skill de dataviz.
- **Tipografia:** só a Archivo, com contraste pela largura. Títulos em largura expandida, texto
  normal, algarismos tabulares nas tabelas.
- **Acessibilidade:**
  - todo gráfico tem uma visão em tabela;
  - foco visível pelo teclado;
  - respeita "reduzir movimento";
  - layout responsivo até celular.
