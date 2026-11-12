package com.bairroatento.etapa.web;

import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.infra.RegistroEtapas.ItemHistorico;
import java.time.LocalDateTime;

/** Mudança de etapa exposta pela API (etapaAnterior é nula na criação do encaminhamento). */
public record HistoricoEtapaResponse(Etapa etapaAnterior, Etapa etapaNova, int rodada, LocalDateTime data) {

    static HistoricoEtapaResponse de(ItemHistorico i) {
        return new HistoricoEtapaResponse(i.etapaAnterior(), i.etapaNova(), i.rodada(), i.data());
    }
}
