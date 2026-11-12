CREATE TABLE resposta_etapa (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    encaminhamento_id BIGINT       NOT NULL,
    etapa             VARCHAR(60)  NOT NULL,
    morador_id        BIGINT       NOT NULL,
    resposta          VARCHAR(60)  NOT NULL,
    data              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_resposta_etapa UNIQUE (encaminhamento_id, etapa, morador_id),
    CONSTRAINT fk_resposta_encaminhamento FOREIGN KEY (encaminhamento_id) REFERENCES encaminhamento (id),
    CONSTRAINT fk_resposta_morador FOREIGN KEY (morador_id) REFERENCES morador (id)
);

CREATE TABLE etapa_confirmacao (
    encaminhamento_id BIGINT      NOT NULL,
    etapa             VARCHAR(60) NOT NULL,
    confirmada_em     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (encaminhamento_id, etapa),
    CONSTRAINT fk_etapa_conf_encaminhamento FOREIGN KEY (encaminhamento_id) REFERENCES encaminhamento (id)
);

CREATE TABLE historico_etapa (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    encaminhamento_id BIGINT      NOT NULL,
    etapa_anterior    VARCHAR(60),
    etapa_nova        VARCHAR(60) NOT NULL,
    data              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_encaminhamento FOREIGN KEY (encaminhamento_id) REFERENCES encaminhamento (id)
);

CREATE TABLE config_regiao (
    bairro              VARCHAR(120) PRIMARY KEY,
    minimo_confirmacoes INT NOT NULL DEFAULT 10
);
