-- Andamento por rodadas (F04/F05): cada reabertura inicia uma rodada nova no encaminhamento.
ALTER TABLE encaminhamento ADD COLUMN rodada_atual INT NOT NULL DEFAULT 1;
-- Resultado apurado da etapa IMPACTO (AJUDOU / AJUDOU_PARCIALMENTE / NAO_AJUDOU), usado pelo F07.
ALTER TABLE encaminhamento ADD COLUMN resultado_impacto VARCHAR(40);
ALTER TABLE encaminhamento ADD COLUMN data_conclusao TIMESTAMP;
ALTER TABLE encaminhamento ADD CONSTRAINT uk_encaminhamento_protocolo UNIQUE (protocolo);
ALTER TABLE encaminhamento ADD CONSTRAINT uk_encaminhamento_ocorrencia_destinatario
    UNIQUE (ocorrencia_id, destinatario_id);

-- Respostas antigas ficam guardadas; a unicidade passa a ser por rodada.
ALTER TABLE resposta_etapa ADD COLUMN rodada INT NOT NULL DEFAULT 1;
ALTER TABLE resposta_etapa DROP CONSTRAINT uk_resposta_etapa;
ALTER TABLE resposta_etapa ADD CONSTRAINT uk_resposta_etapa_rodada
    UNIQUE (encaminhamento_id, etapa, rodada, morador_id);

-- Uma etapa pode ser confirmada de novo depois de uma reabertura (em outra rodada).
ALTER TABLE etapa_confirmacao ADD COLUMN rodada INT NOT NULL DEFAULT 1;
ALTER TABLE etapa_confirmacao DROP PRIMARY KEY;
ALTER TABLE etapa_confirmacao ADD CONSTRAINT pk_etapa_confirmacao PRIMARY KEY (encaminhamento_id, etapa, rodada);

-- Rodada em vigor depois da mudança de etapa.
ALTER TABLE historico_etapa ADD COLUMN rodada INT NOT NULL DEFAULT 1;
