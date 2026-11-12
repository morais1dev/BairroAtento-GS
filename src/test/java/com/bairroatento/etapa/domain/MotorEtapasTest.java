package com.bairroatento.etapa.domain;

import static com.bairroatento.etapa.domain.Etapa.EXECUCAO;
import static com.bairroatento.etapa.domain.Etapa.IMPACTO;
import static com.bairroatento.etapa.domain.Etapa.MOBILIZACAO;
import static com.bairroatento.etapa.domain.Etapa.RECEBIMENTO;
import static com.bairroatento.etapa.domain.OpcaoResposta.AJUDOU;
import static com.bairroatento.etapa.domain.OpcaoResposta.AJUDOU_PARCIALMENTE;
import static com.bairroatento.etapa.domain.OpcaoResposta.NAO;
import static com.bairroatento.etapa.domain.OpcaoResposta.NAO_AJUDOU;
import static com.bairroatento.etapa.domain.OpcaoResposta.NAO_SEI;
import static com.bairroatento.etapa.domain.OpcaoResposta.SIM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bairroatento.etapa.domain.Decisao.Desfecho;
import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class MotorEtapasTest {

    /** Moradores 1..20 autorizados (autor + confirmadores); 99 não é. */
    static final Set<Long> AUTORIZADOS = LongStream.rangeClosed(1, 20).boxed().collect(Collectors.toSet());
    static final long ESTRANHO = 99;

    /**
     * Simula a persistência: guarda estado e respostas por (etapa, rodada) e só repassa ao motor as
     * respostas da etapa/rodada atuais, como faz o serviço.
     */
    static class Andamento {
        final int minimo;
        EstadoAndamento estado = EstadoAndamento.inicial();
        final Map<String, List<Voto>> respostas = new HashMap<>();
        final List<Decisao> transicoes = new ArrayList<>();

        Andamento(int minimo) {
            this.minimo = minimo;
        }

        Decisao responder(long morador, Etapa etapa, OpcaoResposta opcao) {
            Decisao d = MotorEtapas.responder(estado, votosAtuais(), etapa, new Voto(morador, opcao),
                    AUTORIZADOS, minimo);
            respostas.computeIfAbsent(chave(d.etapaRespondida(), d.rodadaDaResposta()), k -> new ArrayList<>())
                    .add(d.voto());
            if (d.gerouTransicao()) transicoes.add(d);
            estado = d.novo();
            return d;
        }

        Decisao responder(long morador, OpcaoResposta opcao) {
            return responder(morador, estado.etapaAtual(), opcao);
        }

        /** Moradores first..first+n-1 respondem a etapa atual com a mesma opção. */
        Decisao lote(long first, int n, OpcaoResposta opcao) {
            Decisao ultima = null;
            for (long m = first; m < first + n; m++) ultima = responder(m, opcao);
            return ultima;
        }

        /** Confirma a etapa atual com o mínimo exato de SIM. */
        Decisao confirmar() {
            return lote(1, minimo, SIM);
        }

        List<Voto> votosAtuais() {
            return respostas.getOrDefault(chave(estado.etapaAtual(), estado.rodada()), List.of());
        }

        List<Voto> votos(Etapa etapa, int rodada) {
            return respostas.getOrDefault(chave(etapa, rodada), List.of());
        }

        static String chave(Etapa e, int rodada) {
            return e + "#" + rodada;
        }
    }

    // ------------------------------------------------------------------ ordem

    @Nested
    class Ordem {

        @Test
        void comecaEmRecebimentoNaRodadaUm() {
            EstadoAndamento e = EstadoAndamento.inicial();
            assertThat(e.etapaAtual()).isEqualTo(RECEBIMENTO);
            assertThat(e.rodada()).isEqualTo(1);
            assertThat(e.concluido()).isFalse();
        }

        @Test
        void percorreAsEtapasNaOrdemAteConcluir() {
            Andamento a = new Andamento(3);
            assertThat(a.confirmar().novo().etapaAtual()).isEqualTo(MOBILIZACAO);
            assertThat(a.confirmar().novo().etapaAtual()).isEqualTo(EXECUCAO);
            assertThat(a.confirmar().novo().etapaAtual()).isEqualTo(IMPACTO);
            Decisao fim = a.lote(1, 3, AJUDOU);
            assertThat(fim.desfecho()).isEqualTo(Desfecho.CONCLUIDA);
            assertThat(fim.novo().resultadoImpacto()).isEqualTo(AJUDOU);
            assertThat(a.estado.rodada()).isEqualTo(1);
            assertThat(a.transicoes).extracting(d -> d.anterior().etapaAtual() + ">" + d.novo().etapaAtual())
                    .containsExactly("RECEBIMENTO>MOBILIZACAO", "MOBILIZACAO>EXECUCAO", "EXECUCAO>IMPACTO");
        }

        @ParameterizedTest
        @EnumSource(value = Etapa.class, names = {"MOBILIZACAO", "EXECUCAO"})
        void pularEtapaEhConflito(Etapa futura) {
            Andamento a = new Andamento(3);
            assertThatThrownBy(() -> a.responder(1, futura, SIM))
                    .isInstanceOf(EtapaException.ForaDeOrdem.class)
                    .isInstanceOf(ConflitoException.class);
            assertThat(a.estado).isEqualTo(EstadoAndamento.inicial());
        }

        @Test
        void responderEtapaJaConfirmadaEhConflito() {
            Andamento a = new Andamento(2);
            a.confirmar();
            a.confirmar(); // agora em EXECUCAO
            assertThatThrownBy(() -> a.responder(5, RECEBIMENTO, SIM)).isInstanceOf(EtapaException.ForaDeOrdem.class);
            assertThatThrownBy(() -> a.responder(5, MOBILIZACAO, NAO)).isInstanceOf(EtapaException.ForaDeOrdem.class);
        }

        @Test
        void foraDeOrdemComRespostaDeImpactoNaoMascaraOrdem() {
            Andamento a = new Andamento(2);
            a.confirmar(); // MOBILIZACAO
            assertThatThrownBy(() -> a.responder(1, EXECUCAO, SIM)).isInstanceOf(EtapaException.ForaDeOrdem.class);
        }

        @Test
        void proximaEDestinoDaReabertura() {
            assertThat(RECEBIMENTO.proxima()).contains(MOBILIZACAO);
            assertThat(EXECUCAO.proxima()).contains(IMPACTO);
            assertThat(IMPACTO.proxima()).isEmpty();
            assertThat(RECEBIMENTO.destinoDaReabertura()).isEqualTo(RECEBIMENTO);
            assertThat(MOBILIZACAO.destinoDaReabertura()).isEqualTo(RECEBIMENTO);
            assertThat(EXECUCAO.destinoDaReabertura()).isEqualTo(MOBILIZACAO);
        }

        @Test
        void conversaoDeNomesIgnoraCaixa() {
            assertThat(Etapa.de("mobilizacao")).contains(MOBILIZACAO);
            assertThat(Etapa.de(" Impacto ")).contains(IMPACTO);
            assertThat(Etapa.de("xyz")).isEmpty();
            assertThat(Etapa.de(null)).isEmpty();
            assertThat(OpcaoResposta.de("nao_sei")).contains(NAO_SEI);
            assertThat(OpcaoResposta.de("talvez")).isEmpty();
        }
    }

    // ------------------------------------------------------------------ limite configurável

    @Nested
    class Limite {

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 5, 10, 15})
        void confirmaExatamenteNoMinimoConfigurado(int minimo) {
            Andamento a = new Andamento(minimo);
            for (long m = 1; m < minimo; m++) {
                Decisao d = a.responder(m, SIM);
                assertThat(d.desfecho()).as("resposta %d de %d", m, minimo).isEqualTo(Desfecho.PENDENTE);
                assertThat(d.novo().etapaAtual()).isEqualTo(RECEBIMENTO);
            }
            Decisao d = a.responder(minimo, SIM);
            assertThat(d.desfecho()).isEqualTo(Desfecho.CONFIRMADA);
            assertThat(d.novo().etapaAtual()).isEqualTo(MOBILIZACAO);
            assertThat(d.placar().de(SIM)).isEqualTo(minimo);
        }

        @Test
        void mesmasRespostasDecidemDiferenteConformeOMinimo() {
            Andamento baixo = new Andamento(3);
            Andamento alto = new Andamento(10);
            for (Andamento a : List.of(baixo, alto)) a.lote(1, 3, SIM);
            assertThat(baixo.estado.etapaAtual()).isEqualTo(MOBILIZACAO);
            assertThat(alto.estado.etapaAtual()).isEqualTo(RECEBIMENTO);
        }

        @Test
        void simNoMinimoMasSemMaioriaNaoConfirma() {
            // SIM 3 (>= mínimo) mas NAO 4: não confirma; e NAO 4 > SIM 3 com mínimo 5 também não reabre
            List<Voto> votos = List.of(new Voto(1, SIM), new Voto(2, SIM), new Voto(3, NAO), new Voto(4, NAO),
                    new Voto(5, NAO), new Voto(6, NAO));
            Decisao d = MotorEtapas.responder(EstadoAndamento.inicial(), votos, RECEBIMENTO, new Voto(7, SIM),
                    AUTORIZADOS, 3);
            assertThat(d.desfecho()).isEqualTo(Desfecho.REABERTA); // NAO 4 >= 3 e NAO > SIM
            Decisao e = MotorEtapas.responder(EstadoAndamento.inicial(), votos, RECEBIMENTO, new Voto(7, SIM),
                    AUTORIZADOS, 5);
            assertThat(e.desfecho()).isEqualTo(Desfecho.PENDENTE);
        }

        @Test
        void naoNoMinimoMasSemMaioriaNaoReabre() {
            List<Voto> votos = List.of(new Voto(1, NAO), new Voto(2, NAO), new Voto(3, SIM), new Voto(4, SIM),
                    new Voto(5, SIM), new Voto(6, SIM));
            Decisao d = MotorEtapas.responder(EstadoAndamento.inicial(), votos, RECEBIMENTO, new Voto(7, NAO),
                    AUTORIZADOS, 5); // NAO 3 x SIM 4, mínimo 5
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);
        }

        @Test
        void naoSeiNaoContaParaNenhumLado() {
            Andamento a = new Andamento(2);
            Decisao d = a.lote(1, 10, NAO_SEI);
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);
            assertThat(d.placar().de(NAO_SEI)).isEqualTo(10);
            assertThat(a.responder(11, SIM).desfecho()).isEqualTo(Desfecho.PENDENTE);
            assertThat(a.responder(12, SIM).desfecho()).isEqualTo(Desfecho.CONFIRMADA);
        }

        @Test
        void confirmaComMaioriaMesmoHavendoNao() {
            Andamento a = new Andamento(2);
            a.responder(1, NAO);
            a.responder(2, SIM);
            a.responder(3, SIM); // SIM 2 x NAO 1 -> confirma
            assertThat(a.estado.etapaAtual()).isEqualTo(MOBILIZACAO);

            Andamento b = new Andamento(3);
            b.lote(1, 2, NAO);
            b.lote(3, 2, SIM);
            Decisao d = b.responder(5, SIM); // SIM 3 x NAO 2
            assertThat(d.desfecho()).isEqualTo(Desfecho.CONFIRMADA);
        }

        @Test
        void naoAtingeMinimoMasMaiorQueSimNaoReabre() {
            Andamento a = new Andamento(3);
            a.lote(1, 2, NAO);
            Decisao d = a.responder(3, NAO_SEI);
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);
            assertThat(a.estado.rodada()).isEqualTo(1);
        }

        @Test
        void minimoInvalidoEhRecusado() {
            assertThatThrownBy(() -> MotorEtapas.responder(EstadoAndamento.inicial(), List.of(), RECEBIMENTO,
                    new Voto(1, SIM), AUTORIZADOS, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ------------------------------------------------------------------ empate

    @Nested
    class Empate {

        @Test
        void empateAbaixoDoMinimoNaoDecideEODesempateSim() {
            Andamento a = new Andamento(3);
            a.responder(1, SIM);
            a.responder(2, NAO);
            a.responder(3, SIM);
            a.responder(4, NAO);
            Decisao d = a.responder(5, NAO_SEI);
            assertThat(d.placar().de(SIM)).isEqualTo(2);
            assertThat(d.placar().de(NAO)).isEqualTo(2);
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);
            // terceiro SIM: SIM 3 x NAO 2 -> confirma
            assertThat(a.responder(6, SIM).desfecho()).isEqualTo(Desfecho.CONFIRMADA);
        }

        @Test
        void empateComAmbosNoMinimoNaoDecide() {
            // Placar sintético (com respostas sequenciais um lado decidiria antes): 3 SIM e 2 NAO
            // já registrados; o terceiro NAO empata em 3 x 3 com ambos no mínimo.
            List<Voto> votos = List.of(new Voto(1, SIM), new Voto(2, SIM), new Voto(3, SIM),
                    new Voto(4, NAO), new Voto(5, NAO));
            Decisao d = MotorEtapas.responder(EstadoAndamento.inicial(), votos, RECEBIMENTO, new Voto(6, NAO),
                    AUTORIZADOS, 3);
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);
            assertThat(d.novo()).isEqualTo(EstadoAndamento.inicial());
            assertThat(d.placar().de(SIM)).isEqualTo(3);
            assertThat(d.placar().de(NAO)).isEqualTo(3);
        }

        @Test
        void desempateDepoisDoEmpateDecide() {
            List<Voto> votos = List.of(new Voto(1, SIM), new Voto(2, SIM), new Voto(3, SIM),
                    new Voto(4, NAO), new Voto(5, NAO), new Voto(6, NAO));
            Decisao reabre = MotorEtapas.responder(new EstadoAndamento(MOBILIZACAO, 1, null), votos, MOBILIZACAO,
                    new Voto(7, NAO), AUTORIZADOS, 3);
            assertThat(reabre.desfecho()).isEqualTo(Desfecho.REABERTA);
            assertThat(reabre.novo()).isEqualTo(new EstadoAndamento(RECEBIMENTO, 2, null));

            Decisao confirma = MotorEtapas.responder(new EstadoAndamento(MOBILIZACAO, 1, null), votos, MOBILIZACAO,
                    new Voto(7, SIM), AUTORIZADOS, 3);
            assertThat(confirma.desfecho()).isEqualTo(Desfecho.CONFIRMADA);
            assertThat(confirma.novo().etapaAtual()).isEqualTo(EXECUCAO);
        }
    }

    // ------------------------------------------------------------------ reabertura e rodada nova

    @Nested
    class Reabertura {

        @Test
        void naoRecebiMantemRecebimentoEAbreRodadaNova() {
            Andamento a = new Andamento(2);
            a.responder(1, NAO);
            Decisao d = a.responder(2, NAO);
            assertThat(d.desfecho()).isEqualTo(Desfecho.REABERTA);
            assertThat(d.rodadaDaResposta()).isEqualTo(1);
            assertThat(d.novo()).isEqualTo(new EstadoAndamento(RECEBIMENTO, 2, null));
            assertThat(d.gerouTransicao()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = Etapa.class, names = {"MOBILIZACAO", "EXECUCAO"})
        void reaberturaVoltaParaAEtapaAnterior(Etapa etapa) {
            Andamento a = new Andamento(2);
            while (a.estado.etapaAtual() != etapa) a.confirmar();
            Decisao d = a.lote(1, 2, NAO);
            assertThat(d.desfecho()).isEqualTo(Desfecho.REABERTA);
            assertThat(d.anterior().etapaAtual()).isEqualTo(etapa);
            assertThat(d.novo().etapaAtual()).isEqualTo(etapa.destinoDaReabertura());
            assertThat(d.novo().rodada()).isEqualTo(2);
        }

        @Test
        void rodadaNovaZeraContagemEPermiteResponderDeNovo() {
            Andamento a = new Andamento(3);
            a.confirmar();               // MOBILIZACAO, rodada 1
            a.lote(1, 3, NAO);           // reabre -> RECEBIMENTO, rodada 2
            assertThat(a.estado).isEqualTo(new EstadoAndamento(RECEBIMENTO, 2, null));
            assertThat(a.votosAtuais()).isEmpty();

            // quem já respondeu RECEBIMENTO na rodada 1 responde de novo na rodada 2
            Decisao d = a.responder(1, SIM);
            assertThat(d.rodadaDaResposta()).isEqualTo(2);
            assertThat(d.placar().total()).isEqualTo(1);
            assertThat(d.desfecho()).isEqualTo(Desfecho.PENDENTE);

            // respostas antigas continuam guardadas
            assertThat(a.votos(RECEBIMENTO, 1)).hasSize(3);
            assertThat(a.votos(MOBILIZACAO, 1)).hasSize(3).allMatch(v -> v.opcao() == NAO);
            assertThat(a.votos(RECEBIMENTO, 2)).hasSize(1);
        }

        @Test
        void etapaReconfirmadaNaRodadaNovaSegueAdiante() {
            Andamento a = new Andamento(2);
            a.confirmar();          // MOBILIZACAO r1
            a.confirmar();          // EXECUCAO r1
            a.lote(1, 2, NAO);      // reabre -> MOBILIZACAO r2
            assertThat(a.estado).isEqualTo(new EstadoAndamento(MOBILIZACAO, 2, null));
            Decisao d = a.confirmar(); // mesmos moradores confirmam MOBILIZACAO de novo
            assertThat(d.rodadaDaResposta()).isEqualTo(2);
            assertThat(d.novo()).isEqualTo(new EstadoAndamento(EXECUCAO, 2, null));
        }

        @Test
        void reaberturasSucessivasIncrementamARodada() {
            Andamento a = new Andamento(1);
            for (int i = 1; i <= 4; i++) {
                Decisao d = a.responder(1, NAO);
                assertThat(d.desfecho()).isEqualTo(Desfecho.REABERTA);
                assertThat(d.novo().rodada()).isEqualTo(i + 1);
            }
            assertThat(a.estado.etapaAtual()).isEqualTo(RECEBIMENTO);
        }

        @Test
        void historicoDeTransicoesComReabertura() {
            Andamento a = new Andamento(1);
            a.responder(1, SIM);   // REC -> MOB
            a.responder(1, SIM);   // MOB -> EXE
            a.responder(1, NAO);   // EXE -> MOB (r2)
            a.responder(2, NAO);   // MOB -> REC (r3)
            assertThat(a.transicoes).extracting(d -> d.anterior().etapaAtual() + ">" + d.novo().etapaAtual()
                            + "@" + d.novo().rodada())
                    .containsExactly("RECEBIMENTO>MOBILIZACAO@1", "MOBILIZACAO>EXECUCAO@1",
                            "EXECUCAO>MOBILIZACAO@2", "MOBILIZACAO>RECEBIMENTO@3");
        }
    }

    // ------------------------------------------------------------------ duplicada / autorização

    @Nested
    class Recusas {

        @Test
        void respostaDuplicadaNaMesmaRodadaEhConflito() {
            Andamento a = new Andamento(5);
            a.responder(1, SIM);
            assertThatThrownBy(() -> a.responder(1, SIM))
                    .isInstanceOf(EtapaException.RespostaDuplicada.class)
                    .isInstanceOf(ConflitoException.class);
            assertThatThrownBy(() -> a.responder(1, NAO)).isInstanceOf(EtapaException.RespostaDuplicada.class);
            assertThat(a.votosAtuais()).hasSize(1);
        }

        @Test
        void mesmoMoradorPodeResponderEtapasDiferentes() {
            Andamento a = new Andamento(1);
            a.responder(1, SIM);
            assertThat(a.responder(1, SIM).etapaRespondida()).isEqualTo(MOBILIZACAO);
        }

        @Test
        void moradorQueNaoRegistrouNemConfirmouEhRecusado() {
            Andamento a = new Andamento(1);
            assertThatThrownBy(() -> a.responder(ESTRANHO, SIM))
                    .isInstanceOf(EtapaException.MoradorNaoAutorizado.class)
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(a.votosAtuais()).isEmpty();
        }

        @Test
        void autorizacaoVemAntesDasDemaisRecusas() {
            Andamento a = new Andamento(5);
            assertThatThrownBy(() -> a.responder(ESTRANHO, EXECUCAO, SIM))
                    .isInstanceOf(EtapaException.MoradorNaoAutorizado.class);
        }

        @Test
        void opcaoDeImpactoEmEtapaDeConfirmacaoEhRecusada() {
            Andamento a = new Andamento(1);
            assertThatThrownBy(() -> a.responder(1, AJUDOU))
                    .isInstanceOf(EtapaException.OpcaoInvalida.class)
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void opcaoDeConfirmacaoEmImpactoEhRecusada() {
            EstadoAndamento impacto = new EstadoAndamento(IMPACTO, 1, null);
            for (OpcaoResposta o : List.of(SIM, NAO, NAO_SEI)) {
                assertThatThrownBy(() -> MotorEtapas.responder(impacto, List.of(), IMPACTO, new Voto(1, o),
                        AUTORIZADOS, 1)).isInstanceOf(EtapaException.OpcaoInvalida.class);
            }
        }
    }

    // ------------------------------------------------------------------ impacto

    @Nested
    class Impacto {

        Andamento emImpacto(int minimo) {
            Andamento a = new Andamento(minimo);
            a.confirmar();
            a.confirmar();
            a.confirmar();
            assertThat(a.estado.etapaAtual()).isEqualTo(IMPACTO);
            return a;
        }

        @ParameterizedTest
        @EnumSource(value = Etapa.class, names = {"RECEBIMENTO", "MOBILIZACAO", "EXECUCAO"})
        void impactoAntesDaExecucaoConfirmadaEhRegraDeNegocio(Etapa atual) {
            Andamento a = new Andamento(2);
            while (a.estado.etapaAtual() != atual) a.confirmar();
            assertThatThrownBy(() -> a.responder(1, IMPACTO, AJUDOU))
                    .isInstanceOf(EtapaException.ImpactoAntesDaExecucao.class)
                    .isInstanceOf(RegraNegocioException.class);
        }

        @Test
        void impactoFechaComMinimoDeRespostasEGuardaOMaisVotado() {
            Andamento a = emImpacto(5);
            a.responder(1, AJUDOU);
            a.responder(2, AJUDOU_PARCIALMENTE);
            a.responder(3, AJUDOU_PARCIALMENTE);
            Decisao pendente = a.responder(4, NAO_AJUDOU);
            assertThat(pendente.desfecho()).isEqualTo(Desfecho.PENDENTE);
            assertThat(pendente.novo().concluido()).isFalse();
            Decisao fim = a.responder(5, AJUDOU);
            assertThat(fim.desfecho()).isEqualTo(Desfecho.CONCLUIDA);
            // AJUDOU 2 x PARCIAL 2 x NAO_AJUDOU 1: empate, vence a menos positiva
            assertThat(fim.novo().resultadoImpacto()).isEqualTo(AJUDOU_PARCIALMENTE);
            assertThat(fim.gerouTransicao()).isFalse();
        }

        @Test
        void naoAjudouMaisVotadoNaoReabreNada() {
            Andamento a = emImpacto(3);
            Decisao d = a.lote(1, 3, NAO_AJUDOU);
            assertThat(d.desfecho()).isEqualTo(Desfecho.CONCLUIDA);
            assertThat(d.novo()).isEqualTo(new EstadoAndamento(IMPACTO, 1, NAO_AJUDOU));
        }

        @Test
        void maisVotadoVenceSemEmpate() {
            Andamento a = emImpacto(4);
            a.responder(1, NAO_AJUDOU);
            a.lote(2, 3, AJUDOU);
            assertThat(a.estado.resultadoImpacto()).isEqualTo(AJUDOU);
        }

        @Test
        void empateEntreTodasAsOpcoesFicaComNaoAjudou() {
            Andamento a = emImpacto(3);
            a.responder(1, AJUDOU);
            a.responder(2, AJUDOU_PARCIALMENTE);
            a.responder(3, NAO_AJUDOU);
            assertThat(a.estado.resultadoImpacto()).isEqualTo(NAO_AJUDOU);
        }

        @Test
        void depoisDeConcluidoNaoAceitaMaisRespostas() {
            Andamento a = emImpacto(1);
            a.responder(1, AJUDOU);
            assertThat(a.estado.concluido()).isTrue();
            assertThatThrownBy(() -> a.responder(2, AJUDOU))
                    .isInstanceOf(EtapaException.AndamentoConcluido.class)
                    .isInstanceOf(ConflitoException.class);
        }

        @Test
        void impactoDuplicadoEhConflito() {
            Andamento a = emImpacto(3);
            a.responder(1, AJUDOU);
            assertThatThrownBy(() -> a.responder(1, NAO_AJUDOU)).isInstanceOf(EtapaException.RespostaDuplicada.class);
        }

        @Test
        void impactoDepoisDeReaberturaDaExecucaoExigeNovaConfirmacao() {
            Andamento a = new Andamento(1);
            a.responder(1, SIM); // MOB
            a.responder(1, SIM); // EXE
            a.responder(1, NAO); // volta para MOB, r2
            assertThatThrownBy(() -> a.responder(1, IMPACTO, AJUDOU))
                    .isInstanceOf(EtapaException.ImpactoAntesDaExecucao.class);
            a.responder(1, SIM); // EXE r2
            a.responder(1, SIM); // IMPACTO r2
            assertThat(a.responder(1, AJUDOU).novo()).isEqualTo(new EstadoAndamento(IMPACTO, 2, AJUDOU));
        }
    }

    @Test
    void estadoComResultadoInconsistenteEhRecusado() {
        assertThatThrownBy(() -> new EstadoAndamento(EXECUCAO, 1, AJUDOU)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EstadoAndamento(IMPACTO, 1, SIM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EstadoAndamento(RECEBIMENTO, 0, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
