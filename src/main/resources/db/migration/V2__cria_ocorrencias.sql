CREATE TABLE categoria (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE TABLE ocorrencia (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria             VARCHAR(100)  NOT NULL,
    descricao             VARCHAR(2000) NOT NULL,
    endereco              VARCHAR(255)  NOT NULL,
    latitude              DOUBLE,
    longitude             DOUBLE,
    bairro                VARCHAR(120)  NOT NULL,
    status                VARCHAR(40)   NOT NULL,
    autor_id              BIGINT        NOT NULL,
    data_criacao          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    contador_confirmacoes INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_ocorrencia_autor FOREIGN KEY (autor_id) REFERENCES morador (id)
);

CREATE TABLE confirmacao_ocorrencia (
    ocorrencia_id BIGINT    NOT NULL,
    morador_id    BIGINT    NOT NULL,
    data          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_confirmacao UNIQUE (ocorrencia_id, morador_id),
    CONSTRAINT fk_confirmacao_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencia (id),
    CONSTRAINT fk_confirmacao_morador FOREIGN KEY (morador_id) REFERENCES morador (id)
);
