package com.bairroatento.indicador.web;

import com.bairroatento.indicador.domain.Progresso;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Progresso de um encaminhamento exposto pela API. */
@Schema(name = "ProgressoResponse")
public record ProgressoResponse(
        @Schema(example = "3") Long encaminhamentoId,
        @Schema(example = "MOBILIZACAO") String etapaAtual,
        @Schema(description = "Etapa -> (resposta -> quantidade); todas as etapas e opções aparecem, mesmo zeradas",
                example = "{\"RECEBIMENTO\":{\"SIM\":10,\"NAO\":0,\"NAO_SEI\":1},"
                        + "\"MOBILIZACAO\":{\"SIM\":2,\"NAO\":0,\"NAO_SEI\":0},"
                        + "\"EXECUCAO\":{\"SIM\":0,\"NAO\":0,\"NAO_SEI\":0},"
                        + "\"IMPACTO\":{\"AJUDOU\":0,\"AJUDOU_PARCIALMENTE\":0,\"NAO_AJUDOU\":0}}")
        Map<String, Map<String, Long>> respostasPorEtapa,
        List<MudancaEtapaResponse> historico) {

    @Schema(name = "MudancaEtapaResponse")
    public record MudancaEtapaResponse(
            @Schema(nullable = true, example = "RECEBIMENTO") String etapaAnterior,
            @Schema(example = "MOBILIZACAO") String etapaNova,
            @Schema(example = "2026-11-12T11:00:00") LocalDateTime data) {}

    static ProgressoResponse de(Progresso p) {
        return new ProgressoResponse(p.encaminhamentoId(), p.etapaAtual(), p.respostasPorEtapa(),
                p.historico().stream()
                        .map(m -> new MudancaEtapaResponse(m.etapaAnterior(), m.etapaNova(), m.data())).toList());
    }
}
