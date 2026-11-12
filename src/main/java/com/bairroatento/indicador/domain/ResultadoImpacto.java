package com.bairroatento.indicador.domain;

import java.util.Map;
import java.util.Optional;

/** Resultado de um impacto fechado: a opção mais votada. */
public enum ResultadoImpacto {
    NAO_AJUDOU, AJUDOU_PARCIALMENTE, AJUDOU;

    /**
     * Opção mais votada. Sem votos → vazio. Em empate, vence a menos favorável
     * (ordem de declaração), para não superestimar o impacto positivo.
     */
    public static Optional<ResultadoImpacto> maisVotado(Map<ResultadoImpacto, Long> votos) {
        ResultadoImpacto melhor = null;
        long max = 0;
        for (ResultadoImpacto r : values()) {
            long v = votos.getOrDefault(r, 0L);
            if (v > max) {
                max = v;
                melhor = r;
            }
        }
        return Optional.ofNullable(melhor);
    }
}
