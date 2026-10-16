# Plano de Implementação: Consolidação e Consulta dos Dados de RH da Prefeitura de Gravataí/RS

**Branch**: `001-consolidacao-dados-rh` | **Data**: 2026-10-14 | **Especificação**: [spec.md](spec.md)

**Entrada**: especificação da funcionalidade em `specs/001-consolidacao-dados-rh/spec.md`

## Resumo

Back-end único em Java 21 com Spring Boot que:

1. importa diariamente os 55 mil registros de pessoal da API pública de Gravataí, com área
   de preparação, conciliação de totais e descarte do CPF já na leitura;
2. publica os dados numa única transação, registrando as mudanças entre cargas;
3. oferece uma API REST de consulta, indicadores, gráficos, alertas e exportação CSV/PDF.

O front-end React fica em outro repositório e consome o contrato definido em
[contracts/api-consulta.yaml](contracts/api-consulta.yaml). A hospedagem é uma EC2 com
Docker Compose (aplicação e PostgreSQL) atrás de CloudFront, dentro do crédito AWS de
US$ 50. Detalhes e alternativas em [research.md](research.md).

## Contexto Técnico

**Linguagem/Versão**: Java 21

**Dependências principais**: Spring Boot 4.1.1 (Web MVC, JDBC com `JdbcClient`, Validation,
Actuator, Flyway), driver PostgreSQL, OpenPDF 3.0.5, springdoc-openapi 3.1.x. Sem JPA: as
consultas, agregações e a publicação da carga são SQL explícito (decisão de 2026-10-15).

**Armazenamento**: PostgreSQL 17 com extensão `pg_trgm`

**Testes**: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL), `MockRestServiceServer`
para simular a fonte

**Plataforma alvo**: Linux, em contêiner Docker numa EC2 `t4g.small` (ARM)

**Tipo de projeto**: serviço web (API REST); front-end em repositório separado

**Metas de desempenho**: consultas, indicadores, gráficos e alertas em até 3 s (RNF02) com
55 mil registros; carga completa em até 30 min

**Restrições**:

- 1 requisição por segundo à fonte;
- nenhum CPF armazenado;
- custo dentro do crédito AWS;
- entrega em 22/10/2026.

**Escala/Escopo**: cerca de 55 mil vínculos, cerca de 100 mil linhas de histórico por ano,
acesso público de baixo volume, 11 operações na API

## Verificação da Constituição

*Portão: deve passar antes da Fase 0 e ser reavaliado depois da Fase 1.*

| Princípio | Como o plano atende | Situação |
|-----------|---------------------|----------|
| I. Dados públicos e proteção de dados (inegociável) | Única fonte é a API pública. O CPF não existe no objeto de leitura (research §4). O corpo bruto nunca vai para log. `registro_rejeitado` guarda só o campo com problema. Há teste automatizado de ausência de CPF. O nome é público pela LAI (decisão da especificação). | Atende |
| II. Fidelidade à fonte e rastreabilidade | Área de preparação com conciliação de totais. Carga inconsistente não publica. Toda carga é registrada em `carga`. `mudanca_vinculo` guarda o histórico. Vínculos ausentes são marcados e nunca apagados. Nenhum valor é corrigido. Alertas trazem o critério em `descricaoRegra`. | Atende |
| III. Escopo fechado nos seis RFs | Cada endpoint indica o RF que atende no contrato. Não há telas nem funcionalidades extras. O endpoint `/admin/cargas` é a operação do RF01 (carga inicial e demonstração), não uma funcionalidade nova. | Atende |
| IV. Simplicidade e custo zero | Uma aplicação, um banco, uma instância. Sem filas, cache ou mecanismo de busca. Bibliotecas gratuitas e de código aberto. Custo pago com o crédito acadêmico (constituição v1.0.1). springdoc foi incluído para servir o contrato à equipe de front; é uma dependência só. | Atende |
| V. Qualidade verificável | Testes unitários das regras e conversões; testes de integração com banco real e fonte simulada; meta de 3 s verificada no roteiro; responsividade é responsabilidade do repositório de front (RNF03). | Atende |
| Restrições técnicas | Java 21 + Spring Boot, React em repositório próprio, PostgreSQL, AWS, CSV/PDF, português. | Atende |

**Reavaliação após a Fase 1**: o modelo de dados e o contrato não introduziram violações.
Nenhuma entrada em "Acompanhamento de Complexidade".

## Estrutura do Projeto

### Documentação (esta funcionalidade)

```text
specs/001-consolidacao-dados-rh/
├── spec.md              # especificação
├── plan.md              # este arquivo
├── research.md          # Fase 0
├── data-model.md        # Fase 1
├── quickstart.md        # Fase 1, roteiro de validação
├── contracts/
│   └── api-consulta.yaml  # Fase 1, contrato com o front-end
├── checklists/
│   └── requirements.md
└── tasks.md             # Fase 2 (/speckit-tasks, ainda não criado)
```

### Código-fonte (raiz do repositório)

Projeto Maven único na raiz. Pacotes organizados por funcionalidade, não por camada técnica.

```text
pom.xml
compose.yaml                         # banco local; em produção, banco + aplicação
Dockerfile
src/main/java/br/edu/unidavi/rhgravatai/
├── RhGravataiApplication.java
├── comum/                           # configuração, erros (problem+json), normalização de texto, filtros comuns
├── importacao/                      # RF01
│   ├── fonte/                       # cliente da API de Gravataí, DTO sem CPF, limite e novas tentativas
│   ├── tratamento/                  # conversão e validação de cada registro
│   ├── carga/                       # orquestração, área de preparação, conciliação, publicação, histórico
│   └── agendamento/                 # @Scheduled e endpoint /admin/cargas
├── vinculo/                         # RF02: entidade, repositório, consulta com filtros, opções de filtro
├── indicador/                       # RF03: quadro e salários
├── grafico/                         # RF04: movimentação e motivos
├── alerta/                          # RF06: teto, fora do padrão, recálculo pós-carga
├── exportacao/                      # RF05: CSV em fluxo, PDF com OpenPDF
└── status/                          # /status
src/main/resources/
├── application.yml
└── db/migration/                    # V1__esquema_inicial.sql, ...
src/test/java/br/edu/unidavi/rhgravatai/
├── importacao/                      # unidade (tratamento) e integração (carga com fonte simulada)
├── vinculo/  indicador/  grafico/  alerta/  exportacao/
└── privacidade/                     # teste de ausência de CPF
src/test/resources/fonte/            # páginas JSON sintéticas da fonte
```

**Decisão de estrutura**: um único módulo Maven na raiz deste repositório, que já guarda
`specs/` e `docs-project/`. O front-end não fica aqui (decisão D12 da especificação).
Pacotes por funcionalidade deixam cada RF num lugar só, facilitando a divisão de tarefas.

## Ordem de Entrega

Desenvolvimento feito por Deivid com apoio do Claude; Kaue Gorges e Guilherme Antunes cuidam da
qualidade. O detalhamento está em [tasks.md](tasks.md).

| Até | Entrega | Histórias |
|-----|---------|-----------|
| 16/10 (sex) | Projeto, esquema do banco, carga completa, `/status`, `/vinculos` e `/filtros/opcoes` | 1 e 2 (P1) |
| 17/10 (sáb) | Indicadores, gráficos, alertas e exportação: back-end completo | 3 a 6 |
| 18 a 20/10 | Front-end e implantação na AWS | todas |
| 21/10 | Validação por amostragem (100 registros) e ajustes | todas |
| 22/10 | Apresentação final | |

O marco de 15/10 dos documentos aprovados não será cumprido para indicadores e gráficos. A
mudança de prazo deve ser registrada como Solicitação de Mudança (constituição, princípio III).

## Riscos Técnicos

| Risco | Resposta |
|-------|----------|
| A fonte cai ou muda o formato | Os dados da última carga continuam publicados. A falha fica registrada em `carga`. Para a apresentação, ter uma carga bem-sucedida feita no dia anterior. |
| Limite de 1.000 requisições por dia na fonte (uma carga usa cerca de 553) | Uma carga completa por dia, de madrugada. Não disparar carga manual no mesmo dia, inclusive na apresentação. No limite diário a carga para na hora e os dados anteriores continuam publicados. Melhoria futura: carga incremental (ativos e desligados recentes por dia, histórico completo por semana). |
| Páginas lentas (até 17 s) | Tempo limite de 90 s por página e novas tentativas. A carga roda de madrugada. |
| Crédito AWS acaba | Alerta no Budgets em US$ 35 e US$ 45. Desligar a instância depois da avaliação, se ninguém assumir o custo. |
| Pouco tempo para o front-end (3 dias) | Contrato fechado antes; back-end completo até 17/10 para o front começar com a API pronta. |

## Acompanhamento de Complexidade

Sem violações da constituição a justificar.
