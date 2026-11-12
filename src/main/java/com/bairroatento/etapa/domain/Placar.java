package com.bairroatento.etapa.domain;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Contagem das respostas de uma etapa numa rodada. */
public record Placar(Map<OpcaoResposta, Integer> contagem) {

    public Placar {
        contagem = Map.copyOf(contagem);
    }

    public static Placar de(Collection<Voto> votos) {
        Map<OpcaoResposta, Integer> m = new EnumMap<>(OpcaoResposta.class);
        for (Voto v : votos) m.merge(v.opcao(), 1, Integer::sum);
        return new Placar(m);
    }

    public int de(OpcaoResposta opcao) {
        return contagem.getOrDefault(opcao, 0);
    }

    public int total() {
        return contagem.values().stream().mapToInt(Integer::intValue).sum();
    }
}
