# Plano de implementação: Modo Escuro de Alto Contraste

**Branch**: `002-modo-escuro-alto-contraste` | **Data**: 2026-10-21 | **Especificação**: [spec.md](spec.md)

**Entrada**: especificação em `specs/002-modo-escuro-alto-contraste/spec.md` (RNF04, Solicitação
de Mudança 3c)

## Resumo

Acrescentar ao front-end um segundo tema, o Modo Escuro de Alto Contraste, e uma opção de
aparência no índice lateral. A troca vale na hora, guarda a escolha no navegador e, no
primeiro acesso, segue a preferência do sistema operacional, sem clarão do tema errado.

Abordagem: o front já usa tokens de cor em CSS (`frontend/src/estilos/tokens.css`). O tema
escuro redefine esses mesmos tokens sob `:root[data-tema="escuro"]`, um script curto no
`index.html` escolhe o tema antes da primeira pintura, e os gráficos passam a ler a paleta do
tema ativo. Nenhuma mudança no back-end, na API ou nos dados.

## Contexto técnico

**Linguagem/versão**: TypeScript 6, CSS

**Dependências principais**: React 19, React Router 8, Recharts 3 (já existentes). Para a
verificação automática de acessibilidade (SC-003), entram só como dependências de
desenvolvimento: `axe-core` e `puppeteer-core`, usando o Google Chrome já instalado.

**Armazenamento**: `localStorage` do navegador, chave `tema` (ver [data-model.md](data-model.md))

**Testes**: script de contraste (`npm run verificar:contraste`) que lê os tokens do tema
escuro e confere os limites de FR-009 a FR-011; varredura com axe-core nas 6 telas nos dois
temas (`npm run verificar:acessibilidade`); validação da paleta dos gráficos com o script
da skill de dataviz; conferência visual por capturas de tela.

**Plataforma**: navegadores atuais (Chrome, Firefox, Safari, Edge), computador e celular a
partir de 360 px

**Tipo de projeto**: aplicação web (pasta `frontend/`, empacotada dentro do jar do back-end)

**Metas de desempenho**: troca de tema instantânea (só troca de atributo e variáveis CSS,
sem nova requisição à API)

**Restrições**: texto com contraste ≥ 7:1, elementos de interface e marcas de gráfico ≥ 3:1,
sem clarão do tema claro, respeito a `prefers-reduced-motion` e `forced-colors`

**Escala/escopo**: 6 telas, cerca de 15 componentes visuais, 1 opção de aparência

## Verificação da constituição

*Portão: deve passar antes da Fase 0 e ser conferido de novo depois da Fase 1.*

| Princípio | Situação | Observação |
|-----------|----------|------------|
| I. Dados públicos e proteção de dados pessoais | ✅ Passa | A preferência de tema não é dado pessoal, fica só no navegador e não vai ao servidor. Nenhum dado novo é lido ou gravado. |
| II. Fidelidade à fonte e rastreabilidade | ✅ Passa | Não altera dados, indicadores, alertas nem exportações. O aviso de alerta continua com o critério visível. |
| III. Escopo fechado | ✅ Passa | Atende o RNF04, incluído pela Solicitação de Mudança 3c aprovada pelo patrocinador; constituição emendada para v1.1.0. |
| IV. Simplicidade e custo zero | ✅ Passa | Reaproveita os tokens CSS existentes; nenhuma biblioteca de tema. As duas dependências novas são gratuitas, de código aberto e só de desenvolvimento (justificativa em [research.md](research.md) §5). |
| V. Qualidade verificável | ✅ Passa | Contraste medido por script e axe-core; responsividade (RNF03) conferida em 360 px nos dois temas. |

Conferência pós-design: sem violações. Tabela de complexidade não se aplica.

## Estrutura do projeto

### Documentação (esta funcionalidade)

```text
specs/002-modo-escuro-alto-contraste/
├── spec.md
├── plan.md              # este arquivo
├── research.md          # decisões: paleta, troca sem clarão, gráficos, verificação
├── data-model.md        # preferência de aparência
├── quickstart.md        # como validar
├── checklists/
│   └── requirements.md
└── tasks.md             # gerado pelo /speckit-tasks
```

Sem pasta `contracts/`: a funcionalidade não muda a API de consulta (o contrato da
funcionalidade 001 continua valendo).

### Código-fonte

```text
frontend/
├── index.html                       # script que aplica o tema antes da primeira pintura
├── package.json                     # scripts verificar:contraste e verificar:acessibilidade
├── scripts/
│   ├── verificar-contraste.mjs      # mede os pares de cor dos dois temas
│   └── verificar-acessibilidade.mjs # axe-core nas 6 telas, nos dois temas
└── src/
    ├── estilos/
    │   ├── tokens.css               # + bloco :root[data-tema="escuro"] e tokens que faltavam
    │   └── base.css                 # + regras de forced-colors
    ├── util/
    │   └── tema.ts                  # ler, aplicar e guardar o tema; hook useTema
    └── componentes/
        ├── Layout.tsx / .css        # opção de aparência no índice
        ├── SeletorTema.tsx          # opção de aparência (botões de rádio)
        ├── Filtros.css, Tabela.css  # cores fixas trocadas por tokens
        └── graficos/
            ├── cores.ts             # paleta por tema; contraste de rótulo genérico
            ├── Regua.tsx            # usa a paleta do tema ativo
            ├── LinhasTempo.tsx      # idem
            └── Graficos.css         # sombra da dica por token
```

**Decisão de estrutura**: tudo dentro de `frontend/`, seguindo as pastas existentes. O
back-end não muda.
