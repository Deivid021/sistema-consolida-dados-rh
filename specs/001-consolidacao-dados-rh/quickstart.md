# Guia de Validação (Quickstart): Consolidação de Dados de RH

**Plano**: [plan.md](plan.md) | **Contrato**: [contracts/api-consulta.yaml](contracts/api-consulta.yaml) | **Modelo**: [data-model.md](data-model.md)

Roteiro para provar que a funcionalidade funciona de ponta a ponta. Não contém código.

## Pré-requisitos

- Java 21 e Maven 3.9 ou superior (`java -version`, `mvn -v`).
- Docker, para o PostgreSQL local e para os testes de integração.
- Acesso à internet, para a carga real a partir da fonte de Gravataí.

## 1. Subir o ambiente local

```bash
docker compose up -d banco                 # PostgreSQL 17 em localhost:5432
export ADMIN_TOKEN=um-token-local
mvn spring-boot:run                        # aplica as migrações Flyway e sobe em :8080
```

Esperado: a aplicação sobe sem erro e `GET http://localhost:8080/api/v1/status` responde
`ultimaAtualizacao: null`.

## 2. Testes automatizados

```bash
mvn verify
```

Esperado: todos os testes passam, incluindo:

- conversão de datas, valores e horas, e rejeição de formatos inválidos;
- carga completa contra a fonte simulada: conciliação correta, HTTP 429 com nova
  tentativa, erro disfarçado em HTTP 200 tratado como falha, total que muda no meio
  resultando em `INCONSISTENTE` sem alterar os dados publicados;
- ausência de CPF: nenhuma coluna de CPF no esquema e nenhum valor com padrão de CPF
  gravado;
- as duas regras de alerta, com os casos de fronteira (salário igual ao teto; diferença de
  exatamente 20%; grupo com 4 vínculos);
- filtros isolados e combinados, e totais dos indicadores fechando com o total da consulta.

## 3. Carga real (História 1)

```bash
curl -X POST -H "X-Admin-Token: $ADMIN_TOKEN" http://localhost:8080/api/v1/admin/cargas
# acompanhar:
curl -H "X-Admin-Token: $ADMIN_TOKEN" http://localhost:8080/api/v1/admin/cargas
```

Esperado, em 10 a 20 minutos:

- a carga termina com `status: SUCESSO`;
- `gravados + rejeitados` é igual a `totalInformado` (cerca de 55 mil);
- `GET /api/v1/status` mostra a data da carga e o teto com `origem: FONTE`.

Conferir no banco que não existe CPF:

```sql
SELECT count(*) FROM information_schema.columns WHERE column_name ILIKE '%cpf%';  -- 0
```

Rodar uma segunda carga. Esperado: `novos` próximo de 0 e mudanças registradas em
`mudanca_vinculo` só se a fonte mudou.

## 4. Consulta e filtros (História 2)

- `GET /api/v1/vinculos?situacao=AFASTADO&secretaria=<uma secretaria>`: só afastados da
  secretaria, com `total`.
- `GET /api/v1/vinculos?q=professor`: encontra também "PROFESSOR" e cargos com acento.
- `GET /api/v1/vinculos?admissaoInicio=2026-12-31&admissaoFim=2026-01-01`: HTTP 400 com
  mensagem em português.
- Medir o tempo de cada chamada: deve ficar abaixo de 3 segundos (RNF02).

## 5. Indicadores, gráficos, alertas e exportação (Histórias 3 a 6)

- `GET /api/v1/indicadores/quadro`: a soma de `porSituacao` é igual a `total`; idem para
  regime e secretaria.
- `GET /api/v1/indicadores/salarios?cargo=PROGRAMADOR SENIOR`: conferir soma e média
  manualmente com `/vinculos` usando o mesmo filtro.
- `GET /api/v1/graficos/movimentacao?granularidade=ANO&inicio=2020-01-01&fim=2026-12-31`.
- `GET /api/v1/graficos/motivos-rescisao?inicio=2026-01-01&fim=2026-12-31`: no máximo 10
  motivos, mais "Outros".
- `GET /api/v1/alertas?regra=TETO`: cada item com salário maior que o teto; o prefeito não
  aparece.
- `GET /api/v1/exportacoes/vinculos?formato=csv&situacao=AFASTADO`: abrir no Excel ou no
  LibreOffice; acentos e valores corretos e mesmo total da tela.
- `GET /api/v1/exportacoes/vinculos?formato=pdf`, sem filtro: HTTP 422 (mais de 5.000
  linhas). Com filtro de uma secretaria pequena: PDF com título, filtros, datas e totais.

## 6. Validação por amostragem (SC-003)

1. Sortear 100 matrículas:
   `SELECT matricula FROM vinculo ORDER BY random() LIMIT 100;`
2. Para cada uma, comparar no Portal da Transparência de Gravataí: situação, cargo,
   secretaria, salário base e datas.
3. Registrar o resultado numa planilha. A meta é zero divergências. Responsáveis: Kaue
   Gorges e Samuel P. Frare.

## 7. Contrato para o front-end

Com a aplicação no ar, a tela do Swagger fica em `http://localhost:8080/swagger-ui.html` e o
contrato em `/v3/api-docs`. O arquivo de referência é
[contracts/api-consulta.yaml](contracts/api-consulta.yaml). Divergências entre os dois são
defeito do back-end.
