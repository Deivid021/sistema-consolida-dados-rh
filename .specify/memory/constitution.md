# Constituição do Sistema de Consolidação de Dados de RH da Prefeitura de Gravataí/RS

## Princípios Fundamentais

### I. Dados públicos e proteção de dados pessoais (INEGOCIÁVEL)

- O sistema DEVE usar exclusivamente os dados públicos do endpoint
  `/transparencia-pessoal-funcionarios` da API de Dados Abertos de Pessoal de Gravataí.
- O CPF NUNCA DEVE ser armazenado, nem anonimizado, mascarado ou em forma de hash. Ele DEVE
  ser descartado na etapa de tratamento, antes de qualquer gravação em banco, arquivo,
  cache, log ou cópia local.
- O sistema NÃO DEVE armazenar dados pessoais sensíveis nem acessar sistemas internos de RH
  ou de folha de pagamento da Prefeitura.
- Toda funcionalidade DEVE estar em conformidade com a Lei de Acesso à Informação
  (Lei 12.527/2011) e com a LGPD (Lei 13.709/2018).

Justificativa: RNF01, RT04, RT05 e RT06. Um vazamento ou uso indevido de dado pessoal
invalida o projeto, por mais que o restante funcione.

### II. Fidelidade à fonte oficial e rastreabilidade

- A importação DEVE trazer 100% dos registros informados em `totalRegistros`, com
  `tipoBusca=3` (trabalhando, afastados e desligados), percorrendo todas as páginas.
- Nenhum registro DEVE ser descartado ou alterado em silêncio. Registros rejeitados no
  tratamento DEVEM ser contados e registrados com o motivo.
- Cada execução de carga ou sincronização DEVE registrar data, quantidade lida, quantidade
  gravada e resultado (sucesso ou falha).
- O histórico das cargas DEVE ser preservado, pois a API só expõe a situação atual.
- Os dados da Prefeitura são a fonte oficial: o sistema NÃO DEVE corrigir nem inferir
  valores. Indicadores e gráficos DEVEM ser reproduzíveis a partir dos dados armazenados.
- Os alertas (RF06) DEVEM ser apresentados como sinalizações para verificação, com o
  critério da regra visível, e não como afirmação de irregularidade.

Justificativa: Objetivos 1 e 4 (meta de zero divergências em amostra de 100 registros
comparada ao Portal da Transparência) e risco R08 (API fora do ar ou incompleta).

### III. Escopo fechado nos seis requisitos funcionais

- Toda especificação, plano e tarefa DEVE apontar para pelo menos um dos requisitos
  RF01 a RF06 ou RNF01 a RNF04, conforme a Declaração do Escopo e as Solicitações de
  Mudança aprovadas. O RNF04 (Modo Escuro de Alto Contraste) veio da Solicitação de
  Mudança 3c, aprovada pelo patrocinador em 08/10/2026.
- Funcionalidade sem requisito correspondente NÃO DEVE ser implementada.
- Mudança de escopo, prazo ou entrega só é aceita com Solicitação de Mudança registrada e
  aprovada pelo patrocinador.
- Ficam fora do escopo: aplicativo móvel nativo, dados que não venham da API pública e
  qualquer integração com sistemas internos da Prefeitura.

Justificativa: RT07, RT9 e risco R03. O prazo é curto e o limite de seis requisitos é
regra da disciplina.

### IV. Simplicidade e custo zero

- O orçamento é R$ 0,00: só PODEM ser usadas ferramentas, bibliotecas e serviços gratuitos
  ou de código aberto, incluindo hospedagem e banco de dados.
- A solução DEVE usar as tecnologias que a equipe já domina (ver Restrições Técnicas).
  Adotar uma tecnologia nova exige justificativa escrita no plano.
- Entre duas soluções que atendem ao requisito, DEVE ser escolhida a mais simples. Camadas,
  abstrações e serviços adicionais precisam de justificativa registrada no plano.

Justificativa: RT02, premissas P04 e P08 e riscos R01 e R05. A equipe é de acadêmicos com
dedicação parcial de cerca de 8 horas semanais.

### V. Qualidade verificável

- Regras de negócio, cálculos de indicadores e regras de alerta DEVEM ter testes
  automatizados. As duas regras de alerta (teto municipal e salário fora do padrão do
  cargo e nível) DEVEM ter testes que comprovem o funcionamento.
- A integração com a API e os serviços de consulta DEVEM ter testes de integração.
- As consultas DEVEM responder em até 3 segundos (RNF02).
- A interface DEVE ser responsiva e utilizável em computadores e dispositivos móveis
  (RNF03).
- A interface DEVE oferecer o Modo Escuro de Alto Contraste, com contraste verificado
  por medição, conforme as diretrizes de acessibilidade WCAG 2.1 (RNF04).
- Uma entrega só é considerada concluída quando seus critérios de aceitação foram
  verificados e o resultado foi registrado.

Justificativa: Objetivos 2 e 4 e pacote 6.1 da EAP. Indicadores públicos errados
prejudicam a transparência que o projeto se propõe a ampliar.

## Restrições Técnicas e Legais

- **Back-end**: Java 21 com Spring Boot, expondo uma API REST de consulta.
- **Front-end**: React, como aplicação web responsiva, na pasta `frontend/` do mesmo
  repositório, empacotada junto com o back-end.
- **Banco de dados**: PostgreSQL.
- **Hospedagem**: AWS, conforme o Dicionário da EAP, custeada por crédito acadêmico de
  US$ 50 válido até o fim do semestre letivo. O gasto NÃO DEVE ultrapassar o crédito.
- **Fonte de dados**: `GET /transparencia-pessoal-funcionarios`, com paginação,
  `tipoBusca=3` e sem autenticação. O sistema DEVE manter cópia local dos dados para
  continuar funcionando se a API estiver indisponível.
- **Atualização**: a sincronização DEVE ser agendada e automática.
- **Exportação**: relatórios em CSV e PDF.
- **Alertas**: o teto (subsídio do prefeito) e o percentual da regra de salário fora do
  padrão DEVEM ser parâmetros configuráveis. O teto padrão é o salário base do cargo
  "PREFEITO" publicado pela própria fonte.
- **Idioma**: documentação, especificações, planos, tarefas e textos da interface DEVEM
  ser escritos em português do Brasil.
- **Prazo**: entrega e apresentação final em 22/10/2026 (RT01), sem prorrogação.

## Fluxo de Desenvolvimento e Portões de Qualidade

1. Cada funcionalidade segue o fluxo especificação → plano → tarefas → implementação.
2. A especificação DEVE citar os requisitos (RF/RNF) e os pacotes da EAP que atende.
3. O plano DEVE incluir a verificação desta constituição. Qualquer violação DEVE estar
   justificada por escrito ou o plano é reprovado.
4. As tarefas DEVEM ter responsável e prazo e DEVEM estar refletidas no Worklenz, que é a
   ferramenta obrigatória de gerenciamento (RT03).
5. Antes de considerar uma entrega pronta, DEVEM ser conferidos: testes passando,
   ausência de CPF em qualquer armazenamento, tempo de resposta e responsividade.
6. As entregas são validadas pelo patrocinador nos marcos do cronograma e registradas no
   Termo de Aceite da Entrega.

Marcos de referência: integração com a API, consulta, indicadores e gráficos em
15/10/2026; relatórios, alertas e validação dos dados em 20/10/2026; implantação e
apresentação final em 22/10/2026.

## Governança

- Esta constituição prevalece sobre qualquer outra prática do projeto. Em caso de
  conflito com os documentos aprovados pelo patrocinador (Termo de Abertura, Declaração
  do Escopo e Dicionário da EAP), valem os documentos aprovados e a constituição DEVE ser
  emendada.
- **Emendas**: toda alteração DEVE ser registrada neste arquivo, com a nova versão e a
  data, e aprovada pelo gerente do projeto em exercício. Emenda que altere escopo, prazo
  ou entrega exige também Solicitação de Mudança aprovada pelo patrocinador.
- **Versionamento**: versão semântica. MAJOR para remoção ou redefinição incompatível de
  princípio; MINOR para princípio ou seção nova, ou orientação ampliada de forma
  relevante; PATCH para esclarecimentos e correções de redação.
- **Conformidade**: todo plano e toda revisão de código DEVEM verificar o cumprimento dos
  princípios. Os princípios I e III não admitem exceção; exceções aos demais DEVEM ser
  justificadas no plano.

**Versão**: 1.1.0 | **Ratificada em**: 2026-10-13 | **Última emenda**: 2026-10-21

Histórico de emendas:
- 1.1.0 (2026-10-21): inclui o RNF04 (Modo Escuro de Alto Contraste), aprovado pela
  Solicitação de Mudança 3c; registra o front-end no mesmo repositório do back-end.
- 1.0.1 (2026-10-13): correções de redação.
