# Medições de desempenho (RNF02, SC-004)

**Data**: 2026-10-15
**Base**: carga real completa da API de Gravataí, com 55.207 vínculos (8.832 ativos e 46.375
desligados), num banco de 88 MB.
**Ambiente**: máquina de desenvolvimento com 12 núcleos e 6,7 GB de memória; PostgreSQL 17 em
Docker; aplicação via `mvn spring-boot:run`. Uma segunda carga estava rodando ao mesmo tempo.

**Meta**: até 3 segundos por consulta.

| Endpoint | Tempo | Tamanho |
|----------|-------|---------|
| `/vinculos` (sem filtro) | 0,09 s | 8,8 KB |
| `/vinculos?q=professor` | 0,06 s | 9,1 KB |
| `/vinculos?q=silva&situacao=TRABALHANDO&ordenacao=salarioBase,desc` | 0,02 s | 8,7 KB |
| `/vinculos?secretaria=…&admissaoInicio=2020-01-01&pagina=3` | 0,02 s | 0,1 KB |
| `/vinculos?desligamentoInicio=2020-01-01&ordenacao=dataDesligamento,desc` | 0,03 s | 9,4 KB |
| `/filtros/opcoes` | 0,09 s | 140 KB |
| `/indicadores/quadro` | 0,05 s | 38 KB |
| `/indicadores/quadro?regime=CLT` | 0,02 s | 10 KB |
| `/indicadores/salarios` | 0,03 s | 2,9 KB |
| `/indicadores/salarios?situacao=DESLIGADO&ordenacao=media,desc` | 0,07 s | 2,6 KB |
| `/graficos/movimentacao` (mensal, 1950 a 2026) | 0,06 s | 51 KB |
| `/graficos/movimentacao` (anual, 1900 a 2026) | 0,05 s | 6,8 KB |
| `/graficos/motivos-rescisao` (1900 a 2026) | 0,03 s | 0,8 KB |
| `/alertas` | 0,03 s | 0,2 KB |
| `/status` | 0,03 s | 0,2 KB |
| `/exportacoes/vinculos?formato=csv` (55.207 linhas) | 1,06 s | 10,9 MB |
| `/exportacoes/vinculos?formato=pdf&regime=ELETIVO` | 0,20 s | 15 KB |

**Resultado**: todos os endpoints atendem à meta com folga. Não foi preciso criar índices
além dos da migração V1.

**Carga completa**: 553 páginas em 11 min 34 s (cerca de 1,25 s por página, respeitando o limite
de uma requisição por segundo da fonte).

Na EC2 `t4g.small` (2 vCPU, 2 GB), os tempos devem ser maiores. Refazer estas medições depois
da implantação (T059).
