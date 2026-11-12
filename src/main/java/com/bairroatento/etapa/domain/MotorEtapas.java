package com.bairroatento.etapa.domain;

import com.bairroatento.etapa.domain.Decisao.Desfecho;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Regras do andamento por etapas, sem dependência de banco.
 *
 * <ul>
 *   <li>Só se responde a etapa atual; outra etapa é conflito. Responder IMPACTO antes de
 *       EXECUCAO confirmada viola regra de negócio.</li>
 *   <li>Só respondem o autor da ocorrência e quem a confirmou.</li>
 *   <li>Cada morador responde uma vez por etapa por rodada.</li>
 *   <li>Confirma com SIM &gt;= mínimo e SIM &gt; NAO; reabre com NAO &gt;= mínimo e NAO &gt; SIM.
 *       NAO_SEI não conta para nenhum lado. Empate não decide.</li>
 *   <li>Reabertura volta uma etapa (RECEBIMENTO fica em RECEBIMENTO) e inicia rodada nova.</li>
 *   <li>IMPACTO fecha com o mínimo de respostas; o resultado é a opção mais votada (em empate,
 *       a menos positiva). NAO_AJUDOU não reabre nada.</li>
 * </ul>
 */
public final class MotorEtapas {

    private MotorEtapas() {}

    /**
     * Aplica uma resposta e devolve a decisão; não altera nada fora do retorno.
     *
     * @param estado        estado atual do andamento
     * @param votosDaRodada respostas já dadas na etapa atual e na rodada atual
     * @param etapa         etapa que o morador está respondendo
     * @param voto          a resposta
     * @param autorizados   autor da ocorrência e moradores que a confirmaram
     * @param minimo        mínimo de respostas configurado para o bairro (&gt;= 1)
     */
    public static Decisao responder(EstadoAndamento estado, Collection<Voto> votosDaRodada, Etapa etapa,
                                    Voto voto, Set<Long> autorizados, int minimo) {
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(votosDaRodada, "votosDaRodada");
        Objects.requireNonNull(etapa, "etapa");
        Objects.requireNonNull(voto, "voto");
        Objects.requireNonNull(autorizados, "autorizados");
        if (minimo < 1) throw new IllegalArgumentException("mínimo de respostas deve ser >= 1");

        if (!autorizados.contains(voto.moradorId())) {
            throw new EtapaException.MoradorNaoAutorizado(voto.moradorId());
        }
        if (estado.concluido()) {
            throw new EtapaException.AndamentoConcluido();
        }
        if (etapa.ehImpacto() && !estado.etapaAtual().ehImpacto()) {
            throw new EtapaException.ImpactoAntesDaExecucao();
        }
        if (etapa != estado.etapaAtual()) {
            throw new EtapaException.ForaDeOrdem(etapa, estado.etapaAtual());
        }
        if (!voto.opcao().validaPara(etapa)) {
            throw new EtapaException.OpcaoInvalida(voto.opcao(), etapa);
        }
        if (votosDaRodada.stream().anyMatch(v -> v.moradorId() == voto.moradorId())) {
            throw new EtapaException.RespostaDuplicada(voto.moradorId(), etapa);
        }

        List<Voto> votos = new ArrayList<>(votosDaRodada);
        votos.add(voto);
        Placar placar = Placar.de(votos);

        Desfecho desfecho = Desfecho.PENDENTE;
        EstadoAndamento novo = estado;
        if (etapa.ehImpacto()) {
            if (placar.total() >= minimo) {
                desfecho = Desfecho.CONCLUIDA;
                novo = new EstadoAndamento(etapa, estado.rodada(), maisVotado(placar));
            }
        } else {
            int sim = placar.de(OpcaoResposta.SIM);
            int nao = placar.de(OpcaoResposta.NAO);
            if (sim >= minimo && sim > nao) {
                desfecho = Desfecho.CONFIRMADA;
                novo = new EstadoAndamento(etapa.proxima().orElseThrow(), estado.rodada(), null);
            } else if (nao >= minimo && nao > sim) {
                desfecho = Desfecho.REABERTA;
                novo = new EstadoAndamento(etapa.destinoDaReabertura(), estado.rodada() + 1, null);
            }
        }
        return new Decisao(voto, etapa, estado.rodada(), desfecho, estado, novo, placar);
    }

    /** Opção de IMPACTO mais votada; em empate vence a menos positiva. */
    static OpcaoResposta maisVotado(Placar placar) {
        return OpcaoResposta.DE_IMPACTO.stream()
                .max(Comparator.<OpcaoResposta>comparingInt(o -> placar.de(o)).thenComparingInt(Enum::ordinal))
                .orElseThrow();
    }
}
