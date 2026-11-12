package com.bairroatento.indicador.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IndicadorImpactoTest {

    private static EncaminhamentoResumo e(ResultadoImpacto r, Integer horas) {
        return new EncaminhamentoResumo(r, horas == null ? null : Duration.ofHours(horas));
    }

    @Test
    void percentualEParcialSeparado() {
        // 2 AJUDOU, 1 PARCIAL, 1 NAO entre 4 fechados; 1 ainda sem impacto
        var ind = IndicadorImpacto.calcular(List.of(
                e(ResultadoImpacto.AJUDOU, 10), e(ResultadoImpacto.AJUDOU, 20),
                e(ResultadoImpacto.AJUDOU_PARCIALMENTE, 30), e(ResultadoImpacto.NAO_AJUDOU, null),
                e(null, null)));
        assertEquals(5, ind.totalEncaminhamentos());
        assertEquals(4, ind.impactosFechados());
        assertEquals(50.0, ind.percentualAjudou());
        assertEquals(25.0, ind.percentualAjudouParcialmente());
        assertEquals(1, ind.ajudouParcialmente());
        assertEquals(3, ind.execucoesConfirmadas());
        assertEquals(20 * 3600L, ind.tempoMedioExecucaoSegundos());
    }

    @Test
    void parcialNaoEntraNoPositivo() {
        var ind = IndicadorImpacto.calcular(List.of(e(ResultadoImpacto.AJUDOU_PARCIALMENTE, null)));
        assertEquals(0.0, ind.percentualAjudou());
        assertEquals(100.0, ind.percentualAjudouParcialmente());
    }

    @Test
    void arredondaDuasCasas() {
        var ind = IndicadorImpacto.calcular(List.of(e(ResultadoImpacto.AJUDOU, null),
                e(ResultadoImpacto.NAO_AJUDOU, null), e(ResultadoImpacto.NAO_AJUDOU, null)));
        assertEquals(33.33, ind.percentualAjudou());
    }

    @Test
    void bairroVazioZerado() {
        var ind = IndicadorImpacto.calcular(List.of());
        assertEquals(0, ind.totalEncaminhamentos());
        assertEquals(0, ind.impactosFechados());
        assertEquals(0.0, ind.percentualAjudou());
        assertEquals(0.0, ind.percentualAjudouParcialmente());
        assertNull(ind.tempoMedioExecucaoSegundos());
    }

    @Test
    void semImpactoFechadoNaoDividePorZero() {
        var ind = IndicadorImpacto.calcular(List.of(e(null, 5)));
        assertEquals(0.0, ind.percentualAjudou());
        assertEquals(5 * 3600L, ind.tempoMedioExecucaoSegundos());
    }

    @Test
    void contagemDoProgressoPreencheZeros() {
        var c = Progresso.contagem(Map.of("EXECUCAO", Map.of("SIM", 3L, "XPTO", 9L)));
        assertEquals(3L, c.get("EXECUCAO").get("SIM"));
        assertEquals(0L, c.get("EXECUCAO").get("NAO"));
        assertEquals(false, c.get("EXECUCAO").containsKey("XPTO"));
        assertEquals(0L, c.get("IMPACTO").get("AJUDOU_PARCIALMENTE"));
        assertEquals(4, c.size());
    }
}
