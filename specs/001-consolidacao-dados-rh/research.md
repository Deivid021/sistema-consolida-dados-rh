# Pesquisa (Fase 0): Consolidação de Dados de RH

**Data**: 2026-10-14 | **Plano**: [plan.md](plan.md) | **Especificação**: [spec.md](spec.md)

Cada item traz a decisão, o motivo e as alternativas descartadas. As decisões de banco,
hospedagem, repositórios, PDF e linguagem já vinham da especificação; aqui ficam os detalhes.

## 1. Comportamento da fonte de dados

Observado em consultas ao serviço público em 2026-10-13 e 2026-10-14.

- **Endereço**: `GET https://gravatai.atende.net/api/transparencia-pessoal-funcionarios`.
- **Parâmetros**: `tipoBusca` (1 = ativos, 2 = desligados, 3 = ambos), `pagina` (começa em
  1), `desligamentoInicio` e `desligamentoFinal` (obrigatórios para 2 e 3, formato
  `dd/MM/yyyy`). Também aceita `admissaoInicio` e `admissaoFinal`.
- **Paginação**: 100 registros por página; a resposta traz `paginaAtual` (texto),
  `totalPaginas` e `totalRegistros` (números).
- **Volume**: 8.831 ativos; `tipoBusca=3` de 01/01/1900 até hoje retorna 55.206 registros
  em 553 páginas. Conferido: `tipoBusca=3` = ativos + desligados do período
  (9.819 = 8.831 + 988 em 2026).
- **Limite de uso**: 1 requisição por segundo **e 1.000 requisições por dia** (descoberto em
  2026-10-15, na segunda carga real). Os dois excessos retornam HTTP 429 com
  `{"retorno":{"msg":...,"code":"000989"}}`; só o texto diferencia "por segundo" de "por dia".
  Uma carga completa usa cerca de 553 requisições, então cabe **uma carga completa por dia**.
  No limite diário o cliente interrompe a carga na hora, sem novas tentativas.
- **Erros disfarçados**: parâmetro inválido retorna HTTP 200 com `{"msg","sis","code"}` e
  sem `registros`.
- **Campos**: `entidade`, `matricula` (`"20-01"`), `nome`, `cpf` (mascarado,
  `***.628.140-**`), `situacao` (`Trabalhando`, `Afastado`, `Demitido`), `admissao`,
  `dataRescisao` e `motivoRescisao` (só em desligados; o motivo vem cortado em 40
  caracteres), `regime`, `classificacao`, `formaInvestidura` (sempre nula na amostra),
  `centroCusto`, `localTrabalho`, `cargo`, `funcao`, `nivel` (número ou texto, pode ser
  nulo), `salarioBase` (`"3.905,17"`), `horario` (texto livre), `horasMensais` e
  `horasSemanais` (`"180:00"`, `"036:00"`).
- **Teto**: o prefeito é um vínculo como os outros (cargo `PREFEITO`, regime `ELETIVO`,
  salário base R$ 28.936,07 em 2026-10-13).
- **Tempo de resposta**: entre 0,4 s e 17 s por página. Uma carga completa leva de 10 a 20
  minutos.

## 2. Estratégia de carga

- **Decisão**: carga completa a cada execução, gravada primeiro numa área de preparação
  (`vinculo_staging`). Só depois da conciliação os dados publicados são atualizados, numa
  única transação: gravar mudanças, atualizar vínculos, marcar ausentes, recalcular alertas
  e marcar a carga como bem-sucedida.
- **Motivo**: atende ao FR-003 (carga inconsistente não substitui os dados publicados) e ao
  FR-012 (dados anteriores continuam disponíveis durante a carga). Com 55 mil registros, a
  transação final leva segundos.
- **Conciliação**: `totalRegistros` é lido na primeira e na última página. A carga é
  inconsistente se os dois valores diferirem ou se matrículas distintas gravadas mais
  rejeitadas for diferente do total. Matrícula repetida entre páginas indica que os dados
  mudaram durante a leitura.
- **Alternativas descartadas**: carga incremental por data (a fonte não informa data de
  alteração); gravar direto na tabela publicada (violaria o FR-003).

## 3. Cliente HTTP, limite e novas tentativas

- **Decisão**: `RestClient` do Spring, com espera mínima de 1,1 s entre requisições. Até 5
  tentativas por página, com espera crescente (2, 4, 8, 16, 32 s) para HTTP 429, erro 5xx,
  tempo esgotado ou corpo sem `registros`. Esgotadas as tentativas, a carga termina como
  FALHA.
- **Motivo**: não exige biblioteca extra e respeita o limite da fonte (FR-004).
- **Alternativas descartadas**: Resilience4j (dependência a mais para um único cliente);
  requisições em paralelo (proibidas pelo limite de 1 por segundo).

## 4. Descarte do CPF

- **Decisão**: o objeto que recebe o JSON da fonte não tem campo `cpf` e ignora campos
  desconhecidos. O CPF nunca chega a existir como dado dentro do sistema. O corpo bruto da
  resposta nunca é registrado em log. Um teste automatizado confere que nenhuma tabela tem
  coluna de CPF e que nenhum valor gravado bate com o padrão de CPF.
- **Motivo**: princípio I da constituição. Descartar na desserialização é mais seguro que
  apagar depois.
- **Alternativas descartadas**: gravar e apagar depois, ou guardar um hash (proibido pela
  constituição).

## 5. Conversão de valores

- **Decisão**:
  - **Datas**: `dd/MM/yyyy` vira `LocalDate`, com validação estrita.
  - **Salário**: remove pontos, troca a vírgula por ponto e vira `BigDecimal` com 2 casas.
  - **Horas**: `"HHH:MM"` vira total de minutos (`int`).
  - **Textos**: espaços nas pontas são removidos; texto vazio vira nulo.
  - **Valores originais**: os textos recebidos dos campos convertidos ficam numa coluna
    `jsonb` (`valores_originais`), conforme o FR-006.
- **Rejeição**: matrícula ausente, ou data, valor ou hora que não converte. Gera um
  `registro_rejeitado` com página, matrícula (se houver), campo e motivo (FR-007).

## 6. Banco de dados e busca sem acento

- **Decisão**: PostgreSQL 17, com migrações versionadas pelo Flyway. A busca usa uma coluna
  `texto_busca` com nome, cargo e matrícula em minúsculas e sem acento, calculada no Java
  (`java.text.Normalizer`), e um índice trigram (`pg_trgm`) para `LIKE '%texto%'`.
- **Motivo**: normalizar no Java evita depender da extensão `unaccent` nos índices. Com 55
  mil linhas, consultas e agregações levam bem menos de 1 segundo (RNF02).
- **Agregações**: calculadas na hora com `GROUP BY` e `percentile_cont(0.5)` para a
  mediana. Não há tabelas pré-calculadas, exceto a de alertas.
- **Alternativas descartadas**: mecanismo de busca separado (exagero para 55 mil
  registros); visões materializadas (complexidade sem ganho real nesse volume).

## 7. Agendamento

- **Decisão**: `@Scheduled` diário às 02:00 (`America/Sao_Paulo`). Uma única carga por vez,
  garantida por índice único parcial em `carga` com status `EM_ANDAMENTO`. Na
  inicialização, cargas que ficaram `EM_ANDAMENTO` são marcadas como `FALHA` com o motivo
  "interrompida".
- **Disparo manual**: `POST /api/v1/admin/cargas`, protegido pelo cabeçalho
  `X-Admin-Token`, cujo valor vem de variável de ambiente. É necessário para a carga
  inicial e para demonstrações.
- **Alternativas descartadas**: Quartz ou ShedLock (só existe uma instância); cron do
  sistema operacional (fica fora do código e dos testes).

## 8. Regras de alerta

- **Teto**: vale o valor manual em `parametro_teto` com a maior `vigencia_inicio` que não
  seja posterior à data da carga. Sem valor manual, vale o salário base do vínculo ativo
  com cargo `PREFEITO`. Sem nenhum dos dois, a regra fica suspensa. São sinalizados os
  vínculos ativos com salário maior que o teto, exceto o vínculo usado como fonte do teto.
  O valor e a origem do teto ficam gravados na carga.
- **Fora do padrão**: o grupo reúne os vínculos ativos com mesmo cargo, nível e horas
  mensais, salário maior que zero e nível informado. O grupo precisa ter pelo menos 5
  vínculos. A referência é a mediana do grupo. É sinalizado o vínculo com
  `|salário − mediana| / mediana > 0,20`. Percentual e tamanho mínimo são configuráveis
  (`alertas.fora-padrao.percentual`, `alertas.fora-padrao.minimo-grupo`).
- **Cálculo**: refeito a cada carga bem-sucedida, dentro da mesma transação, gravando a
  tabela `alerta`.

## 9. Exportação

- **CSV**: gerado em fluxo contínuo (sem carregar tudo na memória), UTF-8 com BOM, separador
  `;`, vírgula decimal e datas `dd/MM/yyyy`. É o formato que o Excel em português abre
  direto.
- **PDF**: OpenPDF 3.0.x (código aberto, LGPL/MPL), em A4 paisagem, com título, filtros,
  data da última carga, data de geração, totais e a fonte. Acima de 5.000 linhas, a
  geração é recusada com HTTP 422 (FR-036).
- **Alternativas descartadas**: JasperReports (pesado e com curva de aprendizado); gerar o
  PDF no navegador (decidido na especificação).

## 10. Versões

Versões estáveis conferidas no Maven Central em 2026-10-14:

| Item | Versão | Observação |
|------|--------|------------|
| Java | 21 | já instalado (21.0.12) |
| Spring Boot | 4.1.1 | gerencia as versões de Flyway, driver PostgreSQL, Jackson e Testcontainers |
| Maven | 3.9.x | já instalado (3.9.12) |
| PostgreSQL | 17 | imagem Docker oficial |
| OpenPDF | 3.0.5 | |
| springdoc-openapi | 3.1.x | publica o contrato e a tela do Swagger para a equipe de front |
| Testcontainers | gerenciado pelo Boot | exige Docker, que já está instalado |

Front-end (pasta `frontend/` do mesmo repositório): React com Vite e TypeScript, e Recharts
para os gráficos.

## 11. Hospedagem na AWS (crédito de US$ 50)

- **Decisão**: uma instância EC2 `t4g.small` (2 GB de memória, ARM) rodando Docker Compose
  com a aplicação e o PostgreSQL. O build do front-end vai dentro do jar do Spring Boot
  (um contêiner serve o site e a API), e uma distribuição CloudFront na frente da EC2 dá o
  endereço HTTPS. Revisado em 18/10/2026: antes o front ficava num bucket S3 atrás do mesmo
  CloudFront; com front e back no mesmo repositório, o S3 virou só mais uma peça para
  configurar e atualizar.
- **Motivo**:
  - Mesmo domínio e HTTPS sem comprar domínio nem configurar certificado, e sem CORS.
  - Evita o bloqueio de conteúdo misto (página HTTPS chamando API HTTP).
  - Custo estimado de US$ 15 a 20 por mês: instância, disco de 20 GB e IPv4 público. Cabe
    no crédito até o fim do semestre. Os valores são estimativa e devem ser conferidos na
    calculadora da AWS.
- **Controle de gasto**: alerta no AWS Budgets em US$ 35 e US$ 45.
- **Alternativas descartadas**:
  - RDS: dobraria o custo.
  - `t4g.micro`: 1 GB é pouco para Spring Boot e PostgreSQL juntos.
  - Elastic Beanstalk e ECS: mais configuração para uma única instância.

## 12. Testes

- **Unidade (JUnit 5)**: conversões, regras de alerta, montagem de filtros.
- **Integração (Testcontainers com PostgreSQL)**: carga completa contra a fonte simulada,
  consultas, indicadores e exportações.
- **Fonte simulada**: `MockRestServiceServer` com páginas JSON sintéticas. Nenhum nome ou CPF
  real entra nos arquivos de teste.
- **Validação por amostragem (SC-003)**: roteiro manual descrito em
  [quickstart.md](quickstart.md).
