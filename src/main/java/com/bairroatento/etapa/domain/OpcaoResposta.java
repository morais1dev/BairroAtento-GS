package com.bairroatento.etapa.domain;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Respostas possíveis. SIM / NAO / NAO_SEI valem para RECEBIMENTO, MOBILIZACAO e EXECUCAO;
 * AJUDOU / AJUDOU_PARCIALMENTE / NAO_AJUDOU valem só para IMPACTO (declaradas da mais positiva
 * para a menos positiva).
 */
public enum OpcaoResposta {
    SIM,
    NAO,
    NAO_SEI,
    AJUDOU,
    AJUDOU_PARCIALMENTE,
    NAO_AJUDOU;

    public static final Set<OpcaoResposta> DE_CONFIRMACAO = EnumSet.of(SIM, NAO, NAO_SEI);
    public static final Set<OpcaoResposta> DE_IMPACTO = EnumSet.of(AJUDOU, AJUDOU_PARCIALMENTE, NAO_AJUDOU);

    public boolean validaPara(Etapa etapa) {
        return etapa.ehImpacto() ? DE_IMPACTO.contains(this) : DE_CONFIRMACAO.contains(this);
    }

    /** Converte sem diferenciar maiúsculas; vazio se não existir. */
    public static Optional<OpcaoResposta> de(String nome) {
        if (nome == null) return Optional.empty();
        String n = nome.trim();
        return Arrays.stream(values()).filter(o -> o.name().equalsIgnoreCase(n)).findFirst();
    }
}
