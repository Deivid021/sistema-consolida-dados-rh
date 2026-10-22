# Especificação: Modo Escuro de Alto Contraste

**Branch**: `002-modo-escuro-alto-contraste`

**Criada em**: 2026-10-21

**Situação**: Rascunho

**Entrada**: Solicitação de Mudança 3c do patrocinador (Jullian H. Creutzberg, 08/10/2026,
aprovada pelo gerente do projeto Guilherme Antunes Rodrigues). Novo requisito não funcional
RNF04: o sistema deve oferecer aos usuários a interface no Modo Escuro de Alto Contraste,
com legibilidade adequada, navegação confortável e conformidade com as diretrizes de
acessibilidade visual.

**Requisito atendido**: RNF04 (novo, criado pela Solicitação de Mudança 3c). Complementa o
RNF03 (interface responsiva) e se aplica às telas dos RF02 a RF06.

**Fontes**: Solicitação de Mudança 3c v1.0, constituição v1.1.0 e as telas entregues na
funcionalidade 001.

## Cenários de usuário e testes *(obrigatório)*

### História 1: Ligar o modo escuro de alto contraste (Prioridade: P1)

Uma pessoa com baixa visão abre o sistema e encontra, sempre no mesmo lugar do menu, a
opção de trocar a aparência para o modo escuro de alto contraste. Ao escolher, todas as
telas passam a ter fundo escuro e textos, bordas, gráficos e indicadores de foco em alto
contraste, sem perder nenhuma informação.

**Por que esta prioridade**: é o próprio pedido do patrocinador. Sem essa opção o sistema
fica inviável ou desconfortável para quem depende de alto contraste.

**Teste independente**: abrir qualquer tela no tema claro, acionar a opção de aparência e
conferir que a tela inteira muda para o modo escuro de alto contraste, mantendo os mesmos
dados, filtros e posição.

**Cenários de aceitação**:

1. **Dado** que a pessoa está em qualquer tela no tema claro, **quando** escolhe o modo
   escuro de alto contraste, **então** a tela muda na hora, sem recarregar a página e sem
   perder filtros, página da tabela ou posição de rolagem.
2. **Dado** o modo escuro ligado, **quando** a pessoa navega para outra tela, **então** a
   nova tela também aparece no modo escuro.
3. **Dado** o modo escuro ligado, **quando** a pessoa escolhe o tema claro, **então** o
   sistema volta à aparência original.
4. **Dado** que a pessoa usa só o teclado, **quando** chega à opção de aparência, **então**
   consegue trocá-la sem mouse, e o leitor de tela anuncia o nome da opção e qual tema
   está ativo.

---

### História 2: Ler tudo com conforto no modo escuro (Prioridade: P1)

No modo escuro de alto contraste, a pessoa lê textos, números, rótulos de gráficos,
tabelas, filtros, mensagens de erro e alertas sem esforço, e distingue as séries dos
gráficos umas das outras.

**Por que esta prioridade**: um modo escuro com contraste ruim não atende o objetivo da
mudança (inclusão e legibilidade). Esta história garante a qualidade da História 1.

**Teste independente**: com o modo escuro ligado, medir o contraste de cada tipo de
elemento em todas as telas e conferir os limites definidos nos requisitos.

**Cenários de aceitação**:

1. **Dado** o modo escuro ligado, **quando** a pessoa lê qualquer texto da interface
   (títulos, parágrafos, tabelas, rótulos, campos e mensagens), **então** o contraste com o
   fundo atende o nível AAA das diretrizes de acessibilidade (no mínimo 7:1).
2. **Dado** o modo escuro ligado, **quando** a pessoa olha um gráfico com duas ou mais
   séries, **então** cada série se distingue do fundo e das vizinhas, e a legenda e a
   tabela do gráfico continuam disponíveis.
3. **Dado** o modo escuro ligado, **quando** a pessoa navega com o teclado, **então** o
   elemento em foco fica destacado por um contorno claramente visível.
4. **Dado** o modo escuro ligado, **quando** a tela de Alertas ou o aviso do Panorama
   mostram alertas, **então** o destaque de alerta continua reconhecível e não depende só
   da cor.

---

### História 3: Respeitar a preferência da pessoa (Prioridade: P2)

Quem já usa o computador ou o celular em modo escuro abre o sistema e ele já aparece no
modo escuro de alto contraste. A escolha feita no sistema fica guardada para as próximas
visitas no mesmo navegador.

**Por que esta prioridade**: atende a terceira justificativa da mudança (respeito à
preferência do usuário). O sistema já é útil sem isso, mas obriga a pessoa a trocar o tema
toda vez.

**Teste independente**: configurar o sistema operacional em modo escuro, abrir o sistema
pela primeira vez e conferir o tema. Depois trocar o tema, fechar o navegador, abrir de
novo e conferir que a escolha foi mantida.

**Cenários de aceitação**:

1. **Dado** um navegador que nunca abriu o sistema e o sistema operacional em modo escuro,
   **quando** a pessoa abre o sistema, **então** ele aparece no modo escuro de alto
   contraste.
2. **Dado** um navegador que nunca abriu o sistema e o sistema operacional em modo claro,
   **quando** a pessoa abre o sistema, **então** ele aparece no tema claro.
3. **Dado** que a pessoa escolheu um tema no sistema, **quando** volta outro dia no mesmo
   navegador, **então** o sistema abre no tema escolhido, mesmo que o sistema operacional
   esteja diferente.
4. **Dado** que a pessoa abre o sistema no modo escuro, **quando** a página carrega,
   **então** não aparece um clarão do tema claro antes do escuro.

---

### Casos-limite

- **Navegador que não guarda preferências** (janela anônima, armazenamento bloqueado): a
  troca de tema funciona durante a visita; na próxima visita vale a preferência do sistema
  operacional.
- **Modo de alto contraste do sistema operacional** (cores forçadas, como no Windows): o
  sistema não pode esconder textos, bordas, foco ou marcas de gráfico; as cores do sistema
  operacional prevalecem.
- **Tela pequena (celular, a partir de 360 pixels)**: a opção de aparência continua
  visível e acionável no menu, sem rolagem horizontal (RNF03).
- **Pessoa troca o tema do sistema operacional com o sistema aberto**: se ela nunca
  escolheu um tema no sistema, a aparência acompanha a troca; se já escolheu, a escolha
  dela prevalece.
- **Animações**: a troca de tema não pode usar animação piscante; quem pede movimento
  reduzido não vê transição.
- **Exportações CSV e PDF**: não mudam com o tema; o PDF continua com fundo branco para
  impressão.

## Requisitos *(obrigatório)*

### Requisitos funcionais

- **FR-001**: O sistema DEVE oferecer duas aparências: tema claro (o atual) e Modo Escuro
  de Alto Contraste.
- **FR-002**: A opção de aparência DEVE ficar no menu principal, visível em todas as telas,
  em computador e celular.
- **FR-003**: A troca de aparência DEVE valer na hora para a tela inteira, sem recarregar a
  página e sem perder filtros, paginação, ordenação ou rolagem.
- **FR-004**: A opção de aparência DEVE ser acionável só pelo teclado e DEVE informar ao
  leitor de tela o seu nome e o tema ativo.
- **FR-005**: No primeiro acesso, o sistema DEVE usar a preferência de aparência do sistema
  operacional (escuro leva ao Modo Escuro de Alto Contraste; claro ou sem preferência leva
  ao tema claro).
- **FR-006**: Depois que a pessoa escolhe um tema, o sistema DEVE guardar a escolha no
  navegador e usá-la nas visitas seguintes, à frente da preferência do sistema operacional.
- **FR-007**: O sistema NÃO DEVE mostrar o tema errado, nem por um instante, ao abrir a
  página.
- **FR-008**: O Modo Escuro de Alto Contraste DEVE cobrir todas as telas (Panorama,
  Servidores, Salários, Entradas e saídas, Alertas e página não encontrada) e todos os
  componentes: menu, carimbo de atualização, cabeçalhos, filtros, etiquetas de filtro,
  campos de busca e data, botões, tabelas, paginação, exportação, gráficos (régua, barras,
  linhas, dicas ao passar o mouse e tabelas dos gráficos) e os estados de carregando, erro
  e vazio.
- **FR-009**: No Modo Escuro de Alto Contraste, todo texto DEVE ter contraste de no mínimo
  7:1 com o fundo (nível AAA, critério 1.4.6 da WCAG 2.1), inclusive textos secundários,
  rótulos de eixo, valores de gráfico e textos desabilitados que transmitam informação.
- **FR-010**: No Modo Escuro de Alto Contraste, bordas de campos e botões, marcas de
  gráfico, linhas de grade necessárias à leitura e ícones DEVEM ter contraste de no mínimo
  3:1 com o fundo (critério 1.4.11 da WCAG 2.1).
- **FR-011**: O indicador de foco do teclado DEVE ser visível nos dois temas, com contraste
  de no mínimo 3:1 com o fundo ao redor.
- **FR-012**: Nos gráficos, cada série DEVE ser distinguível das vizinhas também por
  pessoas com daltonismo, e a identidade das séries NÃO DEVE depender só da cor (legenda e
  tabela do gráfico continuam presentes).
- **FR-013**: O destaque de alerta (cor reservada a alertas) DEVE continuar exclusivo de
  alertas no Modo Escuro e vir acompanhado de texto ou ícone.
- **FR-014**: Em modo de cores forçadas do sistema operacional, o sistema DEVE manter
  visíveis textos, bordas, foco e marcas de gráfico.
- **FR-015**: A troca de tema NÃO DEVE usar animações piscantes e DEVE respeitar a
  preferência de movimento reduzido.
- **FR-016**: As exportações CSV e PDF NÃO DEVEM mudar de acordo com o tema.

### Entidades principais

- **Preferência de aparência**: tema escolhido pela pessoa (claro ou Modo Escuro de Alto
  Contraste), ou ausência de escolha. Guardada só no navegador da pessoa; não é dado
  pessoal e não vai para o servidor.

## Critérios de sucesso *(obrigatório)*

### Resultados mensuráveis

- **SC-001**: Em 100% das telas, a pessoa liga o Modo Escuro de Alto Contraste com no
  máximo 2 ações (abrir o menu no celular e escolher o tema), sem recarregar a página.
- **SC-002**: 100% dos pares de cor de texto do Modo Escuro atingem contraste de no mínimo
  7:1, e 100% dos elementos gráficos e de interface atingem no mínimo 3:1, conferidos com
  medição de contraste.
- **SC-003**: Uma verificação automática de acessibilidade nas 6 telas, no Modo Escuro,
  não aponta nenhuma falha de contraste.
- **SC-004**: Em 100% dos primeiros acessos com o sistema operacional em modo escuro, o
  sistema abre direto no Modo Escuro, sem clarão do tema claro.
- **SC-005**: A escolha de tema é mantida em 100% das visitas seguintes no mesmo navegador
  (quando o navegador permite guardar preferências).
- **SC-006**: Todas as tarefas principais (buscar um servidor, filtrar salários, agrupar
  entradas e saídas por mês, exportar alertas) podem ser feitas no Modo Escuro, só com o
  teclado, em computador e em celular de 360 pixels.

## Premissas

- O tema claro atual continua como está; esta mudança só acrescenta o Modo Escuro de Alto
  Contraste e a opção de troca.
- Haverá um único tema escuro, já de alto contraste. Um modo escuro "comum", de contraste
  menor, fica fora do escopo, porque a Solicitação de Mudança pede alto contraste.
- O nível de contraste de texto adotado é o AAA (7:1), por ser o nível associado a "alto
  contraste" nas diretrizes; o tema claro atual continua no nível AA já entregue.
- A preferência fica guardada só no navegador (não há login no sistema); trocar de
  navegador ou de aparelho volta à preferência do sistema operacional.
- O back-end, a API e os dados não mudam. As exportações CSV e PDF ficam fora do escopo,
  como indicado na entrada.
- O esforço estimado pela Solicitação de Mudança é de 15 horas, sem impacto no prazo de
  entrega (22/10/2026), com horas extras.
- Pacote da EAP: interface do usuário (front-end). A tarefa correspondente deve ser
  registrada no Worklenz.
