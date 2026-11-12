package com.bairroatento.encaminhamento.domain;

/** Gera o protocolo fictício de cada encaminhamento. */
public interface GeradorProtocolo {
    String gerar(Long ocorrenciaId, Long destinatarioId);
}
