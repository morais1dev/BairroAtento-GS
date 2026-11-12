package com.bairroatento.indicador.web;

import com.bairroatento.indicador.domain.IndicadorImpacto;
import io.swagger.v3.oas.annotations.media.Schema;

/** Indicador de impacto exposto pela API. Percentuais de 0 a 100, com 2 casas. */
@Schema(name = "IndicadorImpactoResponse")
public record IndicadorImpactoResponse(
        @Schema(description = "Encaminhamentos no filtro", example = "12") int totalEncaminhamentos,
        @Schema(description = "Encaminhamentos com IMPACTO apurado", example = "5") int impactosFechados,
        @Schema(example = "3") int ajudou,
        @Schema(example = "1") int ajudouParcialmente,
        @Schema(example = "1") int naoAjudou,
        @Schema(description = "% de AJUDOU entre os impactos fechados", example = "60.0") double percentualAjudou,
        @Schema(description = "% de AJUDOU_PARCIALMENTE entre os fechados (não entra no percentual positivo)",
                example = "20.0") double percentualAjudouParcialmente,
        @Schema(description = "Encaminhamentos com EXECUCAO confirmada", example = "7") int execucoesConfirmadas,
        @Schema(description = "Tempo médio do envio até a execução confirmada, em segundos; nulo se não houve",
                example = "86400", nullable = true) Long tempoMedioExecucaoSegundos) {

    static IndicadorImpactoResponse de(IndicadorImpacto i) {
        return new IndicadorImpactoResponse(i.totalEncaminhamentos(), i.impactosFechados(), i.ajudou(),
                i.ajudouParcialmente(), i.naoAjudou(), i.percentualAjudou(), i.percentualAjudouParcialmente(),
                i.execucoesConfirmadas(), i.tempoMedioExecucaoSegundos());
    }
}
