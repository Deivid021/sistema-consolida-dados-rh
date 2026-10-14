# Especificação da Funcionalidade: Consolidação e Consulta dos Dados de RH da Prefeitura de Gravataí/RS

**Branch da funcionalidade**: `001-consolidacao-dados-rh`

**Criada em**: 2026-10-13

**Status**: Rascunho

**Entrada**: Descrição do usuário: "Especificar todas as regras de negócio do Sistema de Consolidação de Dados de RH da Prefeitura de Gravataí/RS, com base nos documentos em docs-project e na constituição: RF01 a RF06 e RNF01 a RNF03. Registrar como dúvidas abertas as questões de arquitetura, banco de dados e regras ainda indefinidas."

**Fontes**: Termo de Abertura v1.2, Declaração do Escopo v1.3, Dicionário da EAP v1.0, constituição v1.0.0 e consulta direta à API pública em 2026-10-13.

## Cenários de Usuário e Testes *(obrigatório)*

Os usuários são gestores públicos, vereadores, órgãos de controle, sindicatos, jornalistas e
cidadãos. A equipe do projeto atua como operadora da carga de dados.

### História de Usuário 1 - Importar e manter os dados de pessoal (Prioridade: P1)

Como operador do sistema, quero que todos os registros de pessoal publicados pela Prefeitura
sejam importados e atualizados automaticamente, para que as consultas e os indicadores
reflitam a fonte oficial sem trabalho manual.

**Por que esta prioridade**: sem dados importados nenhuma outra funcionalidade existe. Atende
ao RF01 e ao Objetivo 1 (meta de 100% dos registros).

**Teste independente**: executar uma carga completa e comparar a quantidade de registros
gravados e rejeitados com o total informado pela fonte; verificar que nenhum CPF foi gravado.

**Cenários de aceitação**:

1. **Dado** que a fonte informa N registros no período configurado, **Quando** a carga
   completa termina, **Então** a soma de registros gravados e rejeitados é igual a N e o
   resultado fica registrado com data, quantidades e situação.
2. **Dado** um registro recebido com CPF (mesmo mascarado), **Quando** ele é tratado,
   **Então** o CPF é descartado e não aparece em nenhum armazenamento, log ou exportação.
3. **Dado** que uma carga falha no meio (fonte indisponível ou limite de requisições),
   **Quando** o usuário consulta o sistema, **Então** ele continua vendo os dados da última
   carga concluída com sucesso, com a data dessa carga visível.
4. **Dado** que um vínculo mudou de situação, cargo, lotação ou salário entre duas cargas,
   **Quando** a nova carga termina, **Então** a mudança fica registrada no histórico com a
   data em que foi detectada.
5. **Dado** o horário agendado, **Quando** ele chega, **Então** a sincronização roda sem
   intervenção manual e registra seu resultado.

---

### História de Usuário 2 - Consultar servidores com busca e filtros (Prioridade: P1)

Como cidadão ou gestor, quero buscar e filtrar os vínculos de pessoal, para localizar
registros específicos e analisar o quadro por diferentes critérios.

**Por que esta prioridade**: é a funcionalidade central do site (RF02) e a base para
indicadores, gráficos e relatórios.

**Teste independente**: aplicar cada filtro isolado e em combinação e conferir os resultados
com uma amostra da fonte.

**Cenários de aceitação**:

1. **Dado** dados importados, **Quando** o usuário filtra por situação "Afastado" e por uma
   secretaria, **Então** vê apenas os vínculos que atendem aos dois critérios e o total
   encontrado.
2. **Dado** um período de admissão informado, **Quando** a consulta é feita, **Então** só
   aparecem vínculos admitidos entre as duas datas, inclusive.
3. **Dado** um texto digitado na busca, **Quando** a consulta é feita, **Então** aparecem os
   vínculos cujo nome, cargo ou matrícula contém o texto, sem diferenciar maiúsculas, minúsculas
   ou acentos.
4. **Dado** uma consulta sem resultado, **Quando** ela termina, **Então** o usuário vê uma
   mensagem clara de que nada foi encontrado e pode limpar os filtros.
5. **Dado** qualquer consulta, **Quando** o resultado é exibido, **Então** a resposta chega
   em até 3 segundos e a tela mostra a data da última atualização dos dados.

---

### História de Usuário 3 - Ver indicadores de RH (Prioridade: P2)

Como gestor ou vereador, quero ver quantos vínculos existem por situação, regime e
secretaria e quanto somam e qual a média do salário base por cargo e nível, para entender a
composição e o custo do quadro.

**Por que esta prioridade**: entrega a análise que hoje é manual (RF03), mas depende das
histórias 1 e 2.

**Teste independente**: recalcular os indicadores a partir dos dados armazenados e comparar
com o que a tela mostra.

**Cenários de aceitação**:

1. **Dado** dados importados, **Quando** o usuário abre os indicadores, **Então** vê a
   contagem de vínculos por situação, por regime e por secretaria, e a soma de cada
   agrupamento é igual ao total geral.
2. **Dado** um cargo e nível com vários vínculos, **Quando** o indicador salarial é
   exibido, **Então** mostra quantidade, soma e média do salário base, com duas casas
   decimais.
3. **Dado** filtros aplicados na consulta, **Quando** o usuário abre os indicadores,
   **Então** os números consideram os mesmos filtros.

---

### História de Usuário 4 - Ver gráficos de distribuição e movimentação (Prioridade: P2)

Como jornalista ou cidadão, quero ver gráficos do quadro por regime e secretaria, das
admissões e desligamentos ao longo do tempo e dos principais motivos de rescisão, para
acompanhar as movimentações de pessoal.

**Por que esta prioridade**: torna os dados compreensíveis para quem não é técnico (RF04).

**Teste independente**: conferir os valores de cada gráfico com a contagem obtida na
consulta com os mesmos filtros.

**Cenários de aceitação**:

1. **Dado** dados importados, **Quando** o usuário abre os gráficos, **Então** vê a
   distribuição dos vínculos por regime e por secretaria.
2. **Dado** um período escolhido, **Quando** o gráfico de movimentação é exibido, **Então**
   mostra admissões e desligamentos por mês ou por ano dentro do período.
3. **Dado** desligamentos no período, **Quando** o gráfico de motivos é exibido, **Então**
   mostra os dez motivos mais frequentes e agrupa os demais em "Outros".

---

### História de Usuário 5 - Receber alertas de salários (Prioridade: P3)

Como órgão de controle, quero que o sistema sinalize salários base acima do teto municipal
e salários fora do padrão do mesmo cargo e nível, para direcionar verificações.

**Por que esta prioridade**: agrega valor de controle (RF06), mas depende de regras ainda em
definição (pacote 2.4 da EAP, prazo 15/10/2026).

**Teste independente**: carregar dados de teste com casos conhecidos e verificar quais
vínculos são sinalizados por cada regra.

**Cenários de aceitação**:

1. **Dado** o teto obtido do vínculo do prefeito ou configurado, **Quando** um vínculo ativo tem salário base maior que o
   teto, **Então** ele é sinalizado, com o valor do teto e a diferença visíveis.
2. **Dado** um vínculo com salário base igual ao teto, **Quando** as regras são avaliadas,
   **Então** ele não é sinalizado.
3. **Dado** um grupo de mesmo cargo, nível e horas mensais, **Quando** o salário base de um
   vínculo difere mais de 20% da mediana do grupo, **Então** ele é sinalizado, com o valor de referência do
   grupo visível.
4. **Dado** qualquer alerta exibido, **Quando** o usuário o lê, **Então** o texto deixa
   claro que é uma sinalização para verificação e não uma afirmação de irregularidade.

---

### História de Usuário 6 - Exportar relatórios em CSV e PDF (Prioridade: P3)

Como usuário, quero exportar as consultas e os indicadores em CSV e PDF, para usar os dados
fora do sistema e compartilhá-los.

**Por que esta prioridade**: complementa as demais funcionalidades (RF05).

**Teste independente**: exportar uma consulta filtrada e conferir que o arquivo tem os
mesmos registros e totais da tela.

**Cenários de aceitação**:

1. **Dado** uma consulta com filtros, **Quando** o usuário exporta em CSV, **Então** o
   arquivo contém todos os registros do resultado, não só a página visível, e abre
   corretamente em planilhas configuradas para o Brasil.
2. **Dado** uma consulta ou tela de indicadores, **Quando** o usuário exporta em PDF,
   **Então** o arquivo traz título, filtros aplicados, data da última atualização dos
   dados, data de geração e totais.
3. **Dado** qualquer exportação, **Quando** o arquivo é gerado, **Então** ele não contém
   CPF.

---

### Casos de Borda

- A fonte responde com erro dentro de uma resposta aparentemente bem-sucedida (mensagem de
  erro no corpo): a carga deve tratar como falha, e não como página vazia.
- A fonte limita a uma requisição por segundo e recusa o excesso: a carga deve respeitar o
  limite e repetir a página recusada, sem pular registros.
- O total de registros informado muda durante a carga (dados atualizados no meio do
  processo): a carga deve detectar a diferença e registrar o resultado como inconsistente.
- Vínculo presente em uma carga anterior e ausente na atual: o registro é mantido e marcado
  como não encontrado na última carga; nunca é apagado.
- Mesma pessoa com mais de um vínculo (matrículas "27-01" e "27-02"): cada vínculo é um
  registro independente.
- Salário base "0,00" ou ausente (comum em desligamentos antigos): entra nas contagens, mas
  fica fora da média e dos alertas, e é contado à parte.
- Nível, classificação, função, local de trabalho ou forma de investidura ausentes: exibidos
  como "Não informado" e agrupados sob esse rótulo.
- Datas, valores ou horas em formato inválido: o registro é rejeitado, contado e registrado
  com o motivo.
- Aposentados, pensionistas e participantes de PDV aparecem na fonte com situação
  "Trabalhando" e regime próprio ("INATIVO", "PENSIONISTA", "INATIVOS PDV").
- Motivos de rescisão chegam cortados em 40 caracteres na fonte: são exibidos como vieram.
- Grupo de cargo, nível e horas mensais com menos de 5 vínculos ativos: não gera alerta de salário fora do padrão.
- Período de filtro com data inicial maior que a final: o sistema avisa e não consulta.

## Requisitos *(obrigatório)*

A coluna de rastreabilidade liga cada regra aos requisitos aprovados (RF01 a RF06 e RNF01 a
RNF03) da Declaração do Escopo.

### Requisitos Funcionais

**Importação e tratamento (RF01, RNF01)**

- **FR-001**: O sistema DEVE importar os registros do serviço público
  `/transparencia-pessoal-funcionarios` de Gravataí com `tipoBusca=3` (trabalhando,
  afastados e desligados), percorrendo todas as páginas informadas pela fonte.
- **FR-002**: O sistema DEVE importar todo o histórico de desligados disponível na fonte,
  consultando o período de desligamento de 01/01/1900 até a data da carga, além de todos os
  vínculos ativos. Em 13/10/2026 isso correspondia a 55.206 registros.
- **FR-003**: Ao fim de cada carga, o sistema DEVE conferir que registros gravados mais
  registros rejeitados é igual ao total informado pela fonte. Diferença torna a carga
  inconsistente e ela NÃO DEVE substituir os dados publicados.
- **FR-004**: O sistema DEVE respeitar o limite de uso da fonte (uma requisição por
  segundo) e repetir páginas recusadas ou com erro até um número máximo de tentativas,
  registrando a falha se o limite for atingido.
- **FR-005**: O sistema DEVE descartar o CPF de cada registro antes de qualquer gravação,
  inclusive o CPF mascarado que a fonte envia, e NÃO DEVE gravá-lo em banco, arquivo,
  cache, log ou cópia local.
- **FR-006**: O sistema DEVE converter datas (dia/mês/ano), valores monetários (formato
  brasileiro, como "3.905,17") e horas (como "180:00") para tipos próprios, preservando o
  valor original recebido.
- **FR-007**: O sistema DEVE rejeitar registros sem matrícula ou com data, valor ou hora
  inválidos, registrando o motivo e contando-os no resultado da carga. O sistema NÃO DEVE
  corrigir nem inferir valores.
- **FR-008**: O sistema DEVE identificar cada vínculo pela combinação de entidade e
  matrícula completa (número e sequência do contrato). Cargas seguintes atualizam o mesmo
  vínculo, sem duplicar.
- **FR-009**: O sistema DEVE registrar, para cada carga: início, fim, período consultado,
  total informado pela fonte, quantidade gravada, quantidade rejeitada e situação (sucesso,
  falha ou inconsistente).
- **FR-010**: O sistema DEVE manter o histórico das mudanças detectadas entre cargas em
  situação, regime, secretaria, local de trabalho, cargo, função, nível, salário base e
  carga horária, com a data da detecção.
- **FR-011**: O sistema DEVE manter vínculos que deixaram de aparecer na fonte, marcados
  como não encontrados na última carga, com a data da última vez em que foram vistos.
- **FR-012**: O sistema DEVE sincronizar automaticamente, uma vez por dia, fora do horário
  comercial, e manter os dados da última carga bem-sucedida disponíveis durante e depois de
  uma carga com falha.
- **FR-013**: Todas as telas e exportações DEVEM mostrar a data e a hora da última carga
  bem-sucedida.

**Consulta, busca e filtros (RF02, RNF02)**

- **FR-014**: O sistema DEVE permitir filtrar por situação, regime, classificação,
  secretaria (centro de custo), local de trabalho, cargo, período de admissão e período de
  desligamento.
- **FR-015**: Filtros diferentes DEVEM ser combinados com "E". Vários valores escolhidos no
  mesmo filtro DEVEM ser combinados com "OU".
- **FR-016**: As opções de cada filtro DEVEM vir dos valores existentes nos dados
  importados, incluindo "Não informado" quando houver registros sem o dado.
- **FR-017**: Os períodos DEVEM incluir as datas inicial e final. Informar só uma das datas
  é permitido. Data inicial maior que a final DEVE gerar aviso, sem consulta.
- **FR-018**: O filtro de período de desligamento DEVE retornar apenas vínculos desligados.
- **FR-019**: A busca por texto DEVE procurar em nome, cargo e matrícula, sem diferenciar
  maiúsculas, minúsculas e acentos.
- **FR-020**: O resultado DEVE mostrar, por vínculo: nome, matrícula, situação, regime,
  classificação, secretaria, local de trabalho, cargo, função, nível, salário base, carga
  horária mensal, data de admissão e, se houver, data e motivo do desligamento.
- **FR-021**: O sistema DEVE armazenar e exibir o nome completo do servidor, como publicado
  pela fonte, e permitir a busca por nome. O nome é dado público pela Lei de Acesso à
  Informação; a proibição de armazenamento vale para o CPF e para dados pessoais sensíveis.
- **FR-022**: O resultado DEVE ser paginado, ordenável pelas colunas e mostrar o total de
  vínculos encontrados.
- **FR-023**: A situação DEVE usar os valores da fonte ("Trabalhando", "Afastado" e
  "Demitido"). A interface DEVE exibir "Demitido" como "Desligado", porque o valor cobre
  também aposentadoria, exoneração, falecimento e fim de contrato.

**Indicadores (RF03)**

- **FR-024**: O sistema DEVE mostrar a quantidade de vínculos por situação, por regime e por
  secretaria. A unidade de contagem é o vínculo (matrícula), e não a pessoa, porque o
  sistema não guarda o CPF e o nome não identifica a pessoa com segurança (há homônimos).
- **FR-025**: O sistema DEVE mostrar quantidade, soma e média do salário base por cargo e
  nível.
- **FR-026**: Soma e média salariais DEVEM considerar, por padrão, apenas vínculos ativos
  ("Trabalhando" e "Afastado"). Vínculos com salário base zero ou ausente ficam fora da
  média e são mostrados em contagem separada.
- **FR-027**: Os indicadores DEVEM respeitar os filtros ativos, e os totais dos
  agrupamentos DEVEM fechar com o total geral da consulta.
- **FR-028**: Valores monetários DEVEM ser exibidos em reais, com duas casas decimais e
  formato brasileiro.

**Gráficos (RF04)**

- **FR-029**: O sistema DEVE exibir gráficos da distribuição de vínculos por regime e por
  secretaria.
- **FR-030**: O sistema DEVE exibir admissões e desligamentos por período, com escolha entre
  agrupamento mensal e anual, usando a data de admissão e a data de desligamento.
- **FR-031**: O sistema DEVE exibir os dez motivos de rescisão mais frequentes no período,
  agrupando os demais em "Outros" e os ausentes em "Não informado".
- **FR-032**: Os valores de cada gráfico DEVEM poder ser lidos também em números (rótulo ou
  tabela), e DEVEM ser iguais aos da consulta com os mesmos filtros.

**Exportação (RF05)**

- **FR-033**: O sistema DEVE exportar em CSV e em PDF o resultado das consultas e os
  indicadores, respeitando filtros e ordenação.
- **FR-034**: O CSV DEVE conter todos os registros do resultado, com cabeçalho, e abrir
  corretamente em planilhas no padrão brasileiro (acentos, separador e vírgula decimal).
- **FR-035**: O PDF DEVE conter título, filtros aplicados, data da última carga, data de
  geração, totais e a indicação da fonte oficial dos dados.
- **FR-036**: Quando o resultado passar de 5.000 linhas, a exportação em PDF DEVE ser
  recusada com orientação para usar o CSV ou refinar os filtros.

**Alertas (RF06)**

- **FR-037**: O sistema DEVE sinalizar vínculos ativos cujo salário base seja maior que o
  teto municipal (subsídio do prefeito). O teto DEVE ser obtido, a cada carga, do salário
  base do vínculo ativo de cargo "PREFEITO" publicado pela própria fonte (R$ 28.936,07 em
  13/10/2026). Um valor configurado manualmente, com data de início de vigência, DEVE
  prevalecer sobre o valor obtido da fonte quando existir. Se não houver nem vínculo de
  prefeito ativo nem valor configurado, a regra do teto DEVE ficar suspensa, com aviso
  visível na tela de alertas e no registro da carga. O vínculo do próprio prefeito nunca
  é sinalizado, porque seu salário é igual ao teto.
- **FR-038**: O sistema DEVE sinalizar vínculos ativos cujo salário base difira mais de 20%,
  para cima ou para baixo, da mediana do seu grupo. O grupo é formado pelos vínculos ativos
  com o mesmo cargo, o mesmo nível e as mesmas horas mensais. Diferença de exatamente 20%
  não gera alerta. O percentual DEVE ser configurável, sem alteração do sistema.
- **FR-039**: A regra de salário fora do padrão NÃO DEVE gerar alerta em grupos com menos de
  5 vínculos ativos, nem para vínculos sem nível informado ou com salário zero.
- **FR-040**: Cada alerta DEVE mostrar a regra que o gerou, o valor de referência (teto ou
  referência do grupo), o valor do vínculo e a diferença.
- **FR-041**: Os alertas DEVEM ser recalculados a cada carga bem-sucedida e DEVEM ser
  apresentados como sinalização para verificação, sem afirmar irregularidade.
- **FR-042**: O usuário DEVE poder listar os alertas, filtrá-los por regra e pelos mesmos
  filtros da consulta, e exportá-los.

**Requisitos não funcionais (RNF01 a RNF03)**

- **FR-043**: O sistema DEVE usar somente os dados públicos da fonte indicada e NÃO DEVE
  armazenar CPF nem dados pessoais sensíveis (RNF01).
- **FR-044**: Consultas, indicadores, gráficos e listagem de alertas DEVEM responder em até
  3 segundos (RNF02).
- **FR-045**: A interface DEVE ser responsiva e utilizável em computadores e dispositivos
  móveis, a partir de 360 pixels de largura (RNF03).
- **FR-046**: O acesso DEVE ser público, somente para leitura, sem cadastro ou login.

### Entidades Principais

- **Vínculo (servidor/matrícula)**: registro funcional identificado por entidade e
  matrícula. Atributos: nome do servidor, situação, datas de admissão e de desligamento, motivo do
  desligamento, regime, classificação, forma de investidura, secretaria, local de trabalho,
  cargo, função, nível, salário base, descrição do horário, horas mensais e semanais.
- **Entidade**: órgão a que o vínculo pertence (hoje, "MUNICÍPIO DE GRAVATAI").
- **Secretaria (centro de custo)**, **Local de trabalho**, **Cargo**, **Regime**,
  **Classificação**: valores de agrupamento e filtro vindos da fonte.
- **Carga**: uma execução de importação, com datas, período consultado, totais e situação.
- **Registro rejeitado**: registro que não passou no tratamento, com a carga e o motivo.
- **Mudança de vínculo**: alteração detectada entre duas cargas, com o atributo, o valor
  anterior, o novo valor e a data da detecção.
- **Parâmetro de alerta**: teto municipal com vigência e parâmetros da regra de salário fora
  do padrão.
- **Alerta**: sinalização de um vínculo por uma regra, com valor de referência e diferença.

## Critérios de Sucesso *(obrigatório)*

### Resultados Mensuráveis

- **SC-001**: 100% dos registros informados pela fonte no período configurado são
  importados ou contabilizados como rejeitados com motivo.
- **SC-002**: Zero ocorrências de CPF em qualquer dado armazenado, registro de execução ou
  arquivo exportado.
- **SC-003**: Zero divergências em uma amostra de 100 registros comparados com o Portal da
  Transparência de Gravataí.
- **SC-004**: 100% das consultas, indicadores e gráficos respondem em até 3 segundos com a
  base completa carregada.
- **SC-005**: 100% dos filtros previstos (8) funcionam isolados e combinados nos testes de
  aceitação.
- **SC-006**: Os totais de indicadores, gráficos e exportações coincidem com os da consulta
  equivalente em 100% dos casos testados.
- **SC-007**: As 2 regras de alerta são aprovadas em testes com casos conhecidos, sem falso
  alerta nos casos de fronteira definidos.
- **SC-008**: Um usuário sem treinamento responde "quantos vínculos ativos tem a secretaria
  X" em menos de 1 minuto, em computador e em celular.
- **SC-009**: Os dados publicados nunca têm mais de 48 horas de defasagem em relação à
  fonte, salvo indisponibilidade da fonte, que fica visível na data da última carga.
- **SC-010**: Todas as funcionalidades previstas são aceitas pelo patrocinador até
  22/10/2026.

## Premissas

- A fonte continua pública, sem autenticação, com o mesmo formato e os mesmos parâmetros
  (P01 e P02 da Declaração do Escopo).
- Os dados da Prefeitura são a fonte oficial e são considerados corretos (P03).
- Comportamento observado na fonte em 13/10/2026: 100 registros por página; limite de uma
  requisição por segundo; `tipoBusca=3` exige data inicial e final de desligamento no
  formato dia/mês/ano; erros de parâmetro chegam como mensagem dentro de uma resposta de
  sucesso; o CPF vem mascarado e o nome vem completo.
- A situação tem três valores na fonte: "Trabalhando", "Afastado" e "Demitido".
- A forma de investidura veio vazia em toda a amostra consultada; o campo é armazenado, mas
  não é filtro (não consta no RF02).
- "Servidor", nos requisitos, significa vínculo (matrícula). Não há deduplicação por pessoa:
  o nome não é usado para isso, porque homônimos existem.
- Aposentados, pensionistas e participantes de PDV entram nas contagens como vêm da fonte e
  podem ser separados pelo filtro de regime.
- "Secretaria" é o centro de custo como vem da fonte, sem agrupamento adicional. Um mesmo
  órgão pode aparecer em mais de um centro de custo (por exemplo, "SMED - ENSINO FUNDAMENTAL
  - FUNDEB" e "SMED - EDUCAÇÃO BÁSICA FUNDEB").
- O histórico começa na primeira carga do sistema; não há como reconstruir mudanças
  anteriores, porque a fonte só publica a situação atual.
- O teto municipal e os parâmetros de alerta são mantidos pela equipe técnica, sem tela de
  administração, para não ultrapassar os seis requisitos.
- Fora do escopo: aplicativo móvel nativo, dados de outras fontes, folha de pagamento
  detalhada, login e perfis de usuário, e outros municípios.

## Esclarecimentos

### Sessão de 2026-10-13

- P: Qual período de desligados importar? → R: todo o histórico disponível, desde 1900
  (FR-002).
- P: O nome do servidor é armazenado, exibido e pesquisável? → R: sim, os três (FR-019,
  FR-020, FR-021).
- P: Qual a regra de "salário fora do padrão"? → R: diferença maior que 20% em relação à
  mediana do grupo de mesmo cargo, nível e horas mensais (FR-038).
- P: Aposentados, pensionistas e PDV contam nos indicadores? → R: contam, separáveis pelo
  filtro de regime (FR-024).
- P: Soma e média salarial só de ativos? → R: sim (FR-026).
- P: Sincronização diária é suficiente? → R: sim (FR-012).
- P: O site é público, sem login? → R: sim (FR-046).
- P: Exibir "Demitido" como "Desligado"? → R: sim (FR-023).
- P: Qual o valor do teto municipal? → R: o salário base do cargo "PREFEITO" publicado pela
  própria fonte, R$ 28.936,07 em 13/10/2026, lido a cada carga (FR-037).

## Decisões para o Plano

Decisões de arquitetura tomadas pelo usuário em 2026-10-13. Elas não fazem parte das regras
de negócio e ficam registradas aqui para orientar o `/speckit-plan`.

- **Banco de dados**: PostgreSQL.
- **Hospedagem**: AWS, paga com crédito acadêmico de US$ 50 válido até o fim do semestre.
  Depois dessa data o ambiente deixa de ter custeio.
- **Repositórios**: separados. Este repositório contém o back-end e as especificações; o
  front-end fica em repositório próprio, mantido pela equipe de front-end. O contrato entre
  os dois é a documentação dos serviços de consulta.
- **PDF**: gerado no servidor.
- **Back-end**: Java 21.
- **Front-end**: React, com Recharts para os gráficos.
- **Histórico**: guarda apenas as mudanças detectadas entre cargas, e não uma cópia
  completa de cada carga.

## Dúvidas em Aberto

- **D4 (FR-037)**: O valor do teto foi resolvido pela própria fonte (R$ 28.936,07, cargo
  "PREFEITO", regime "ELETIVO", em 13/10/2026). Falta apenas o número da lei municipal que
  fixou o subsídio para a legislatura 2025/2028, para citar na tela de alertas e nos
  relatórios. O Projeto de Lei 12/2024 da Câmara de Gravataí trata do assunto; o número da
  lei sancionada não foi confirmado. Não bloqueia o plano.
