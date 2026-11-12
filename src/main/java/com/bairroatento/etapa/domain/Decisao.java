package com.bairroatento.etapa.domain;

/**
 * Resultado de aplicar uma resposta ao andamento.
 *
 * @param voto             a resposta aceita
 * @param etapaRespondida  etapa à qual a resposta pertence
 * @param rodadaDaResposta rodada em que a resposta deve ser guardada
 * @param desfecho         o que a resposta provocou
 * @param anterior         estado antes da resposta
 * @param novo             estado depois da resposta
 * @param placar           contagem da etapa/rodada já incluindo esta resposta
 */
public record Decisao(Voto voto, Etapa etapaRespondida, int rodadaDaResposta, Desfecho desfecho,
                      EstadoAndamento anterior, EstadoAndamento novo, Placar placar) {

    public enum Desfecho {
        /** Resposta registrada sem atingir nenhum limite. */
        PENDENTE,
        /** Etapa confirmada; o andamento avançou para a próxima. */
        CONFIRMADA,
        /** Etapa reaberta; voltou para a anterior (ou ficou em RECEBIMENTO) com rodada nova. */
        REABERTA,
        /** IMPACTO apurado; o andamento está concluído. */
        CONCLUIDA
    }

    /** Se houve mudança de etapa a registrar no histórico (confirmação ou reabertura). */
    public boolean gerouTransicao() {
        return desfecho == Desfecho.CONFIRMADA || desfecho == Desfecho.REABERTA;
    }
}
