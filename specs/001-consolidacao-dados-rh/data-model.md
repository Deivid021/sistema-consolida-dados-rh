# Modelo de Dados (Fase 1): Consolidação de Dados de RH

**Data**: 2026-10-14 | **Plano**: [plan.md](plan.md) | **Pesquisa**: [research.md](research.md)

Banco PostgreSQL 17, com o esquema criado por migrações Flyway. Nenhuma tabela tem coluna
de CPF (princípio I da constituição).

## Visão geral

```text
carga 1───N vinculo_staging        (área de preparação, limpa a cada carga)
carga 1───N registro_rejeitado
carga 1───N mudanca_vinculo N───1 vinculo
carga 1───N alerta          N───1 vinculo
parametro_teto                     (independente)
```

## carga

Uma execução de importação (FR-009).

| Campo | Tipo | Regra |
|-------|------|-------|
| id | bigint, identidade | |
| origem | texto | `AGENDADA` ou `MANUAL` |
| status | texto | `EM_ANDAMENTO`, `SUCESSO`, `FALHA` ou `INCONSISTENTE` |
| inicio, fim | timestamptz | `fim` nulo enquanto estiver em andamento |
| desligamento_inicio, desligamento_fim | date | período consultado (01/01/1900 até a data da carga) |
| total_informado | int | `totalRegistros` da primeira página |
| total_informado_final | int | `totalRegistros` da última página |
| total_paginas, paginas_lidas | int | |
| gravados, rejeitados | int | gravados conta matrículas distintas |
| novos, alterados, nao_encontrados | int | preenchidos só em `SUCESSO` |
| teto_valor | numeric(12,2) | nulo se a regra estiver suspensa |
| teto_origem | texto | `FONTE`, `MANUAL` ou `SUSPENSA` |
| mensagem | texto | motivo da falha ou da inconsistência |

Índice único parcial em `status` onde `status = 'EM_ANDAMENTO'`, para permitir uma carga
por vez.

**Transições**:

```text
EM_ANDAMENTO ──conciliação ok + publicação ok──▶ SUCESSO
EM_ANDAMENTO ──totais divergentes──────────────▶ INCONSISTENTE
EM_ANDAMENTO ──erro, tentativas esgotadas──────▶ FALHA
EM_ANDAMENTO ──aplicação reiniciada─────────────▶ FALHA ("interrompida")
```

A "última carga" mostrada aos usuários (FR-013) é sempre a última com `SUCESSO`.

## vinculo

Registro publicado, consultado pelo site. Chave natural: `(entidade, matricula)` (FR-008).

| Campo | Tipo | Origem na fonte | Regra |
|-------|------|-----------------|-------|
| id | bigint, identidade | | |
| entidade | texto, obrigatório | `entidade` | |
| matricula | texto, obrigatório | `matricula` | formato `número-sequência` |
| nome | texto | `nome` | FR-021 |
| situacao | texto, obrigatório | `situacao` | `TRABALHANDO`, `AFASTADO` ou `DESLIGADO` (de "Demitido", FR-023) |
| data_admissao | date | `admissao` | |
| data_desligamento | date | `dataRescisao` | só desligados |
| motivo_desligamento | texto | `motivoRescisao` | como veio (até 40 caracteres) |
| regime | texto | `regime` | |
| classificacao | texto | `classificacao` | |
| forma_investidura | texto | `formaInvestidura` | armazenado, não é filtro |
| secretaria | texto | `centroCusto` | |
| local_trabalho | texto | `localTrabalho` | |
| cargo | texto | `cargo` | |
| funcao | texto | `funcao` | |
| nivel | texto | `nivel` | número ou texto |
| salario_base | numeric(12,2) | `salarioBase` | 0,00 é aceito |
| horario | texto | `horario` | descrição livre |
| horas_mensais_min | int | `horasMensais` | minutos (180:00 → 10800) |
| horas_semanais_min | int | `horasSemanais` | minutos |
| valores_originais | jsonb | | textos originais dos campos convertidos (FR-006) |
| texto_busca | texto | | nome, cargo e matrícula, minúsculos e sem acento (FR-019) |
| ativo_na_fonte | boolean | | falso se não veio na última carga bem-sucedida (FR-011) |
| primeira_carga_id, ultima_carga_id | bigint → carga | | |
| visto_por_ultimo_em | timestamptz | | |

**Índices**: único `(entidade, matricula)`; trigram (GIN) em `texto_busca`; simples em
`situacao`, `regime`, `secretaria`, `cargo`, `data_admissao` e `data_desligamento`.

**Definição de vínculo ativo** (FR-026, FR-037, FR-038): `situacao` em (`TRABALHANDO`,
`AFASTADO`) e `ativo_na_fonte = true`.

**Valores nulos**: armazenados como `NULL`. A API devolve `null` e o rótulo
"Não informado" é responsabilidade do front-end. Os filtros aceitam o valor especial
`NAO_INFORMADO` (ver contrato).

## vinculo_staging

Mesmas colunas de negócio de `vinculo`, mais `carga_id` e `pagina`. Recebe os registros
convertidos durante a leitura. É esvaziada no início de cada carga e não é lida pelo site.

## registro_rejeitado

| Campo | Tipo | Regra |
|-------|------|-------|
| id | bigint | |
| carga_id | bigint → carga | |
| pagina | int | |
| matricula | texto | nulo se ausente |
| campo | texto | nome do campo da fonte que falhou |
| valor_recebido | texto | só o valor do campo com problema, nunca o registro inteiro; nunca o CPF |
| motivo | texto | |

## mudanca_vinculo

Histórico de mudanças detectadas entre cargas (FR-010).

| Campo | Tipo | Regra |
|-------|------|-------|
| id | bigint | |
| vinculo_id | bigint → vinculo | |
| carga_id | bigint → carga | carga que detectou a mudança |
| atributo | texto | `situacao`, `regime`, `secretaria`, `local_trabalho`, `cargo`, `funcao`, `nivel`, `salario_base`, `horas_mensais_min`, `horas_semanais_min`, `data_desligamento` ou `ativo_na_fonte` |
| valor_anterior, valor_novo | texto | |
| detectado_em | timestamptz | |

Na primeira carga não se gravam mudanças, porque tudo é novo; apenas `primeira_carga_id`.

## parametro_teto

Valor manual do teto, que prevalece sobre o valor da fonte (FR-037). É mantido pela equipe
por SQL ou migração, sem tela.

| Campo | Tipo | Regra |
|-------|------|-------|
| id | bigint | |
| valor | numeric(12,2), obrigatório | maior que zero |
| vigencia_inicio | date, obrigatório | único |
| fundamento | texto | por exemplo, número da lei |
| criado_em | timestamptz | |

## alerta

Recalculada a cada carga bem-sucedida: apaga e insere na mesma transação (FR-041).

| Campo | Tipo | Regra |
|-------|------|-------|
| id | bigint | |
| carga_id | bigint → carga | |
| vinculo_id | bigint → vinculo | |
| regra | texto | `TETO` ou `FORA_PADRAO` |
| valor_vinculo | numeric(12,2) | salário base do vínculo |
| valor_referencia | numeric(12,2) | teto ou mediana do grupo |
| diferenca | numeric(12,2) | `valor_vinculo − valor_referencia` |
| diferenca_percentual | numeric(7,2) | sobre `valor_referencia` |
| grupo_tamanho | int | só `FORA_PADRAO` |

Único `(vinculo_id, regra)`.

## Regras de validação e conversão

| Campo da fonte | Regra | Se falhar |
|----------------|-------|-----------|
| `matricula` | não vazio | rejeita |
| `situacao` | `Trabalhando`, `Afastado` ou `Demitido` | rejeita |
| `admissao`, `dataRescisao` | `dd/MM/yyyy` válido, quando presente | rejeita |
| `salarioBase` | `^\d{1,3}(\.\d{3})*,\d{2}$`, quando presente | rejeita |
| `horasMensais`, `horasSemanais` | `^\d+:\d{2}$`, quando presente | rejeita |
| `cpf` | ignorado na leitura | nunca é lido |
| demais textos | espaços nas pontas removidos; vazio vira nulo | |
