package com.bairroatento.etapa.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bairroatento.etapa.domain.Decisao;
import com.bairroatento.etapa.domain.Decisao.Desfecho;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import com.bairroatento.shared.domain.ConflitoException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Respostas simultâneas, em threads reais, cada uma na sua própria transação. O bloqueio da linha
 * do encaminhamento precisa serializar a apuração: uma única confirmação, nenhuma resposta a mais.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RespostaEtapaConcorrenciaTest {

    static final int TIMEOUT_SEGUNDOS = 30;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired RespostaEtapaService service;

    record Cenario(long encaminhamento, List<Long> moradores) {}

    long criarMorador() throws Exception {
        String r = mvc.perform(post("/moradores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Lia\",\"email\":\"" + UUID.randomUUID()
                                + "@example.com\",\"cpf\":\"969.430.400-81\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asLong();
    }

    /** Ocorrência com autor + (autorizados - 1) confirmadores, mínimo do bairro e um encaminhamento. */
    Cenario cenario(int autorizados, int minimo) throws Exception {
        String bairro = "Concorrencia-" + UUID.randomUUID();
        jdbc.update("insert into config_regiao (bairro, minimo_confirmacoes) values (?, ?)", bairro, minimo);
        List<Long> moradores = new ArrayList<>();
        for (int i = 0; i < autorizados; i++) moradores.add(criarMorador());
        String oc = mvc.perform(post("/ocorrencias").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoria\":\"buraco\",\"descricao\":\"X\",\"endereco\":\"Rua D\",\"bairro\":\""
                                + bairro + "\",\"autorId\":" + moradores.get(0) + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long ocorrencia = json.readTree(oc).get("id").asLong();
        for (Long m : moradores.subList(1, moradores.size())) {
            mvc.perform(post("/ocorrencias/" + ocorrencia + "/confirmacoes").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"moradorId\":" + m + "}")).andExpect(status().isCreated());
        }
        String enc = mvc.perform(post("/ocorrencias/" + ocorrencia + "/encaminhamentos")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinatarioIds\":[1]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Cenario(json.readTree(enc).get(0).get("id").asLong(), moradores);
    }

    /** Dispara todas as tarefas ao mesmo tempo e devolve as decisões (ou a exceção de cada uma). */
    List<Object> emParalelo(List<Callable<Decisao>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        try {
            CountDownLatch prontas = new CountDownLatch(tarefas.size());
            CountDownLatch largada = new CountDownLatch(1);
            List<Future<Decisao>> futuros = new ArrayList<>();
            for (Callable<Decisao> t : tarefas) {
                futuros.add(pool.submit(() -> {
                    prontas.countDown();
                    largada.await();
                    return t.call();
                }));
            }
            assertThat(prontas.await(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)).as("threads prontas").isTrue();
            largada.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Decisao> f : futuros) {
                try {
                    resultados.add(f.get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    resultados.add(e.getCause());
                }
            }
            return resultados;
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)).as("pool encerrado").isTrue();
        }
    }

    static boolean conflito(Object r) {
        return r instanceof ConflitoException || r instanceof DataIntegrityViolationException;
    }

    int contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @Test
    @Timeout(value = 2 * TIMEOUT_SEGUNDOS, unit = TimeUnit.SECONDS)
    void respostasSimultaneasConfirmamAEtapaUmaUnicaVez() throws Exception {
        int minimo = 3;
        Cenario c = cenario(8, minimo);
        List<Callable<Decisao>> tarefas = new ArrayList<>();
        for (Long m : c.moradores()) {
            tarefas.add(() -> service.responder(c.encaminhamento(), Etapa.RECEBIMENTO, m, OpcaoResposta.SIM));
        }

        List<Object> resultados = emParalelo(tarefas);

        List<Decisao> aceitas = resultados.stream().filter(Decisao.class::isInstance).map(Decisao.class::cast).toList();
        List<Object> recusadas = resultados.stream().filter(r -> !(r instanceof Decisao)).toList();
        assertThat(recusadas).as("recusas devem ser só conflitos: %s", recusadas)
                .allMatch(RespostaEtapaConcorrenciaTest::conflito);
        assertThat(aceitas).hasSize(minimo);
        assertThat(recusadas).hasSize(c.moradores().size() - minimo);
        assertThat(aceitas).filteredOn(d -> d.desfecho() == Desfecho.CONFIRMADA).hasSize(1);

        long enc = c.encaminhamento();
        assertThat(jdbc.queryForObject("select etapa_atual from encaminhamento where id = ?", String.class, enc))
                .isEqualTo("MOBILIZACAO");
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ?", enc)).isEqualTo(minimo);
        assertThat(contar("select count(*) from etapa_confirmacao where encaminhamento_id = ?", enc)).isEqualTo(1);
        assertThat(contar("select count(*) from historico_etapa where encaminhamento_id = ? and etapa_nova = 'MOBILIZACAO'",
                enc)).isEqualTo(1);
    }

    @Test
    @Timeout(value = 2 * TIMEOUT_SEGUNDOS, unit = TimeUnit.SECONDS)
    void mesmoMoradorRespondendoEmParaleloSoTemUmaRespostaAceita() throws Exception {
        Cenario c = cenario(2, 5);
        long morador = c.moradores().get(1);
        List<Callable<Decisao>> tarefas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            OpcaoResposta opcao = i % 2 == 0 ? OpcaoResposta.SIM : OpcaoResposta.NAO;
            tarefas.add(() -> service.responder(c.encaminhamento(), Etapa.RECEBIMENTO, morador, opcao));
        }

        List<Object> resultados = emParalelo(tarefas);

        assertThat(resultados).filteredOn(Decisao.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(r -> !(r instanceof Decisao)).hasSize(5)
                .allMatch(RespostaEtapaConcorrenciaTest::conflito);
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ? and morador_id = ?",
                c.encaminhamento(), morador)).isEqualTo(1);
    }

    @Test
    @Timeout(value = 2 * TIMEOUT_SEGUNDOS, unit = TimeUnit.SECONDS)
    void reaberturaSimultaneaAbreUmaUnicaRodadaNova() throws Exception {
        int minimo = 2;
        Cenario c = cenario(6, minimo);
        List<Callable<Decisao>> tarefas = new ArrayList<>();
        for (Long m : c.moradores()) {
            tarefas.add(() -> service.responder(c.encaminhamento(), Etapa.RECEBIMENTO, m, OpcaoResposta.NAO));
        }

        List<Object> resultados = emParalelo(tarefas);

        // RECEBIMENTO reabre em RECEBIMENTO: quem chega depois responde na rodada seguinte e pode
        // reabrir de novo, mas cada rodada só reabre uma vez e nenhuma resposta se perde.
        assertThat(resultados).allMatch(Decisao.class::isInstance);
        List<Decisao> decisoes = resultados.stream().map(Decisao.class::cast).toList();
        long reaberturas = decisoes.stream().filter(d -> d.desfecho() == Desfecho.REABERTA).count();
        assertThat(reaberturas).isEqualTo(c.moradores().size() / minimo);
        assertThat(decisoes.stream().filter(d -> d.desfecho() == Desfecho.REABERTA).map(Decisao::rodadaDaResposta))
                .doesNotHaveDuplicates();
        long enc = c.encaminhamento();
        assertThat(contar("select rodada_atual from encaminhamento where id = ?", enc)).isEqualTo(1 + (int) reaberturas);
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ?", enc))
                .isEqualTo(c.moradores().size());
        assertThat(contar("select max(c) from (select count(*) c from resposta_etapa where encaminhamento_id = ? "
                + "group by rodada) t", enc)).isEqualTo(minimo);
    }
}
