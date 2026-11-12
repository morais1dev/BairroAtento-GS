package com.bairroatento.etapa.domain;

import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.RegraNegocioException;

/** Recusas do motor de etapas, já mapeadas para 409 (conflito) ou 422 (regra de negócio). */
public final class EtapaException {

    private EtapaException() {}

    /** Resposta para etapa que não é a atual (pular ou voltar). 409. */
    public static class ForaDeOrdem extends ConflitoException {
        public ForaDeOrdem(Etapa respondida, Etapa atual) {
            super("Etapa " + respondida + " não é a etapa atual (" + atual + ")");
        }
    }

    /** O morador já respondeu esta etapa nesta rodada. 409. */
    public static class RespostaDuplicada extends ConflitoException {
        public RespostaDuplicada(long moradorId, Etapa etapa) {
            super("Morador " + moradorId + " já respondeu a etapa " + etapa + " nesta rodada");
        }
    }

    /** IMPACTO já apurado; não aceita mais respostas. 409. */
    public static class AndamentoConcluido extends ConflitoException {
        public AndamentoConcluido() {
            super("Encaminhamento concluído: a etapa IMPACTO já foi apurada");
        }
    }

    /** Morador não é autor nem confirmou a ocorrência. 422. */
    public static class MoradorNaoAutorizado extends RegraNegocioException {
        public MoradorNaoAutorizado(long moradorId) {
            super("Morador " + moradorId + " não registrou nem confirmou a ocorrência");
        }
    }

    /** IMPACTO respondido antes de EXECUCAO confirmada. 422. */
    public static class ImpactoAntesDaExecucao extends RegraNegocioException {
        public ImpactoAntesDaExecucao() {
            super("A etapa IMPACTO só aceita respostas depois de EXECUCAO confirmada");
        }
    }

    /** Opção que não vale para a etapa (ex.: SIM em IMPACTO). 422. */
    public static class OpcaoInvalida extends RegraNegocioException {
        public OpcaoInvalida(OpcaoResposta opcao, Etapa etapa) {
            super("Resposta " + opcao + " não é válida para a etapa " + etapa);
        }
    }
}
