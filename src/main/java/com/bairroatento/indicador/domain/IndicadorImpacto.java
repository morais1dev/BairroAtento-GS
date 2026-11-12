package com.bairroatento.indicador.domain;

import java.time.Duration;
import java.util.List;

/**
 * Indicador de impacto. Percentuais em 0..100, com 2 casas; AJUDOU_PARCIALMENTE
 * é contado à parte e não entra no percentual positivo.
 */
public record IndicadorImpacto(int totalEncaminhamentos, int impactosFechados, int ajudou,
                               int ajudouParcialmente, int naoAjudou,
                               double percentualAjudou, double percentualAjudouParcialmente,
                               int execucoesConfirmadas, Long tempoMedioExecucaoSegundos) {

    public static IndicadorImpacto calcular(List<EncaminhamentoResumo> itens) {
        int ajudou = 0, parcial = 0, nao = 0, execucoes = 0;
        long somaSegundos = 0;
        for (EncaminhamentoResumo e : itens) {
            if (e.impacto() != null) {
                switch (e.impacto()) {
                    case AJUDOU -> ajudou++;
                    case AJUDOU_PARCIALMENTE -> parcial++;
                    case NAO_AJUDOU -> nao++;
                }
            }
            Duration d = e.tempoAteExecucao();
            if (d != null) {
                execucoes++;
                somaSegundos += d.getSeconds();
            }
        }
        int fechados = ajudou + parcial + nao;
        return new IndicadorImpacto(itens.size(), fechados, ajudou, parcial, nao,
                percentual(ajudou, fechados), percentual(parcial, fechados),
                execucoes, execucoes == 0 ? null : Math.round((double) somaSegundos / execucoes));
    }

    private static double percentual(int parte, int total) {
        if (total == 0) return 0.0;
        return Math.round(parte * 10000.0 / total) / 100.0;
    }
}
