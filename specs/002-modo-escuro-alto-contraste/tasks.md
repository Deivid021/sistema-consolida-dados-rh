---

description: "Lista de tarefas da funcionalidade 002: Modo Escuro de Alto Contraste (RNF04)"
---

# Tarefas: Modo Escuro de Alto Contraste

**Entrada**: documentos de projeto em `specs/002-modo-escuro-alto-contraste/`

**Pré-requisitos**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [quickstart.md](quickstart.md)

**Testes**: verificação automática de contraste (script) e de acessibilidade (axe-core), mais
conferência visual por capturas de tela. O princípio V exige que o contraste seja medido, não
avaliado a olho.

**Responsáveis e prazo**: desenvolvimento por Deivid Sestren Santos com o Claude; revisão de
qualidade por Kaue Gorges e Guilherme Antunes Rodrigues. Prazo de todas as tarefas:
22/10/2026, antes da apresentação. Registrar no Worklenz como pacote "Interface do usuário,
Solicitação de Mudança 3c".

**Formato**: `[ID] [P?] [História] Descrição`. `[P]` indica tarefa que pode ser feita em
paralelo (arquivos diferentes, sem dependência).

## Fase 1: Preparação

- [X] T001 Emendar `.specify/memory/constitution.md` para a v1.1.0, incluindo o RNF04 no princípio III e no V (Solicitação de Mudança 3c).
- [ ] T002 [P] Adicionar `axe-core` e `puppeteer-core` como dependências de desenvolvimento em `frontend/package.json` e os scripts `verificar:contraste` e `verificar:acessibilidade`.

## Fase 2: Fundação (bloqueia as histórias)

- [ ] T003 Em `frontend/src/estilos/tokens.css`, criar os tokens que faltavam (`--sobre-caneta`, `--sobre-alerta`, `--caneta-fundo-forte`, `--linha-destaque`, `--sombra`) e o bloco `:root[data-tema="escuro"]` com os valores de [research.md](research.md) §1, incluindo as séries dos gráficos (§2) e `color-scheme: dark`.
- [ ] T004 [P] Trocar as cores fixas por tokens em `frontend/src/componentes/Filtros.css`, `Tabela.css`, `Layout.css` e `graficos/Graficos.css` (FR-008).
- [ ] T005 Criar `frontend/scripts/verificar-contraste.mjs`: ler os dois temas de `tokens.css` e conferir os pares de cor da interface (texto ≥ 7:1 no escuro e ≥ 4,5:1 no claro; bordas, foco e marcas ≥ 3:1). Sai com erro se algum par falhar (FR-009 a FR-011, SC-002).

## Fase 3: História 1, ligar o modo escuro (P1)

**Objetivo**: a pessoa troca o tema pelo índice e a tela inteira muda na hora.

**Teste independente**: em qualquer tela, escolher **Escuro** em **Aparência** e conferir a
mudança sem recarregar e sem perder filtros.

- [ ] T006 [US1] Criar `frontend/src/util/tema.ts`: tema em uso no atributo `data-tema` do `<html>`, escolha no `localStorage` (com `try/catch`), hook `useTema()` com `useSyncExternalStore` e `escolherTema()` (FR-001, FR-003, FR-006).
- [ ] T007 [US1] Permitir rótulo com texto oculto para leitor de tela em `frontend/src/componentes/Alternativas.tsx` e criar `frontend/src/componentes/SeletorTema.tsx` ("Aparência": Claro / Escuro, anunciado como "Escuro de alto contraste") (FR-004).
- [ ] T008 [US1] Colocar o `SeletorTema` no índice em `frontend/src/componentes/Layout.tsx` e `Layout.css`, no rodapé do índice no computador e na faixa do topo no celular (FR-002, RNF03).

## Fase 4: História 2, ler tudo com conforto (P1)

**Objetivo**: todos os componentes e gráficos legíveis no escuro, com contraste medido.

**Teste independente**: `npm run verificar:contraste` e `npm run verificar:acessibilidade`
sem falhas, mais as capturas das 6 telas no escuro.

- [ ] T009 [US2] Em `frontend/src/componentes/graficos/cores.ts`, exportar a paleta dos dois temas e o hook `usePaleta()`; generalizar `corDoTextoSobre` para receber as duas cores candidatas (FR-012).
- [ ] T010 [P] [US2] Usar `usePaleta()` em `graficos/LinhasTempo.tsx` (linhas, grade, eixos, cursor, ponto ativo) (FR-008, FR-010).
- [ ] T011 [P] [US2] Usar `usePaleta()` em `graficos/Regua.tsx`; no escuro, rótulo dentro do segmento com fundo `--papel` e texto `--tinta` ([research.md](research.md) §2) (FR-009).
- [ ] T012 [US2] Em `frontend/src/estilos/base.css`, regras de `forced-colors` (marcas dos gráficos com `forced-color-adjust: none`, foco com `Highlight`) (FR-014, FR-015).
- [ ] T013 [US2] Criar `frontend/scripts/verificar-acessibilidade.mjs`: abrir as 6 telas no Google Chrome (`puppeteer-core`) nos dois temas e rodar o `axe-core` com `color-contrast` e `color-contrast-enhanced`; falhar se houver violação (SC-003).
- [ ] T014 [US2] Conferir visualmente as 6 telas no escuro, no computador e em 360 px, por capturas de tela, e corrigir o que aparecer (SC-006).

## Fase 5: História 3, respeitar a preferência (P2)

**Objetivo**: primeiro acesso segue o sistema operacional; a escolha fica guardada; sem clarão.

**Teste independente**: janela anônima com o sistema operacional em escuro abre no escuro;
recarregar depois de escolher mantém o tema.

- [ ] T015 [US3] Script síncrono no `<head>` de `frontend/index.html` que aplica `data-tema` antes da primeira pintura: escolha guardada, senão preferência do sistema operacional (FR-005, FR-007).
- [ ] T016 [US3] Em `util/tema.ts`, acompanhar a mudança de preferência do sistema operacional enquanto a pessoa não escolheu um tema (caso-limite da especificação).

## Fase 6: Acabamento

- [ ] T017 [P] Atualizar `frontend/README.md` (temas, tokens e verificações) e o `README.md` da raiz (RNF04).
- [ ] T018 Rodar o [quickstart.md](quickstart.md) inteiro, registrar o resultado e conferir que as exportações CSV e PDF não mudaram (FR-016).
- [ ] T019 Revisão de qualidade por Kaue e Guilherme no tema escuro, com registro no Termo de Aceite da Entrega.

## Dependências

- T003 bloqueia T004, T005, T009 a T012.
- T006 bloqueia T007, T008, T009 (o hook de paleta lê o tema) e T015 e T016.
- T013 e T014 dependem das fases 3 e 4 completas.
- Histórias 1 e 2 juntas formam o mínimo entregável (P1); a História 3 completa a experiência.
