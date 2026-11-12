CREATE TABLE morador (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    cpf_hash      VARCHAR(64)  NOT NULL,
    data_cadastro TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_morador_email UNIQUE (email)
);
