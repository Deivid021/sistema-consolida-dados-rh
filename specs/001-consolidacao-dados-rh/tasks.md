---

description: "Lista de tarefas da funcionalidade 001: consolidação e consulta dos dados de RH de Gravataí/RS"
---

# Tarefas: Consolidação e Consulta dos Dados de RH da Prefeitura de Gravataí/RS

**Entrada**: documentos de projeto em `specs/001-consolidacao-dados-rh/`

**Pré-requisitos**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/api-consulta.yaml](contracts/api-consulta.yaml), [quickstart.md](quickstart.md)

**Testes**: incluídos. O princípio V da constituição exige testes automatizados para regras de
negócio, cálculos, alertas, integração com a fonte e serviços de consulta. Não é TDD
obrigatório: escreva o teste junto com a implementação, antes de marcar a tarefa como feita.

**Organização**: tarefas agrupadas por história de usuário, na ordem de entrega do plano. Cada
tarefa indica o requisito (RF/RNF) que atende.

**Nota de implementação (2026-10-15)**: o acesso ao banco usa SQL direto (`JdbcClient`) em vez
de JPA. O mesmo filtro (`FiltroVinculos.paraSql`) serve à consulta, às agregações e à exportação,
e a publicação da carga é feita em SQL em lote. Por isso `Vinculo.java`, `VinculoRepository.java`
e `EspecificacaoVinculos.java` (T011, T013) não existem; a lógica está em `FiltroVinculos`,
`ConsultaVinculoService` e `CargaRepository`. As entidades das tarefas T012, T024 e T048 viram
records e repositórios JDBC.

**Nota de estrutura (2026-10-18)**: depois da implementação, o código foi reorganizado em
camadas clássicas (`controller/`, `service/`, `repository/`, `model/`, `dto/`, `client/`, `config/`,
`exception/`, `util/`). Os caminhos citados nas tarefas abaixo são os originais; o
[plan.md](plan.md) traz a estrutura atual. Renomeações principais: `ExecutorCarga` →
`CargaService`, `PublicadorCarga` → `PublicacaoCargaService`, `TratadorRegistro` →
`TratamentoRegistroService`, `CalculadorAlertas` → `CalculoAlertasService`, `QuadroService` e
`SalarioService` → `IndicadorService`, `ResolvedorTeto` e `TetoVigente` → `TetoService`.

## Formato: `[ID] [P?] [História] Descrição`

- **[P]**: pode rodar em paralelo (arquivos diferentes, sem dependência de tarefa pendente)
- **[US1]** a **[US6]**: história de usuário da especificação
- Caminhos relativos à raiz do repositório

## Convenções de caminho

- Código: `src/main/java/br/edu/unidavi/rhgravatai/`
- Testes: `src/test/java/br/edu/unidavi/rhgravatai/`
- Recursos: `src/main/resources/` e `src/test/resources/`
- Projeto Maven único na raiz; o front-end fica em outro repositório

---

## Fase 1: Preparação (infraestrutura compartilhada)

**Objetivo**: criar o projeto e o ambiente local.

- [X] T001 Criar `pom.xml`:
  - **Base**: parent `spring-boot-starter-parent` 4.1.1, `java.version` 21, groupId `br.edu.unidavi`, artifactId `rh-gravatai`.
  - **Dependências**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `flyway-core`, `flyway-database-postgresql`, `postgresql` (runtime), `com.github.librepdf:openpdf` 3.0.5 e `org.springdoc:springdoc-openapi-starter-webmvc-ui` 3.1.x.
  - **Testes**: `spring-boot-starter-test`, `spring-boot-testcontainers` e `org.testcontainers:postgresql`.
  - **Versões**: as demais são gerenciadas pelo Boot.
- [X] T002 Criar `src/main/java/br/edu/unidavi/rhgravatai/RhGravataiApplication.java`, com `@SpringBootApplication`, `@EnableScheduling` e `@EnableAsync`.
- [X] T003 Criar `src/main/resources/application.yml`:
  - **Banco**: datasource lido de `DB_URL`, `DB_USUARIO` e `DB_SENHA`, com padrão para `jdbc:postgresql://localhost:5432/rh`.
  - **JPA**: `spring.jpa.hibernate.ddl-auto=validate` e `spring.jpa.open-in-view=false`.
  - **Fuso**: `spring.jackson.time-zone=America/Sao_Paulo`.
  - **Fonte**: `fonte.url-base=https://gravatai.atende.net/api/transparencia-pessoal-funcionarios`, `fonte.intervalo-minimo-ms=1100`, `fonte.timeout-s=90`, `fonte.max-tentativas=5`.
  - **Carga**: `importacao.cron=0 0 2 * * *`.
  - **Alertas**: `alertas.fora-padrao.percentual=20` e `alertas.fora-padrao.minimo-grupo=5`.
  - **Exportação**: `exportacao.pdf.max-linhas=5000`.
  - **Administração**: `admin.token` lido de `ADMIN_TOKEN`, sem padrão.
  - **CORS**: `cors.origens` lido de `CORS_ORIGENS`, vazio por padrão.
- [X] T004 [P] Criar `compose.yaml`:
  - **Serviço `banco`**: `postgres:17`, banco `rh`, volume nomeado e porta 5432.
  - **Serviço `app`**: build do `Dockerfile`, variáveis do T003, `restart: unless-stopped` e healthcheck em `/actuator/health`.
- [X] T005 [P] Criar `Dockerfile` multi-estágio:
  - **Build**: `maven:3.9-eclipse-temurin-21`.
  - **Execução**: `eclipse-temurin:21-jre`.
  - **Compatibilidade**: imagens com suporte a arm64, para a EC2 `t4g`.
- [X] T006 [P] Criar `.gitignore` (`target/`, `.env`, `.idea/`, `*.iml`, `.vscode/`) e `.editorconfig` (UTF-8, LF, indentação de 4 espaços em Java e 2 em YAML).

---

## Fase 2: Fundação (bloqueia todas as histórias)

**Objetivo**: esquema do banco, entidades centrais, filtros comuns e tratamento de erros.

**⚠️ CRÍTICO**: nenhuma história começa antes desta fase terminar.

- [X] T007 Criar `src/main/resources/db/migration/V1__esquema_inicial.sql` com todas as tabelas do [data-model.md](data-model.md):
  - **Extensão**: `CREATE EXTENSION IF NOT EXISTS pg_trgm`.
  - **`carga`**:
    - `status` com CHECK em (`EM_ANDAMENTO`, `SUCESSO`, `FALHA`, `INCONSISTENTE`) e `origem` com CHECK em (`AGENDADA`, `MANUAL`);
    - `teto_origem` com CHECK em (`FONTE`, `MANUAL`, `SUSPENSA`);
    - índice único parcial `ON carga (status) WHERE status = 'EM_ANDAMENTO'`.
  - **`vinculo`**:
    - único `(entidade, matricula)`;
    - `situacao` com CHECK em (`TRABALHANDO`, `AFASTADO`, `DESLIGADO`);
    - `salario_base numeric(12,2)` e `valores_originais jsonb`;
    - `ativo_na_fonte boolean NOT NULL DEFAULT true`;
    - índice GIN `gin_trgm_ops` em `texto_busca`;
    - índices em `situacao`, `regime`, `secretaria`, `cargo`, `data_admissao` e `data_desligamento`.
  - **`vinculo_staging`**: mesmas colunas de negócio de `vinculo`, mais `carga_id` e `pagina`.
  - **`registro_rejeitado`**, **`mudanca_vinculo`**: como no modelo.
  - **`parametro_teto`**: `valor numeric(12,2) NOT NULL CHECK (valor > 0)` e `vigencia_inicio date NOT NULL UNIQUE`.
  - **`alerta`**: `regra` com CHECK em (`TETO`, `FORA_PADRAO`) e único `(vinculo_id, regra)`.
  - **CPF**: nenhuma coluna de CPF em nenhuma tabela.
- [X] T008 [P] Criar a base de testes de integração `src/test/java/br/edu/unidavi/rhgravatai/TesteIntegracaoBase.java`: `@SpringBootTest` com um contêiner `PostgreSQLContainer("postgres:17")` anotado com `@ServiceConnection`, compartilhado entre as classes.
- [X] T009 [P] Criar `src/main/java/br/edu/unidavi/rhgravatai/comum/NormalizadorTexto.java`:
  - remove espaços nas pontas, converte para minúsculas e remove acentos (`Normalizer.Form.NFD` mais remoção de marcas);
  - devolve nulo para texto vazio.
  - Teste em `src/test/java/br/edu/unidavi/rhgravatai/comum/NormalizadorTextoTest.java` (RF02, FR-019).
- [X] T010 [P] Criar `src/main/java/br/edu/unidavi/rhgravatai/comum/erro/TratadorErros.java` (`@RestControllerAdvice`), respondendo `ProblemDetail` (application/problem+json) com `detail` em português:
  - **400**: `FiltroInvalidoException` e erros de conversão de parâmetro;
  - **409**: `CargaEmAndamentoException`;
  - **422**: `ExportacaoMuitoGrandeException`.
- [X] T011 Criar `src/main/java/br/edu/unidavi/rhgravatai/vinculo/Situacao.java` (enum `TRABALHANDO`, `AFASTADO`, `DESLIGADO`, com conversão de "Trabalhando", "Afastado" e "Demitido"; FR-023), `Vinculo.java` (entidade JPA da tabela `vinculo`) e `VinculoRepository.java` (`JpaRepository` + `JpaSpecificationExecutor`).
- [X] T012 [P] Criar em `src/main/java/br/edu/unidavi/rhgravatai/importacao/carga/`: `Carga.java` (entidade JPA), `StatusCarga.java`, `OrigemCarga.java`, `OrigemTeto.java` e `CargaRepository.java`. O repositório deve ter `findTopByStatusOrderByFimDesc(SUCESSO)` e `existsByStatus(EM_ANDAMENTO)`.
- [X] T013 Criar `src/main/java/br/edu/unidavi/rhgravatai/comum/filtro/FiltroVinculos.java` (record com `q`, `situacao`, `regime`, `classificacao`, `secretaria`, `localTrabalho` e `cargo` como listas, e os períodos de admissão e desligamento) e `EspecificacaoVinculos.java` (`Specification<Vinculo>`):
  - **Combinação**: filtros diferentes com E; valores do mesmo filtro com OU (FR-015).
  - **Não informado**: o valor `NAO_INFORMADO` vira `IS NULL`.
  - **Períodos**: incluem as duas datas; aceitam só uma das datas. Data inicial maior que a final lança `FiltroInvalidoException` (FR-017).
  - **Desligamento**: período informado força `situacao = DESLIGADO` (FR-018).
  - **Busca**: `q` com no mínimo 2 caracteres, normalizado pelo `NormalizadorTexto` e aplicado com `texto_busca LIKE %q%` (FR-019).
  - Teste unitário em `src/test/java/br/edu/unidavi/rhgravatai/comum/filtro/FiltroVinculosTest.java` (RF02).
- [X] T014 [P] Criar `src/main/java/br/edu/unidavi/rhgravatai/comum/UltimaAtualizacao.java`, serviço que devolve o `fim` da última carga com `SUCESSO`, ou nulo (FR-013).
- [X] T015 [P] Criar `src/main/java/br/edu/unidavi/rhgravatai/comum/ConfiguracaoWeb.java`:
  - CORS só para as origens de `cors.origens` (desenvolvimento do front);
  - título do springdoc "API de Consulta - Dados de RH de Gravataí/RS";
  - Swagger em `/swagger-ui.html`.
- [X] T016 [P] Criar `src/test/java/br/edu/unidavi/rhgravatai/privacidade/EsquemaSemCpfTest.java`: depois das migrações, `information_schema.columns` não pode ter coluna cujo nome contenha `cpf` (RNF01, princípio I).

**Ponto de controle**: `mvn verify` passa e a aplicação sobe contra o PostgreSQL do `compose.yaml`.

---

## Fase 3: História 1 - Importar e manter os dados de pessoal (P1) 🎯 MVP

**Meta**: carga completa, diária e manual, conciliada, sem CPF e com histórico de mudanças (RF01, RNF01).

**Teste independente**: rodar `POST /api/v1/admin/cargas`. A carga deve terminar em `SUCESSO`,
com `gravados + rejeitados = totalInformado` e nenhum CPF no banco ([quickstart.md](quickstart.md) §3).

### Testes da História 1

- [X] T017 [P] [US1] Criar páginas sintéticas da fonte em `src/test/resources/fonte/`. Use só nomes fictícios e CPFs fictícios mascarados (`***.000.000-**`).
  - **Páginas válidas**: `pagina-1.json` e `pagina-2.json`, com `totalPaginas` 2 e `totalRegistros` 5. Incluem um "Demitido" com `dataRescisao` e `motivoRescisao`, um registro com `salarioBase` "0,00", um com `nivel` nulo e um com `salarioBase` inválido "abc".
  - **Erros**: `erro-parametro.json` (`{"msg":...,"sis":"WFP","code":"001772"}`) e `limite-429.json` (`{"retorno":{"msg":...,"code":"000989"}}`).
  - **Total que muda**: `pagina-2-total-alterado.json`, com `totalRegistros` 6.
- [X] T018 [P] [US1] Criar `src/test/java/br/edu/unidavi/rhgravatai/importacao/tratamento/TratadorRegistroTest.java` (RF01, FR-006, FR-007):
  - **Conversões**: "3.905,17" → 3905.17; "29/01/1979" → data; "180:00" → 10800 min; "036:00" → 2160 min.
  - **Situação**: "Demitido" → `DESLIGADO`.
  - **Textos**: vazio e só espaços viram nulo.
  - **Originais**: `valores_originais` guarda os textos recebidos.
  - **Rejeições** (com campo e motivo): matrícula ausente, data "31/02/2026", salário "abc", hora "18h", situação desconhecida.
- [X] T019 [P] [US1] Criar `src/test/java/br/edu/unidavi/rhgravatai/importacao/fonte/ClienteFonteTest.java`, com `MockRestServiceServer` e um `Esperador` falso, sem esperar de verdade (RF01, FR-004):
  - **Parâmetros**: `tipoBusca=3`, `desligamentoInicio=01/01/1900`, `desligamentoFinal` igual à data atual em `dd/MM/yyyy`, e `pagina`.
  - **Novas tentativas**: HTTP 429 seguido de 200 resulta em sucesso; erro em HTTP 200 conta como falha e nova tentativa; 5 falhas seguidas lançam exceção.
  - **Intervalo**: espera mínima de 1100 ms entre requisições.
- [X] T020 [US1] Criar `src/test/java/br/edu/unidavi/rhgravatai/importacao/carga/CargaIntegracaoTest.java`, herdando `TesteIntegracaoBase` e usando as páginas do T017 (RF01, FR-003, FR-009 a FR-011). Cenários:
  - **(a) Carga completa**: `SUCESSO`, com 4 gravados e 1 rejeitado conciliados.
  - **(b) Total alterado**: `INCONSISTENTE`, sem alterar a tabela `vinculo`.
  - **(c) Falha da fonte**: `FALHA`, com os dados anteriores intactos.
  - **(d) Segunda carga com mudanças**: salário e situação alterados geram linhas em `mudanca_vinculo`; um vínculo ausente fica com `ativo_na_fonte=false`.
  - **(e) CPF**: nenhuma coluna de texto de nenhuma tabela contém valor que case com `\d{3}\.\d{3}\.\d{3}-\d{2}` ou `\*\*\*\.`.
  - **(f) Concorrência**: segunda carga simultânea recebe 409.

### Implementação da História 1

- [X] T021 [P] [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/fonte/RegistroFonte.java`, record com `@JsonIgnoreProperties(ignoreUnknown = true)` e **sem campo `cpf`**. Campos: `entidade`, `matricula`, `nome`, `situacao`, `admissao`, `dataRescisao`, `motivoRescisao`, `regime`, `classificacao`, `formaInvestidura`, `centroCusto`, `localTrabalho`, `cargo`, `funcao`, `nivel`, `salarioBase`, `horario`, `horasMensais`, `horasSemanais`, todos `String`. Criar também `PaginaFonte.java` (`registros`, `paginaAtual` String, `totalPaginas` int, `totalRegistros` int) (RF01, RNF01).
- [X] T022 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/fonte/ClienteFonte.java` e `Esperador.java` (interface com implementação real por `Thread.sleep`), com `RestClient` (RF01, FR-004):
  - **Parâmetros**: os do T019.
  - **Timeout**: `fonte.timeout-s`.
  - **Erro**: resposta sem `registros` conta como erro.
  - **Novas tentativas**: em 429, 5xx, timeout ou erro, até `fonte.max-tentativas`, com esperas de 2, 4, 8, 16 e 32 s; esgotadas, lança `FonteIndisponivelException`.
  - **Intervalo**: respeita `fonte.intervalo-minimo-ms` entre requisições.
  - **Logs**: NUNCA registrar o corpo da resposta; registrar só página, status HTTP e código de erro.
- [X] T023 [P] [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/tratamento/TratadorRegistro.java`, `RegistroTratado.java` e `Rejeicao.java`, aplicando as regras da tabela "Regras de validação e conversão" do [data-model.md](data-model.md) (RF01, FR-006, FR-007):
  - `matricula` não vazio;
  - `situacao` em `Trabalhando|Afastado|Demitido`;
  - datas `dd/MM/yyyy` com `ResolverStyle.STRICT`;
  - `salarioBase` casando com `^\d{1,3}(\.\d{3})*,\d{2}$`;
  - horas casando com `^\d+:\d{2}$`;
  - `texto_busca` calculado com o `NormalizadorTexto` sobre nome, cargo e matrícula.
- [X] T024 [P] [US1] Criar `VinculoStaging.java`, `RegistroRejeitado.java` e `MudancaVinculo.java`, com seus repositórios, em `src/main/java/br/edu/unidavi/rhgravatai/importacao/carga/` (RF01). `RegistroRejeitado.valorRecebido` guarda só o valor do campo com problema, nunca o registro inteiro.
- [X] T025 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/carga/ExecutorCarga.java` (RF01, FR-003, FR-009). Passos:
  1. Lançar `CargaEmAndamentoException` se já houver carga `EM_ANDAMENTO`.
  2. Criar a carga `EM_ANDAMENTO` com a origem e o período `01/01/1900` até hoje.
  3. Limpar `vinculo_staging`.
  4. Ler página por página. Para cada página: tratar os registros; gravar os válidos em lote no staging e os rejeitados em `registro_rejeitado`; atualizar `paginas_lidas`.
  5. Guardar `totalRegistros` da primeira página em `total_informado` e da última em `total_informado_final`.
  6. Conciliar. A carga fica `INCONSISTENTE`, com mensagem, se os dois totais diferirem ou se matrículas distintas no staging mais rejeitados for diferente de `total_informado`.
  7. Se `FonteIndisponivelException` ocorrer em qualquer passo, a carga fica `FALHA`, com a mensagem.
  8. Com a conciliação correta, chamar o `PublicadorCarga`.
- [X] T026 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/carga/PublicadorCarga.java`, com uma única `@Transactional`, usando SQL em lote (`INSERT ... ON CONFLICT (entidade, matricula) DO UPDATE`) (RF01, FR-008, FR-010, FR-011). Passos:
  1. **Mudanças**: comparar staging com `vinculo` nos atributos `situacao`, `regime`, `secretaria`, `local_trabalho`, `cargo`, `funcao`, `nivel`, `salario_base`, `horas_mensais_min`, `horas_semanais_min` e `data_desligamento`, e gravar `mudanca_vinculo`. Não gravar na primeira carga, quando `vinculo` está vazia.
  2. **Atualização**: inserir e atualizar `vinculo`, preenchendo `ultima_carga_id` e `visto_por_ultimo_em`, e `primeira_carga_id` nos novos.
  3. **Ausentes**: marcar `ativo_na_fonte=false` em quem não veio, registrando mudança em `ativo_na_fonte`; quem voltou recebe `true`.
  4. **Contadores**: preencher `novos`, `alterados` e `nao_encontrados`.
  5. **Pós-publicação**: chamar todos os beans `AposPublicacao` (interface criada aqui; os alertas da US5 a implementam).
  6. **Encerramento**: marcar a carga `SUCESSO` com `fim`.
- [X] T027 [P] [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/carga/RecuperadorCargasInterrompidas.java`: no `ApplicationReadyEvent`, marcar cargas `EM_ANDAMENTO` como `FALHA`, com a mensagem "interrompida" (RF01).
- [X] T028 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/agendamento/AgendadorCarga.java`, com `@Scheduled(cron = "${importacao.cron}", zone = "America/Sao_Paulo")` e origem `AGENDADA` (RF01, FR-012).
- [X] T029 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/importacao/agendamento/AdminCargaController.java` (RF01):
  - **`POST /api/v1/admin/cargas`**: inicia a carga com `@Async` e origem `MANUAL`; responde 202 com a `Carga`, ou 409.
  - **`GET /api/v1/admin/cargas`**: paginado, da mais recente para a mais antiga.
  - **Segurança**: cabeçalho `X-Admin-Token` comparado com `MessageDigest.isEqual` a `admin.token`. Responde 401 se o token estiver ausente ou errado, e 401 sempre que `admin.token` não estiver configurado.
  - **Esquemas**: `Carga` do contrato.
- [X] T030 [US1] Criar `src/main/java/br/edu/unidavi/rhgravatai/status/StatusController.java` e `StatusService.java`, com `GET /api/v1/status` conforme o esquema `Status` do contrato (FR-013):
  - `ultimaAtualizacao`, `totalVinculos`, `totalAtivos` (situação `TRABALHANDO` ou `AFASTADO` e `ativo_na_fonte`) e `fonte`;
  - `teto` lido de `teto_valor` e `teto_origem` da última carga com sucesso; `SUSPENSA` enquanto a US5 não existir.
- [X] T031 [US1] Registrar logs da carga em `ExecutorCarga` e `PublicadorCarga`: início, progresso a cada 50 páginas e fim com totais e status. Nunca registrar conteúdo de registros (RNF01).

**Ponto de controle (meta 16/10)**: carga real concluída localmente ([quickstart.md](quickstart.md) §3) e `CargaIntegracaoTest` passando.

---

## Fase 4: História 2 - Consultar servidores com busca e filtros (P1) 🎯 MVP

**Meta**: `/vinculos` e `/filtros/opcoes` conforme o contrato (RF02, RNF02).

**Teste independente**: com os dados de exemplo, cada filtro isolado e combinado retorna o
esperado em menos de 3 s ([quickstart.md](quickstart.md) §4).

### Testes da História 2

- [X] T032 [P] [US2] Criar `src/test/resources/dados/vinculos-exemplo.sql`, com cerca de 30 vínculos fictícios cobrindo: as 3 situações, 3 regimes, 3 secretarias, nomes e cargos com acento, campos nulos, desligados com datas e uma carga `SUCESSO`. Será reutilizado pelas US2 a US6.
- [X] T033 [US2] Criar `src/test/java/br/edu/unidavi/rhgravatai/vinculo/ConsultaVinculoIntegracaoTest.java` (RF02, FR-014 a FR-022):
  - **Filtros**: os 8 filtros isolados; combinação E entre filtros e OU dentro do mesmo; `NAO_INFORMADO`.
  - **Busca**: `q=jose` encontra "JOSÉ".
  - **Períodos**: filtro de desligamento retorna só `DESLIGADO`; período invertido responde 400 com `detail` em português.
  - **Paginação e ordenação**: `tamanho` acima de 100 responde 400; ordenação por `salarioBase,desc`; campo de ordenação fora da lista responde 400.
  - **Respostas**: `total` correto; `ultimaAtualizacao` presente; `/filtros/opcoes` com quantidades e a opção `valor: null`.

### Implementação da História 2

- [X] T034 [P] [US2] Criar `src/main/java/br/edu/unidavi/rhgravatai/vinculo/VinculoResposta.java` (record conforme o esquema `Vinculo` do contrato; `horasMensais = horas_mensais_min / 60.0`) e `src/main/java/br/edu/unidavi/rhgravatai/comum/PaginaResposta.java` (genérico, com `itens`, `total`, `pagina`, `tamanho` e `ultimaAtualizacao`).
- [X] T035 [US2] Criar `src/main/java/br/edu/unidavi/rhgravatai/vinculo/ConsultaVinculoService.java` (RF02):
  - aplica a `EspecificacaoVinculos`;
  - ordenação só pelos campos do padrão do contrato (`nome|matricula|situacao|regime|secretaria|cargo|nivel|salarioBase|dataAdmissao|dataDesligamento`), com padrão `nome,asc`;
  - `tamanho` entre 1 e 100, padrão 20;
  - `pagina` começando em 0.
- [X] T036 [US2] Criar `src/main/java/br/edu/unidavi/rhgravatai/vinculo/VinculoController.java` com `GET /api/v1/vinculos`, aceitando parâmetros repetidos para as listas e convertendo para `FiltroVinculos` (RF02, FR-014, FR-022).
- [X] T037 [US2] Criar `src/main/java/br/edu/unidavi/rhgravatai/vinculo/OpcoesFiltroService.java` e adicionar `GET /api/v1/filtros/opcoes` ao `VinculoController`: `GROUP BY` em `situacao`, `regime`, `classificacao`, `secretaria`, `local_trabalho` e `cargo`, incluindo os nulos, ordenado por valor (RF02, FR-016).

**Ponto de controle (meta 16/10)**: MVP pronto (importação e consulta).

---

## Fase 5: História 3 - Ver indicadores de RH (P2)

**Meta**: `/indicadores/quadro` e `/indicadores/salarios` (RF03).

**Teste independente**: somas por agrupamento iguais ao total, e salários conferidos à mão
([quickstart.md](quickstart.md) §5).

- [X] T038 [P] [US3] Criar `src/test/java/br/edu/unidavi/rhgravatai/indicador/IndicadorIntegracaoTest.java` (RF03, FR-024 a FR-028):
  - **Quadro**: a soma de `porSituacao`, de `porRegime` e de `porSecretaria` é igual a `total`.
  - **Salários**: por padrão só ativos; `situacao` explícita prevalece; salário zero ou nulo fica fora de soma e média e entra em `quantidadeSemSalario`; média com 2 casas (`HALF_UP`).
  - **Filtros**: respeitados nos dois endpoints.
- [X] T039 [P] [US3] Criar `src/main/java/br/edu/unidavi/rhgravatai/indicador/QuadroService.java` e `QuadroResposta.java`: três `GROUP BY` (`situacao`, `regime`, `secretaria`) com o mesmo filtro, mais o `total` (RF03, FR-024, FR-027, FR-029).
- [X] T040 [P] [US3] Criar `src/main/java/br/edu/unidavi/rhgravatai/indicador/SalarioService.java` e `GrupoSalarialResposta.java` (RF03, FR-025, FR-026):
  - agrupa por `cargo` e `nivel`;
  - `quantidade` = vínculos com salário maior que 0; `quantidadeSemSalario` = os demais;
  - `soma` e `media` só sobre salário maior que 0;
  - sem filtro `situacao`, considera `TRABALHANDO`/`AFASTADO` e `ativo_na_fonte`;
  - ordenação `cargo|nivel|quantidade|soma|media`, padrão `soma,desc`;
  - paginado.
- [X] T041 [US3] Criar `src/main/java/br/edu/unidavi/rhgravatai/indicador/IndicadorController.java` com `GET /api/v1/indicadores/quadro` e `GET /api/v1/indicadores/salarios` (RF03).

**Ponto de controle (meta 17/10)**

---

## Fase 6: História 4 - Ver gráficos de distribuição e movimentação (P2)

**Meta**: `/graficos/movimentacao` e `/graficos/motivos-rescisao`. A distribuição por regime e
secretaria reaproveita o `/indicadores/quadro` (RF04).

**Teste independente**: valores dos gráficos iguais às contagens da consulta com os mesmos filtros.

- [X] T042 [P] [US4] Criar `src/test/java/br/edu/unidavi/rhgravatai/grafico/GraficoIntegracaoTest.java` (RF04, FR-030 a FR-032):
  - **Movimentação MES**: inclui meses sem movimento com zero; admissões contadas por `data_admissao` e desligamentos por `data_desligamento`.
  - **Movimentação ANO**: agrupamento anual.
  - **Motivos**: no máximo 10, mais "Outros" e "Não informado".
  - **Validação**: `inicio` maior que `fim` responde 400; parâmetro obrigatório ausente responde 400.
- [X] T043 [P] [US4] Criar `src/main/java/br/edu/unidavi/rhgravatai/grafico/MovimentacaoService.java`: `generate_series` dos períodos entre `inicio` e `fim`, com granularidade `MES` (`yyyy-MM`) ou `ANO` (`yyyy`) e filtros `regime`, `secretaria` e `cargo` (RF04, FR-030).
- [X] T044 [P] [US4] Criar `src/main/java/br/edu/unidavi/rhgravatai/grafico/MotivosRescisaoService.java`: desligados no período, agrupados por `motivo_desligamento`; os 10 maiores, o resto somado em "Outros" e os nulos em "Não informado" (RF04, FR-031).
- [X] T045 [US4] Criar `src/main/java/br/edu/unidavi/rhgravatai/grafico/GraficoController.java` com `GET /api/v1/graficos/movimentacao` e `GET /api/v1/graficos/motivos-rescisao` (RF04).

**Ponto de controle (meta 17/10)**

---

## Fase 7: História 5 - Receber alertas de salários (P3)

**Meta**: regras de teto e de fora do padrão, recalculadas a cada carga, com `/alertas` (RF06).

**Teste independente**: dados com casos conhecidos geram exatamente os alertas esperados (SC-007).

- [X] T046 [P] [US5] Criar `src/test/java/br/edu/unidavi/rhgravatai/alerta/RegraForaPadraoTest.java` (RF06, FR-038, FR-039). Casos:
  - grupo de 5 com mediana 1000; salário 1200 (exatamente 20%) não gera alerta; 1200,01 gera;
  - 799,99 gera; 800 não gera;
  - grupo com 4 vínculos não gera;
  - nível nulo não gera; salário zero não gera;
  - grupo par usa a mediana pela média dos dois centrais;
  - mesmo cargo e nível com horas mensais diferentes formam grupos distintos.
- [X] T047 [P] [US5] Criar `src/test/java/br/edu/unidavi/rhgravatai/alerta/ResolvedorTetoTest.java` (RF06, FR-037):
  - **Valor manual**: um `parametro_teto` vigente prevalece (`MANUAL`); com dois valores, vale o de maior `vigencia_inicio` não posterior à data da carga.
  - **Fonte**: sem valor manual, usa o vínculo ativo de cargo `PREFEITO` (`FONTE`).
  - **Sem teto**: sem nenhum dos dois, a regra fica `SUSPENSA` e não gera alertas.
  - **Sinalização**: salário igual ao teto não é sinalizado; o vínculo do prefeito nunca é sinalizado.
- [X] T048 [P] [US5] Criar `ParametroTeto.java`, `Alerta.java`, `RegraAlerta.java` (`TETO`, `FORA_PADRAO`) e os repositórios em `src/main/java/br/edu/unidavi/rhgravatai/alerta/` (RF06).
- [X] T049 [US5] Criar `src/main/java/br/edu/unidavi/rhgravatai/alerta/ResolvedorTeto.java`, aplicando as regras do T047 e devolvendo valor, origem, fundamento e o id do vínculo-fonte (RF06, FR-037).
- [X] T050 [US5] Criar `src/main/java/br/edu/unidavi/rhgravatai/alerta/CalculadorAlertas.java`, implementando `AposPublicacao` na mesma transação da publicação (RF06, FR-037 a FR-041):
  1. Apagar todos os registros de `alerta`.
  2. **Teto**: inserir alertas `TETO` para vínculos ativos com salário maior que o teto, exceto o vínculo-fonte.
  3. **Fora do padrão**: inserir alertas `FORA_PADRAO`, por SQL com `percentile_cont(0.5)`, sobre grupos (`cargo`, `nivel`, `horas_mensais_min`) de vínculos ativos com `nivel` não nulo e salário maior que 0, com pelo menos `alertas.fora-padrao.minimo-grupo` vínculos e `abs(salario - mediana) / mediana > percentual/100`.
  4. Gravar `teto_valor` e `teto_origem` na carga.
- [X] T051 [US5] Criar `src/main/java/br/edu/unidavi/rhgravatai/alerta/AlertaController.java` e `AlertaService.java` com `GET /api/v1/alertas` (RF06, FR-040 a FR-042):
  - filtro `regra` mais `q`, `regime`, `secretaria` e `cargo`;
  - resposta conforme `PaginaAlertas`, com `teto` e `percentualForaPadrao`;
  - `descricaoRegra` em português, por exemplo: "Salário base 25,3% acima da mediana dos 12 vínculos ativos com o mesmo cargo, nível e carga horária." e "Salário base R$ 1.234,56 acima do teto municipal (subsídio do prefeito)".

**Ponto de controle (meta 17/10)**

---

## Fase 8: História 6 - Exportar relatórios em CSV e PDF (P3)

**Meta**: `/exportacoes/{relatorio}` para `vinculos`, `quadro`, `salarios` e `alertas` (RF05).

**Dependência**: reaproveita os serviços das US2, US3 e US5. O relatório `alertas` só existe
depois da US5.

**Teste independente**: o arquivo exportado tem os mesmos registros e totais da tela ([quickstart.md](quickstart.md) §5).

- [X] T052 [P] [US6] Criar `src/test/java/br/edu/unidavi/rhgravatai/exportacao/ExportacaoIntegracaoTest.java` (RF05, FR-033 a FR-036):
  - **CSV**: contém todas as linhas filtradas, mesmo acima do tamanho de página; começa com BOM UTF-8; separador `;`; valores como `3905,17`; datas `dd/MM/yyyy`; nenhum CPF.
  - **PDF**: `application/pdf` com texto do título e dos filtros; mais de `exportacao.pdf.max-linhas` responde 422 (use um limite menor só no teste).
  - **Arquivo**: `Content-Disposition` com nome e data.
- [X] T053 [P] [US6] Criar `src/main/java/br/edu/unidavi/rhgravatai/exportacao/EscritorCsv.java`: `StreamingResponseBody` lendo em fluxo (`Stream` do repositório com fetch size de 1000), cabeçalhos em português e formato do T052 (RF05, FR-034).
- [X] T054 [P] [US6] Criar `src/main/java/br/edu/unidavi/rhgravatai/exportacao/GeradorPdf.java`, com OpenPDF em A4 paisagem (RF05, FR-035):
  - título do relatório;
  - lista dos filtros aplicados;
  - "Dados atualizados em" com a última carga e "Gerado em";
  - tabela e totais;
  - rodapé "Fonte: API de Dados Abertos de Pessoal - Prefeitura de Gravataí/RS".
- [X] T055 [US6] Criar `src/main/java/br/edu/unidavi/rhgravatai/exportacao/ExportacaoController.java` com `GET /api/v1/exportacoes/{relatorio}?formato=csv|pdf` (RF05, FR-033, FR-036):
  - recebe os mesmos parâmetros de filtro e ordenação do endpoint correspondente;
  - conta as linhas antes de gerar o PDF e lança `ExportacaoMuitoGrandeException` acima do limite.

**Ponto de controle (meta 17/10): back-end completo**

---

## Fase 9: Acabamento e preocupações transversais

- [X] T056 [P] Criar `src/test/java/br/edu/unidavi/rhgravatai/ContratoApiTest.java`: todo caminho e método de `specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml` deve existir em `/v3/api-docs`. Divergência é defeito.
- [X] T057 Medir o desempenho com a base real, cerca de 55 mil vínculos (RNF02, SC-004). Cronometrar `/vinculos` com `q` e filtros, `/indicadores/*`, `/graficos/*` e `/alertas`. Se algum passar de 3 s, criar índice em `src/main/resources/db/migration/V2__indices_desempenho.sql`. Registrar os tempos em `docs/desempenho.md`.
- [X] T058 [P] Criar `docs/implantacao-aws.md`:
  - **Instância**: EC2 `t4g.small` com Amazon Linux e Docker Compose.
  - **Variáveis**: `DB_*`, `ADMIN_TOKEN` e `CORS_ORIGENS`.
  - **Rede**: security group liberando a porta 8080 só para o CloudFront (prefix list gerenciada).
  - **CloudFront**: comportamento `/api/*` para a EC2, sem cache e repassando a query string; padrão para o bucket S3 do front.
  - **Custos**: alertas do AWS Budgets em US$ 35 e US$ 45.
  - **Backup**: `pg_dump` diário.
  - **Encerramento**: passos para desligar tudo ao fim do semestre.
- [ ] T059 Implantar na AWS seguindo `docs/implantacao-aws.md`, rodar a carga em produção e conferir `GET /api/v1/status` pela URL do CloudFront (meta 20/10).
- [X] T060 [P] Criar `README.md` com objetivo, requisitos, como rodar localmente (link para o [quickstart.md](specs/001-consolidacao-dados-rh/quickstart.md)), link do Swagger e estrutura de pacotes.
- [ ] T061 Executar o [quickstart.md](quickstart.md) completo e a validação por amostragem de 100 registros (§6, SC-003, revisão de qualidade com Kaue Gorges e Guilherme Antunes). Registrar o resultado em `docs/validacao-amostragem.md` (meta 21/10).

---

## Dependências e ordem de execução

### Entre fases

- **Preparação (Fase 1)**: sem dependências.
- **Fundação (Fase 2)**: depende da Fase 1 e bloqueia todas as histórias.
- **US1 e US2 (P1)**: dependem só da Fase 2. A US2 usa dados de exemplo e não precisa da US1 para ser testada; precisa dela para ter dados reais.
- **US3 e US4 (P2)**: dependem só da Fase 2.
- **US5 (P3)**: depende da Fase 2 e da interface `AposPublicacao`, criada no T026 (US1).
- **US6 (P3)**: depende dos serviços da US2 (T035), da US3 (T039, T040) e da US5 (T051, só o relatório de alertas).
- **Acabamento (Fase 9)**: T057 e T059 dependem da US1 com carga real. T056 e T061 dependem de todas as histórias.

### Dentro de cada história

- Dados de teste antes dos testes; entidades antes dos serviços; serviços antes dos controladores.
- O teste da história deve passar antes de marcar o ponto de controle.

### Oportunidades de paralelismo

- **Fase 1**: T004, T005 e T006.
- **Fase 2**: T008, T009, T010, T012, T014, T015 e T016, depois do T007.
- **US1**: T017, T018, T019, T021, T023 e T024 em paralelo; depois T022, T025, T026 e T027.
- **Entre pessoas**: depois da Fase 2, uma pessoa segue a US1 (carga) enquanto outra faz a US2 e a US3 com os dados de exemplo.

## Exemplo de paralelismo: História 1

```bash
# Em paralelo, arquivos diferentes e sem dependência entre si:
Tarefa: "T017 Páginas sintéticas em src/test/resources/fonte/"
Tarefa: "T021 RegistroFonte e PaginaFonte em importacao/fonte/"
Tarefa: "T023 TratadorRegistro em importacao/tratamento/"
Tarefa: "T024 Entidades VinculoStaging, RegistroRejeitado e MudancaVinculo"

# Depois:
Tarefa: "T022 ClienteFonte"  →  "T025 ExecutorCarga"  →  "T026 PublicadorCarga"
```

## Exemplo de paralelismo: Histórias 3 e 4

```bash
Tarefa: "T039 QuadroService"
Tarefa: "T040 SalarioService"
Tarefa: "T043 MovimentacaoService"
Tarefa: "T044 MotivosRescisaoService"
```

---

## Estratégia de implementação

### MVP primeiro (US1 + US2, meta 16/10)

1. Fase 1 (preparação) e Fase 2 (fundação).
2. US1: carga real concluída e conciliada.
3. US2: consulta com filtros.
4. **Parar e validar**: [quickstart.md](quickstart.md) §3 e §4.

### Entrega incremental

| Até | Fases | Validação |
|-----|-------|-----------|
| 16/10 (sex) | 1, 2, US1, US2 | carga real e consulta |
| 17/10 (sáb) | US3, US4, US5, US6, T056 | back-end completo, `mvn verify` passando |
| 18 a 20/10 | front-end, T057 a T060 | telas consumindo a API; produção no ar |
| 21/10 | T061 | amostragem de 100 registros e ajustes |
| 22/10 | | apresentação |

### Equipe

- **Desenvolvimento** (back-end, front-end e implantação): Deivid, com apoio do Claude.
- **Qualidade**: Kaue Gorges e Guilherme Antunes revisam os casos de alerta (T046, T047),
  testam pelo Swagger e participam da amostragem (T061).
- A organização em repositórios pode mudar (front-end no mesmo repositório). Se isso
  acontecer, o back-end vai para uma pasta `backend/` sem mudar as tarefas.

---

## Observações

- [P] = arquivos diferentes, sem dependência pendente.
- Cada história tem ponto de controle próprio; pare nele para validar.
- Commits por tarefa ou grupo lógico, com mensagem em português, sem linha de coautoria.
- Dados de teste são sempre fictícios: nenhum nome ou CPF real em `src/test/resources/`.
