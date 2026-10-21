-- Vínculos fictícios para os testes de consulta, indicadores, gráficos e exportação.
-- texto_busca segue NormalizadorTexto: nome, cargo e matrícula em minúsculas e sem acento.

INSERT INTO carga (origem, status, inicio, fim, desligamento_inicio, desligamento_fim,
                   total_informado, total_informado_final, total_paginas, paginas_lidas, gravados, rejeitados)
VALUES ('MANUAL', 'SUCESSO', '2026-10-15 02:00:00-03', '2026-10-15 02:15:00-03', '1900-01-01', '2026-10-15',
        9, 9, 1, 1, 9, 0);

INSERT INTO vinculo (entidade, matricula, nome, situacao, data_admissao, data_desligamento, motivo_desligamento,
                     regime, classificacao, secretaria, local_trabalho, cargo, nivel, salario_base,
                     horas_mensais_min, texto_busca, ativo_na_fonte, primeira_carga_id, ultima_carga_id)
VALUES
('MUNICÍPIO DE TESTE', '1-01', 'JOSÉ DA SILVA', 'TRABALHANDO', '2020-03-01', NULL, NULL,
 'CLT', NULL, 'SMS - SAUDE', 'UBS CENTRO', 'ENFERMEIRO', '10', 5000.00, 12000,
 'jose da silva enfermeiro 1-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '2-01', 'ANA SOUZA', 'TRABALHANDO', '2015-02-10', NULL, NULL,
 'ESTATUTARIO', NULL, 'SMED - EDUCACAO', 'EMEF A', 'PROFESSOR', '20', 4000.00, 12000,
 'ana souza professor 2-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '3-01', 'JOÃO PEREIRA', 'AFASTADO', '2010-05-05', NULL, NULL,
 'ESTATUTARIO', NULL, 'SMED - EDUCACAO', 'EMEF B', 'PROFESSOR', '20', 4100.00, 12000,
 'joao pereira professor 3-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '4-01', 'MÁRCIA LIMA', 'DESLIGADO', '2000-01-01', '2026-03-15', 'Exoneração',
 'CLT', NULL, 'SMS - SAUDE', NULL, 'ENFERMEIRO', '10', 0.00, 12000,
 'marcia lima enfermeiro 4-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '5-01', 'PEDRO ALVES', 'DESLIGADO', '1990-01-01', '2019-07-01', 'Morte',
 'CLT', 'PENSIONISTA', 'SMS - SAUDE', NULL, 'MOTORISTA', NULL, NULL, NULL,
 'pedro alves motorista 5-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '6-01', 'LÚCIA COSTA', 'TRABALHANDO', '2025-01-02', NULL, NULL,
 'CARGO COMISSAO', NULL, 'GP - GABINETE', 'GABINETE', 'ASSESSOR', NULL, 8000.00, 13200,
 'lucia costa assessor 6-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '7-01', 'CARLOS GOMES', 'TRABALHANDO', '2026-02-01', NULL, NULL,
 'CLT', 'PENSIONISTA', 'SMED - EDUCACAO', NULL, 'MERENDEIRO', '5', 2000.00, 13200,
 'carlos gomes merendeiro 7-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '8-01', 'RENATA DIAS', 'AFASTADO', '2018-08-08', NULL, NULL,
 'CLT', NULL, NULL, NULL, 'AUXILIAR', NULL, 2500.00, 13200,
 'renata dias auxiliar 8-01', true, 1, 1),
('MUNICÍPIO DE TESTE', '9-01', 'SERVIDOR ANTIGO', 'TRABALHANDO', '2005-06-06', NULL, NULL,
 'CLT', NULL, 'SMS - SAUDE', NULL, 'ENFERMEIRO', '10', 5200.00, 12000,
 'servidor antigo enfermeiro 9-01', false, 1, 1);
