# Sistema de Consolidação de Dados de RH — Prefeitura de Gravataí/RS

Importa diariamente os dados de pessoal publicados pela
[API de Dados Abertos de Pessoal de Gravataí](https://gravatai.atende.net/) e oferece uma API de
consulta com filtros, indicadores, gráficos, alertas de salário e exportação em CSV e PDF.

Projeto da disciplina de Gerência de Projetos (Unidavi), 2026.

## Documentação

| Documento | Conteúdo |
|-----------|----------|
| [Constituição](.specify/memory/constitution.md) | Princípios do projeto (dados públicos, sem CPF, escopo fechado) |
| [Especificação](specs/001-consolidacao-dados-rh/spec.md) | Regras de negócio (RF01 a RF06, RNF01 a RNF03) |
| [Plano](specs/001-consolidacao-dados-rh/plan.md) e [pesquisa](specs/001-consolidacao-dados-rh/research.md) | Decisões técnicas e comportamento da fonte |
| [Modelo de dados](specs/001-consolidacao-dados-rh/data-model.md) | Tabelas e regras de conversão |
| [Contrato da API](specs/001-consolidacao-dados-rh/contracts/api-consulta.yaml) | Contrato com o front-end (OpenAPI) |
| [Guia de validação](specs/001-consolidacao-dados-rh/quickstart.md) | Como provar que tudo funciona |
| [Tarefas](specs/001-consolidacao-dados-rh/tasks.md) | Andamento da implementação |
| [Implantação na AWS](docs/implantacao-aws.md) | Passo a passo e encerramento |
| [Desempenho](docs/desempenho.md) | Tempos medidos com a base real |

## Tecnologias

Java 21, Spring Boot 4.1, PostgreSQL 17 (Flyway), OpenPDF, springdoc-openapi. Testes com JUnit 5
e Testcontainers.

## Rodar localmente

Pré-requisitos: Java 21, Maven 3.9 e Docker acessível sem `sudo`.

```bash
docker compose up -d banco                      # PostgreSQL em localhost:5433
ADMIN_TOKEN=token-local mvn spring-boot:run     # API em http://localhost:8080
```

- Swagger: <http://localhost:8080/swagger-ui.html>
- Situação dos dados: <http://localhost:8080/api/v1/status>
- Primeira carga (cerca de 12 minutos):
  ```bash
  curl -X POST -H "X-Admin-Token: token-local" http://localhost:8080/api/v1/admin/cargas
  curl -H "X-Admin-Token: token-local" http://localhost:8080/api/v1/admin/cargas   # acompanhar
  ```

Para o front-end rodando em outra porta, defina `CORS_ORIGENS=http://localhost:5173`.

## Testes

```bash
mvn verify
```

Os testes de integração usam um PostgreSQL em contêiner e uma fonte simulada com dados
fictícios; nunca acessam a API real.

## Estrutura

```text
src/main/java/br/edu/unidavi/rhgravatai/
├── comum/        filtros, paginação, erros, normalização de texto
├── importacao/   cliente da API, tratamento, carga, agendamento (RF01)
├── vinculo/      consulta e opções de filtro (RF02)
├── indicador/    quadro e salários (RF03)
├── grafico/      movimentação e motivos de rescisão (RF04)
├── exportacao/   CSV e PDF (RF05)
├── alerta/       teto municipal e salário fora do padrão (RF06)
└── status/       data da última carga e totais
```

## Privacidade

O CPF enviado pela fonte é descartado na leitura e nunca é armazenado (LGPD, constituição,
princípio I). Os testes `EsquemaSemCpfTest` e `CargaIntegracaoTest` verificam isso.
