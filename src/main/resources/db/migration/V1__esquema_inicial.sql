-- Esquema inicial (specs/001-consolidacao-dados-rh/data-model.md)
-- Princípio I da constituição: nenhuma tabela tem coluna de CPF.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE carga (
    id                    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    origem                text        NOT NULL CHECK (origem IN ('AGENDADA', 'MANUAL')),
    status                text        NOT NULL CHECK (status IN ('EM_ANDAMENTO', 'SUCESSO', 'FALHA', 'INCONSISTENTE')),
    inicio                timestamptz NOT NULL DEFAULT now(),
    fim                   timestamptz,
    desligamento_inicio   date        NOT NULL,
    desligamento_fim      date        NOT NULL,
    total_informado       integer,
    total_informado_final integer,
    total_paginas         integer,
    paginas_lidas         integer     NOT NULL DEFAULT 0,
    gravados              integer     NOT NULL DEFAULT 0,
    rejeitados            integer     NOT NULL DEFAULT 0,
    novos                 integer,
    alterados             integer,
    nao_encontrados       integer,
    teto_valor            numeric(12, 2),
    teto_origem           text CHECK (teto_origem IN ('FONTE', 'MANUAL', 'SUSPENSA')),
    mensagem              text
);

-- Uma carga por vez
CREATE UNIQUE INDEX carga_uma_em_andamento ON carga (status) WHERE status = 'EM_ANDAMENTO';
CREATE INDEX carga_status_fim ON carga (status, fim DESC);

CREATE TABLE vinculo (
    id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entidade            text        NOT NULL,
    matricula           text        NOT NULL,
    nome                text,
    situacao            text        NOT NULL CHECK (situacao IN ('TRABALHANDO', 'AFASTADO', 'DESLIGADO')),
    data_admissao       date,
    data_desligamento   date,
    motivo_desligamento text,
    regime              text,
    classificacao       text,
    forma_investidura   text,
    secretaria          text,
    local_trabalho      text,
    cargo               text,
    funcao              text,
    nivel               text,
    salario_base        numeric(12, 2),
    horario             text,
    horas_mensais_min   integer,
    horas_semanais_min  integer,
    valores_originais   jsonb,
    texto_busca         text,
    ativo_na_fonte      boolean     NOT NULL DEFAULT true,
    primeira_carga_id   bigint REFERENCES carga (id),
    ultima_carga_id     bigint REFERENCES carga (id),
    visto_por_ultimo_em timestamptz,
    CONSTRAINT vinculo_entidade_matricula UNIQUE (entidade, matricula)
);

CREATE INDEX vinculo_texto_busca ON vinculo USING gin (texto_busca gin_trgm_ops);
CREATE INDEX vinculo_situacao ON vinculo (situacao);
CREATE INDEX vinculo_regime ON vinculo (regime);
CREATE INDEX vinculo_secretaria ON vinculo (secretaria);
CREATE INDEX vinculo_cargo ON vinculo (cargo);
CREATE INDEX vinculo_data_admissao ON vinculo (data_admissao);
CREATE INDEX vinculo_data_desligamento ON vinculo (data_desligamento);

-- Área de preparação: recebe a carga em andamento e não é lida pelo site
CREATE TABLE vinculo_staging (
    carga_id            bigint  NOT NULL REFERENCES carga (id),
    pagina              integer NOT NULL,
    entidade            text    NOT NULL,
    matricula           text    NOT NULL,
    nome                text,
    situacao            text    NOT NULL,
    data_admissao       date,
    data_desligamento   date,
    motivo_desligamento text,
    regime              text,
    classificacao       text,
    forma_investidura   text,
    secretaria          text,
    local_trabalho      text,
    cargo               text,
    funcao              text,
    nivel               text,
    salario_base        numeric(12, 2),
    horario             text,
    horas_mensais_min   integer,
    horas_semanais_min  integer,
    valores_originais   jsonb,
    texto_busca         text
);

CREATE INDEX vinculo_staging_chave ON vinculo_staging (carga_id, entidade, matricula);

CREATE TABLE registro_rejeitado (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carga_id       bigint  NOT NULL REFERENCES carga (id),
    pagina         integer NOT NULL,
    matricula      text,
    campo          text    NOT NULL,
    valor_recebido text,
    motivo         text    NOT NULL
);

CREATE INDEX registro_rejeitado_carga ON registro_rejeitado (carga_id);

CREATE TABLE mudanca_vinculo (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    vinculo_id     bigint      NOT NULL REFERENCES vinculo (id),
    carga_id       bigint      NOT NULL REFERENCES carga (id),
    atributo       text        NOT NULL,
    valor_anterior text,
    valor_novo     text,
    detectado_em   timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX mudanca_vinculo_vinculo ON mudanca_vinculo (vinculo_id);
CREATE INDEX mudanca_vinculo_carga ON mudanca_vinculo (carga_id);

CREATE TABLE parametro_teto (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    valor           numeric(12, 2) NOT NULL CHECK (valor > 0),
    vigencia_inicio date           NOT NULL UNIQUE,
    fundamento      text,
    criado_em       timestamptz    NOT NULL DEFAULT now()
);

CREATE TABLE alerta (
    id                   bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    carga_id             bigint         NOT NULL REFERENCES carga (id),
    vinculo_id           bigint         NOT NULL REFERENCES vinculo (id),
    regra                text           NOT NULL CHECK (regra IN ('TETO', 'FORA_PADRAO')),
    valor_vinculo        numeric(12, 2) NOT NULL,
    valor_referencia     numeric(12, 2) NOT NULL,
    diferenca            numeric(12, 2) NOT NULL,
    diferenca_percentual numeric(7, 2)  NOT NULL,
    grupo_tamanho        integer,
    CONSTRAINT alerta_vinculo_regra UNIQUE (vinculo_id, regra)
);
