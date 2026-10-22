# Pesquisa: Modo Escuro de Alto Contraste

## 1. Paleta do tema escuro ("papel-carbono")

- **Decisão**: o tema escuro segue a mesma direção visual do claro (a papelada do serviço
  público), trocando o ofício branco pelo papel-carbono azul-escuro. Os tokens existentes
  ganham valores novos sob `:root[data-tema="escuro"]`, sem renomear nada:

  | Token | Claro | Escuro | Uso |
  |-------|-------|--------|-----|
  | `--papel` | `#f5f7f6` | `#0f1724` | fundo |
  | `--papel-elevado` | `#ffffff` | `#18222f` | tabelas, painéis, campos |
  | `--tinta` | `#1d2a36` | `#f3f6f9` | texto principal |
  | `--tinta-suave` | `#4a5864` | `#d3dbe3` | texto secundário |
  | `--tinta-apagada` | `#6b7883` | `#b9c4ce` | legendas, eixos |
  | `--pauta` | `#cbd3d8` | `#7d8b98` | bordas de campos, botões, eixos |
  | `--pauta-leve` | `#e3e8eb` | `#2f3b4a` | grade e separadores (decorativos) |
  | `--caneta` | `#2447a6` | `#9ab8ff` | links, foco, item ativo |
  | `--caneta-escura` | `#1a3580` | `#c3d4ff` | texto sobre `--caneta-fundo`, hover |
  | `--caneta-fundo` | `#e6ebf7` | `#1c2c4f` | opção marcada, etiquetas |
  | `--carimbo` | `#6b3fa0` | `#d2b4ff` | carimbo de atualização |
  | `--alerta` | `#b42318` | `#ff9b91` | só alertas e erros |
  | `--alerta-fundo` | `#fbeceb` | `#3b1a19` | caixa de erro e de alerta |

  Tokens novos, para as cores que estavam fixas no CSS: `--sobre-caneta` e `--sobre-alerta`
  (texto sobre fundo `--caneta`/`--alerta`: branco no claro, `#0f1724` no escuro),
  `--caneta-fundo-forte` (hover das etiquetas), `--linha-destaque` (hover das linhas da
  tabela) e `--sombra` (dica dos gráficos).
- **Medição** (WCAG 2.1, razão de contraste): todo texto do tema escuro fica entre 7,7:1 e
  16,6:1 sobre `--papel` e `--papel-elevado` (o menor par é `--alerta` sobre `--alerta-fundo`,
  7,68:1); `--pauta` fica em 5,15:1 e 4,60:1. O script `npm run verificar:contraste`
  repete essas medições a cada mudança.
- **Por que não preto puro**: `#000` com branco puro causa halo e cansaço em quem tem
  astigmatismo; o azul-escuro mantém o contraste acima de 16:1 com menos ofuscamento, que é
  uma das justificativas da Solicitação de Mudança.
- **Alternativa descartada**: dois temas escuros (comum e alto contraste). A Solicitação
  de Mudança pede só o de alto contraste; um segundo tema dobraria a verificação.

## 2. Paleta dos gráficos no escuro

- **Decisão**: mesma ordem de matizes do claro (azul, laranja, verde-azulado, violeta,
  rosa, oliva), com passos próprios para o fundo escuro, gerados em OKLCH:
  `#628bea, #d37812, #05a388, #915dc5, #d9668e, #889930`; "Outros" `#a3adb6`.
- **Validação**: script da skill de dataviz,
  `validate_palette.js --mode dark --surface "#0f1724"`: todas as verificações passam
  (luminosidade 0,48–0,67, croma ≥ 0,1, separação para daltonismo ΔE 9,8, visão normal
  ΔE 16,3, contraste ≥ 3:1 com o fundo).
- **Rótulos dentro da régua**: nenhuma cor de texto atinge 7:1 sobre cores de série dentro
  da faixa validada. No escuro, o percentual dentro do segmento ganha uma etiqueta com fundo
  `--papel` e texto `--tinta` (16,6:1). No claro continua como está.
- **Cor segue a entidade**: a série 1 é a mesma entidade nos dois temas; só muda o passo
  de luminosidade.

## 3. Troca de tema sem clarão

- **Decisão**: atributo `data-tema` no `<html>`. Um script curto e síncrono no `<head>` do
  `index.html` lê a escolha guardada (ou a preferência do sistema) e aplica o atributo antes
  da primeira pintura (FR-007). O React só lê e altera esse atributo.
- **Alternativas descartadas**:
  - Só `@media (prefers-color-scheme: dark)`: não permite a escolha manual (FR-001, FR-006).
  - Aplicar o tema num `useEffect` do React: a página pintaria clara antes, que é o clarão
    que o FR-007 proíbe.

## 4. Estado do tema no React

- **Decisão**: módulo `util/tema.ts` com uma pequena "loja externa" lida por
  `useSyncExternalStore`. O tema fica no atributo do `<html>`, e a escolha da pessoa no
  `localStorage` (com `try/catch`, porque janelas anônimas podem bloquear). Se a pessoa nunca
  escolheu, o módulo escuta a mudança de preferência do sistema operacional.
- **Gráficos**: o Recharts recebe cores como texto, então `cores.ts` passa a exportar a
  paleta dos dois temas e o hook `usePaleta()` devolve a do tema ativo. Os gráficos se
  redesenham na troca.
- **Alternativa descartada**: Context do React com provedor no topo. Daria o mesmo
  resultado com mais código, e o atributo no `<html>` já é a fonte da verdade.

## 5. Verificação automática (SC-002 e SC-003)

- **Decisão**:
  - `scripts/verificar-contraste.mjs`: lê `tokens.css`, extrai os dois temas e confere cada
    par de cor usado na interface (texto ≥ 7:1 no escuro, ≥ 4,5:1 no claro; bordas e marcas
    ≥ 3:1). Sem dependências.
  - `scripts/verificar-acessibilidade.mjs`: abre as 6 telas no Google Chrome instalado
    (`puppeteer-core`), nos dois temas, e roda o `axe-core` com as regras de contraste
    `color-contrast` e `color-contrast-enhanced` (AAA). Falha se houver violação.
- **Justificativa (princípio IV)**: as duas bibliotecas são gratuitas, de código aberto,
  só de desenvolvimento e não entram no pacote do site. O `axe-core` é a referência de
  mercado para verificação automática de acessibilidade; sem ele o SC-003 dependeria de
  conferência manual.

## 6. Cores forçadas e movimento

- **Decisão**: em `@media (forced-colors: active)`, as marcas dos gráficos (segmentos da
  régua, barras, legendas) usam `forced-color-adjust: none` para não sumirem, e o foco usa a
  cor de sistema `Highlight`. A troca de tema não tem transição; a animação existente da régua
  já respeita `prefers-reduced-motion`.
