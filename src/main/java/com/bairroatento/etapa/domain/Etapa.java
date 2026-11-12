package com.bairroatento.etapa.domain;

import java.util.Arrays;
import java.util.Optional;

/** Etapas do andamento de um encaminhamento, na ordem em que precisam ser confirmadas. */
public enum Etapa {
    RECEBIMENTO,
    MOBILIZACAO,
    EXECUCAO,
    IMPACTO;

    public static final Etapa INICIAL = RECEBIMENTO;

    /** Etapa seguinte; vazio para IMPACTO, que é a última. */
    public Optional<Etapa> proxima() {
        int i = ordinal() + 1;
        return i < values().length ? Optional.of(values()[i]) : Optional.empty();
    }

    /** Etapa para a qual uma reabertura volta: a anterior, ou a própria RECEBIMENTO. */
    public Etapa destinoDaReabertura() {
        return this == INICIAL ? this : values()[ordinal() - 1];
    }

    public boolean ehImpacto() {
        return this == IMPACTO;
    }

    /** Converte sem diferenciar maiúsculas; vazio se não existir. */
    public static Optional<Etapa> de(String nome) {
        if (nome == null) return Optional.empty();
        String n = nome.trim();
        return Arrays.stream(values()).filter(e -> e.name().equalsIgnoreCase(n)).findFirst();
    }
}
