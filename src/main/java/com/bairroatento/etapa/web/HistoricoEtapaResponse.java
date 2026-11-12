package com.bairroatento.etapa.web;

import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.infra.RegistroEtapas.ItemHistorico;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Mudança de etapa exposta pela API (etapaAnterior é nula na criação do encaminhamento). */
@Schema(name = "HistoricoEtapaResponse")
public record HistoricoEtapaResponse(@Schema(nullable = true) Etapa etapaAnterior, Etapa etapaNova,
                                     @Schema(example = "1") int rodada,
                                     @Schema(example = "2026-11-12T10:20:00") LocalDateTime data) {

    static HistoricoEtapaResponse de(ItemHistorico i) {
        return new HistoricoEtapaResponse(i.etapaAnterior(), i.etapaNova(), i.rodada(), i.data());
    }
}
