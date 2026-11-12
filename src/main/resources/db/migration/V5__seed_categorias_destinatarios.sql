INSERT INTO categoria (nome) VALUES ('buraco');
INSERT INTO categoria (nome) VALUES ('iluminação');
INSERT INTO categoria (nome) VALUES ('lixo');
INSERT INTO categoria (nome) VALUES ('calçada');
INSERT INTO categoria (nome) VALUES ('outro');

INSERT INTO destinatario (nome, tipo, area_atuacao) VALUES ('Saúde pública', 'FICTICIO', 'Saúde');
INSERT INTO destinatario (nome, tipo, area_atuacao) VALUES ('Mobilidade e transporte', 'FICTICIO', 'Mobilidade');
INSERT INTO destinatario (nome, tipo, area_atuacao) VALUES ('Vigilância sanitária', 'FICTICIO', 'Vigilância sanitária');
INSERT INTO destinatario (nome, tipo, area_atuacao) VALUES ('Outros', 'FICTICIO', 'Geral');

-- Morador fictício (CPF 969.430.400-81; só o hash SHA-256 dos 11 dígitos é armazenado)
INSERT INTO morador (nome, email, cpf_hash)
VALUES ('Morador Fictício', 'morador.ficticio@example.com', '0721fa1db6683f1d514565851b213775ee8c5a9ebe3cfd74a9722432275e0a34');
