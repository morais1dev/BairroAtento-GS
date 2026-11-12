package com.bairroatento.etapa.domain;

import java.util.Objects;

/** Resposta de um morador numa etapa e rodada. */
public record Voto(long moradorId, OpcaoResposta opcao) {
    public Voto {
        Objects.requireNonNull(opcao, "opcao");
    }
}
