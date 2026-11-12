package com.bairroatento.etapa.domain;

import java.util.Objects;

/**
 * Estado do andamento de um encaminhamento. A rodada começa em 1 e cresce a cada reabertura;
 * resultadoImpacto só é preenchido quando a etapa IMPACTO fecha, o que conclui o andamento.
 */
public record EstadoAndamento(Etapa etapaAtual, int rodada, OpcaoResposta resultadoImpacto) {

    public EstadoAndamento {
        Objects.requireNonNull(etapaAtual, "etapaAtual");
        if (rodada < 1) throw new IllegalArgumentException("rodada deve ser >= 1");
        if (resultadoImpacto != null
                && (!etapaAtual.ehImpacto() || !OpcaoResposta.DE_IMPACTO.contains(resultadoImpacto))) {
            throw new IllegalArgumentException("resultado de impacto inconsistente");
        }
    }

    public static EstadoAndamento inicial() {
        return new EstadoAndamento(Etapa.INICIAL, 1, null);
    }

    public boolean concluido() {
        return resultadoImpacto != null;
    }
}
