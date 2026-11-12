CREATE TABLE destinatario (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(150) NOT NULL,
    tipo         VARCHAR(60)  NOT NULL,
    area_atuacao VARCHAR(150)
);

CREATE TABLE encaminhamento (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ocorrencia_id   BIGINT       NOT NULL,
    destinatario_id BIGINT       NOT NULL,
    protocolo       VARCHAR(60)  NOT NULL,
    data_envio      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    etapa_atual     VARCHAR(60)  NOT NULL,
    CONSTRAINT fk_encaminhamento_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencia (id),
    CONSTRAINT fk_encaminhamento_destinatario FOREIGN KEY (destinatario_id) REFERENCES destinatario (id)
);
