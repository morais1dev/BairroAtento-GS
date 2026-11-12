package com.bairroatento.indicador.domain;

import java.time.Duration;

/**
 * Dados mínimos de um encaminhamento para o cálculo.
 * @param impacto resultado do impacto fechado, ou null se o impacto ainda não foi fechado
 * @param tempoAteExecucao tempo entre o envio e a confirmação de EXECUCAO, ou null se não confirmada
 */
public record EncaminhamentoResumo(ResultadoImpacto impacto, Duration tempoAteExecucao) {}
