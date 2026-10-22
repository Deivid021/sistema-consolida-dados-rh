# Front-end — Quadro de pessoal de Gravataí

React 19, TypeScript, Vite, React Router e Recharts. Consome a API do back-end
(`/api/v1`, contrato em `specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml`).

## Rodar

Com o back-end rodando em `localhost:8080` (ver o README da raiz):

```bash
npm install
npm run dev      # http://localhost:5173 (o Vite repassa /api para o 8080)
npm run build    # gera dist/ (em produção o Dockerfile faz isso e põe o resultado dentro do jar)
npm run lint
npm run verificar:contraste       # contraste dos pares de cor dos dois temas
npm run verificar:acessibilidade  # axe-core nas 6 telas, nos dois temas (precisa do npm run dev)
```

A varredura de acessibilidade usa o Google Chrome instalado (`/usr/bin/google-chrome`, ou a
variável `CHROME`). Com `CAPTURAS=/uma/pasta` ela salva capturas de cada tela no computador e
em 360 px.

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
- **Paleta dos gráficos:** validada para daltonismo com o script da skill de dataviz, nos
  dois temas.
- **Modo Escuro de Alto Contraste** (RNF04, Solicitação de Mudança 3c): o "papel-carbono",
  fundo `#0F1724` e tinta `#F3F6F9`. Escolhido em **Aparência**, no índice. No primeiro acesso
  segue o sistema operacional, e a escolha fica guardada no navegador.
  - Os mesmos tokens de `tokens.css` ganham outros valores sob `:root[data-tema='escuro']`;
    nenhum componente tem cor fixa.
  - Texto com contraste ≥ 7:1 (AAA); bordas, foco e marcas ≥ 3:1. Medido por
    `npm run verificar:contraste`.
  - O tema é aplicado por um script no `index.html` antes da primeira pintura, sem clarão.
  - Os gráficos leem as cores do tema ativo (`usePaleta`, em `graficos/cores.ts`).
  - Detalhes em `specs/002-modo-escuro-alto-contraste/`.
- **Tipografia:** só a Archivo, com contraste pela largura. Títulos em largura expandida, texto
  normal, algarismos tabulares nas tabelas.
- **Acessibilidade:**
  - todo gráfico tem uma visão em tabela;
  - foco visível pelo teclado;
  - respeita "reduzir movimento" e as cores forçadas do sistema operacional;
  - layout responsivo até celular;
  - nenhuma violação do axe-core (WCAG 2.1 AA, e AAA de contraste no escuro) nas 6 telas.
