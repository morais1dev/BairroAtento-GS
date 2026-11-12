package com.bairroatento.etapa.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RespostaEtapaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    long autor;
    long confirmador1;
    long confirmador2;
    long estranho;
    long encaminhamento;
    long outroEncaminhamento;

    ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    JsonNode corpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    long criarMorador() throws Exception {
        return corpo(postJson("/moradores", "{\"nome\":\"Rui\",\"email\":\"" + UUID.randomUUID()
                + "@example.com\",\"cpf\":\"969.430.400-81\"}").andExpect(status().isCreated())).get("id").asLong();
    }

    /** Ocorrência num bairro novo; minimo = null deixa o bairro sem linha em config_regiao. */
    void prepararCenario(Integer minimo) throws Exception {
        String bairro = "Bairro-" + UUID.randomUUID();
        if (minimo != null) {
            jdbc.update("insert into config_regiao (bairro, minimo_confirmacoes) values (?, ?)", bairro, minimo);
        }
        autor = criarMorador();
        confirmador1 = criarMorador();
        confirmador2 = criarMorador();
        estranho = criarMorador();
        long oc = corpo(postJson("/ocorrencias", "{\"categoria\":\"lixo\",\"descricao\":\"Lixo\",\"endereco\":\"Rua C\","
                + "\"bairro\":\"" + bairro + "\",\"autorId\":" + autor + "}").andExpect(status().isCreated()))
                .get("id").asLong();
        for (long m : List.of(confirmador1, confirmador2)) {
            postJson("/ocorrencias/" + oc + "/confirmacoes", "{\"moradorId\":" + m + "}").andExpect(status().isCreated());
        }
        JsonNode encs = corpo(postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[1,2]}")
                .andExpect(status().isCreated()));
        encaminhamento = encs.get(0).get("id").asLong();
        outroEncaminhamento = encs.get(1).get("id").asLong();
    }

    ResultActions responder(long enc, String etapa, long morador, String resposta) throws Exception {
        return postJson("/encaminhamentos/" + enc + "/etapas/" + etapa + "/respostas",
                "{\"moradorId\":" + morador + ",\"resposta\":\"" + resposta + "\"}");
    }

    ResultActions responder(String etapa, long morador, String resposta) throws Exception {
        return responder(encaminhamento, etapa, morador, resposta);
    }

    void confirmar(String etapa) throws Exception {
        responder(etapa, autor, "SIM").andExpect(status().isCreated());
        responder(etapa, confirmador1, "SIM").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("CONFIRMADA"));
    }

    int contar(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @BeforeEach
    void cenarioPadrao() throws Exception {
        prepararCenario(2);
    }

    @Test
    void fluxoCompletoAteImpacto() throws Exception {
        responder("RECEBIMENTO", autor, "SIM").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("PENDENTE"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.placar.SIM").value(1));
        responder("recebimento", confirmador1, "sim").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("CONFIRMADA"))
                .andExpect(jsonPath("$.etapaAtual").value("MOBILIZACAO"));
        confirmar("MOBILIZACAO");
        confirmar("EXECUCAO");
        mvc.perform(get("/encaminhamentos/" + encaminhamento)).andExpect(jsonPath("$.etapaAtual").value("IMPACTO"));

        responder("IMPACTO", autor, "AJUDOU_PARCIALMENTE").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("PENDENTE"));
        responder("IMPACTO", confirmador2, "AJUDOU_PARCIALMENTE").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("CONCLUIDA"))
                .andExpect(jsonPath("$.concluido").value(true))
                .andExpect(jsonPath("$.resultadoImpacto").value("AJUDOU_PARCIALMENTE"));

        mvc.perform(get("/encaminhamentos/" + encaminhamento))
                .andExpect(jsonPath("$.resultadoImpacto").value("AJUDOU_PARCIALMENTE"))
                .andExpect(jsonPath("$.dataConclusao").exists());
        responder("IMPACTO", confirmador1, "AJUDOU").andExpect(status().isConflict());

        mvc.perform(get("/encaminhamentos/" + encaminhamento + "/historico"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].etapaAnterior").value("RECEBIMENTO"))
                .andExpect(jsonPath("$[1].etapaNova").value("MOBILIZACAO"))
                .andExpect(jsonPath("$[3].etapaNova").value("IMPACTO"))
                .andExpect(jsonPath("$[3].data").exists());
        assertThat(jdbc.queryForList("select etapa from etapa_confirmacao where encaminhamento_id = ? order by confirmada_em",
                String.class, encaminhamento)).containsExactlyInAnyOrder("RECEBIMENTO", "MOBILIZACAO", "EXECUCAO", "IMPACTO");

        // andamento do outro destinatário é independente
        mvc.perform(get("/encaminhamentos/" + outroEncaminhamento))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"));
    }

    @Test
    void reaberturaIniciaRodadaNovaEGuardaRespostasAntigas() throws Exception {
        confirmar("RECEBIMENTO");
        responder("MOBILIZACAO", autor, "NAO").andExpect(jsonPath("$.desfecho").value("PENDENTE"));
        responder("MOBILIZACAO", confirmador2, "NAO").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("REABERTA"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.rodadaDaResposta").value(1))
                .andExpect(jsonPath("$.rodadaAtual").value(2));

        // os mesmos moradores respondem RECEBIMENTO de novo, na rodada 2
        responder("RECEBIMENTO", autor, "NAO").andExpect(status().isCreated())
                .andExpect(jsonPath("$.rodadaDaResposta").value(2))
                .andExpect(jsonPath("$.placar.NAO").value(1))
                .andExpect(jsonPath("$.placar.SIM").value(0));
        // "não recebi" continua em RECEBIMENTO, com nova rodada
        responder("RECEBIMENTO", confirmador1, "NAO").andExpect(jsonPath("$.desfecho").value("REABERTA"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.rodadaAtual").value(3));

        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ?", encaminhamento)).isEqualTo(6);
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ? and rodada = 1", encaminhamento))
                .isEqualTo(4);
        mvc.perform(get("/encaminhamentos/" + encaminhamento + "/historico"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[2].etapaAnterior").value("MOBILIZACAO"))
                .andExpect(jsonPath("$[2].etapaNova").value("RECEBIMENTO"))
                .andExpect(jsonPath("$[2].rodada").value(2))
                .andExpect(jsonPath("$[3].etapaAnterior").value("RECEBIMENTO"))
                .andExpect(jsonPath("$[3].etapaNova").value("RECEBIMENTO"));

        // reconfirma RECEBIMENTO numa rodada nova (etapa_confirmacao aceita a segunda confirmação)
        confirmar("RECEBIMENTO");
        assertThat(contar("select count(*) from etapa_confirmacao where encaminhamento_id = ? and etapa = 'RECEBIMENTO'",
                encaminhamento)).isEqualTo(2);
    }

    @Test
    void reaberturaDaExecucaoReconfirmaMobilizacaoEIndicadorLeOResultado() throws Exception {
        String bairro = jdbc.queryForObject("select o.bairro from ocorrencia o join encaminhamento e "
                + "on e.ocorrencia_id = o.id where e.id = ?", String.class, encaminhamento);
        confirmar("RECEBIMENTO");
        confirmar("MOBILIZACAO");
        responder("EXECUCAO", autor, "NAO");
        responder("EXECUCAO", confirmador1, "NAO").andExpect(jsonPath("$.desfecho").value("REABERTA"))
                .andExpect(jsonPath("$.etapaAtual").value("MOBILIZACAO"))
                .andExpect(jsonPath("$.rodadaAtual").value(2));
        confirmar("MOBILIZACAO");
        confirmar("EXECUCAO");
        assertThat(contar("select count(*) from etapa_confirmacao where encaminhamento_id = ? and etapa = 'MOBILIZACAO'",
                encaminhamento)).isEqualTo(2);
        responder("IMPACTO", autor, "NAO_AJUDOU");
        responder("IMPACTO", confirmador1, "NAO_AJUDOU").andExpect(jsonPath("$.resultadoImpacto").value("NAO_AJUDOU"))
                .andExpect(jsonPath("$.etapaAtual").value("IMPACTO"));

        mvc.perform(get("/indicadores/impacto").param("bairro", bairro)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(2))
                .andExpect(jsonPath("$.impactosFechados").value(1));
    }

    @Test
    void empateNaoConfirmaNemReabre() throws Exception {
        responder("RECEBIMENTO", autor, "SIM");
        responder("RECEBIMENTO", confirmador1, "NAO").andExpect(jsonPath("$.desfecho").value("PENDENTE"));
        responder("RECEBIMENTO", confirmador2, "NAO_SEI").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("PENDENTE"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.rodadaAtual").value(1));
    }

    @Test
    void foraDeOrdemEh409() throws Exception {
        responder("EXECUCAO", autor, "SIM").andExpect(status().isConflict());
        responder("MOBILIZACAO", autor, "SIM").andExpect(status().isConflict());
        confirmar("RECEBIMENTO");
        responder("RECEBIMENTO", confirmador2, "SIM").andExpect(status().isConflict());
    }

    @Test
    void duplicadaEh409() throws Exception {
        responder("RECEBIMENTO", autor, "NAO_SEI").andExpect(status().isCreated());
        responder("RECEBIMENTO", autor, "SIM").andExpect(status().isConflict());
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ?", encaminhamento)).isEqualTo(1);
    }

    @Test
    void moradorQueNaoRegistrouNemConfirmouEh422() throws Exception {
        responder("RECEBIMENTO", estranho, "SIM").andExpect(status().isUnprocessableContent());
    }

    @Test
    void impactoAntesDaExecucaoEh422() throws Exception {
        responder("IMPACTO", autor, "AJUDOU").andExpect(status().isUnprocessableContent());
        confirmar("RECEBIMENTO");
        confirmar("MOBILIZACAO");
        responder("IMPACTO", autor, "AJUDOU").andExpect(status().isUnprocessableContent());
    }

    @Test
    void respostasInvalidas() throws Exception {
        responder("RECEBIMENTO", autor, "AJUDOU").andExpect(status().isUnprocessableContent());
        responder("RECEBIMENTO", autor, "TALVEZ").andExpect(status().isBadRequest());
        responder("ETAPA_X", autor, "SIM").andExpect(status().isBadRequest());
        postJson("/encaminhamentos/" + encaminhamento + "/etapas/RECEBIMENTO/respostas", "{\"resposta\":\"SIM\"}")
                .andExpect(status().isBadRequest());
        postJson("/encaminhamentos/" + encaminhamento + "/etapas/RECEBIMENTO/respostas", "{\"moradorId\":" + autor + "}")
                .andExpect(status().isBadRequest());
        responder(999999, "RECEBIMENTO", autor, "SIM").andExpect(status().isNotFound());
        responder("RECEBIMENTO", 999999, "SIM").andExpect(status().isNotFound());
        mvc.perform(get("/encaminhamentos/999999/historico")).andExpect(status().isNotFound());
        assertThat(contar("select count(*) from resposta_etapa where encaminhamento_id = ?", encaminhamento)).isZero();
    }

    @Test
    void semConfigRegiaoUsaMinimoPadrao() throws Exception {
        prepararCenario(null); // padrão 10: três moradores não bastam
        responder("RECEBIMENTO", autor, "SIM");
        responder("RECEBIMENTO", confirmador1, "SIM");
        responder("RECEBIMENTO", confirmador2, "SIM").andExpect(status().isCreated())
                .andExpect(jsonPath("$.desfecho").value("PENDENTE"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"));
    }

    @Test
    void minimoVemDoBairro() throws Exception {
        prepararCenario(1);
        responder("RECEBIMENTO", confirmador2, "SIM").andExpect(jsonPath("$.desfecho").value("CONFIRMADA"));
        responder("MOBILIZACAO", confirmador2, "NAO").andExpect(jsonPath("$.desfecho").value("REABERTA"))
                .andExpect(jsonPath("$.etapaAtual").value("RECEBIMENTO"));
    }
}
