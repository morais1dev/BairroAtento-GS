package com.bairroatento.indicador.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Progresso de um encaminhamento: etapa atual, contagem de respostas por etapa e histórico. */
public record Progresso(Long encaminhamentoId, String etapaAtual,
                        Map<String, Map<String, Long>> respostasPorEtapa,
                        List<MudancaEtapa> historico) {

    public record MudancaEtapa(String etapaAnterior, String etapaNova, LocalDateTime data) {}

    /** Opções válidas de resposta de cada etapa; todas aparecem na contagem, mesmo com zero. */
    public static List<String> opcoes(Etapa etapa) {
        return etapa == Etapa.IMPACTO
                ? List.of("AJUDOU", "AJUDOU_PARCIALMENTE", "NAO_AJUDOU")
                : List.of("SIM", "NAO", "NAO_SEI");
    }

    /**
     * Monta a contagem por etapa (todas as etapas e opções, zeradas por padrão).
     * @param brutas etapa → (resposta → quantidade), como veio do banco; respostas desconhecidas são ignoradas
     */
    public static Map<String, Map<String, Long>> contagem(Map<String, Map<String, Long>> brutas) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        for (Etapa etapa : Etapa.values()) {
            Map<String, Long> porOpcao = new LinkedHashMap<>();
            Map<String, Long> origem = brutas.getOrDefault(etapa.name(), Map.of());
            for (String opcao : opcoes(etapa)) {
                porOpcao.put(opcao, origem.getOrDefault(opcao, 0L));
            }
            out.put(etapa.name(), porOpcao);
        }
        return out;
    }
}
