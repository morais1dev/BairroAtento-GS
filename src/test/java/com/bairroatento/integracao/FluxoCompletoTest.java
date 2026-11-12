package com.bairroatento.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
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

/**
 * Fluxo ponta a ponta pela API (MockMvc + H2): cadastro, ocorrência, confirmações, encaminhamento a dois
 * destinatários, etapas até a execução, uma reabertura por maioria de NAO, IMPACTO, progresso e indicador.
 * O bairro de teste tem limite N = 2 em config_regiao. Todos os dados são fictícios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoCompletoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private final Random random = new Random(42);

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode corpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    /** Gera um CPF fictício com dígitos verificadores válidos (sem todos os dígitos iguais). */
    private String cpfFicticio() {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) d[i] = random.nextInt(10);
        } while (Arrays.stream(d, 0, 9).distinct().count() == 1);
        for (int k = 9; k < 11; k++) {
            int soma = 0;
            for (int i = 0; i < k; i++) soma += d[i] * (k + 1 - i);
            int r = (soma * 10) % 11;
            d[k] = r == 10 ? 0 : r;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    private long morador(String nome, String cpf) throws Exception {
        JsonNode m = corpo(postJson("/moradores", "{\"nome\":\"" + nome + "\",\"email\":\"" + UUID.randomUUID()
                + "@example.com\",\"cpf\":\"" + cpf + "\"}").andExpect(status().isCreated()));
        String mascarado = m.get("cpf").asString();
        assertThat(mascarado).contains("*").doesNotContain(cpf);
        return m.get("id").asLong();
    }

    private JsonNode responder(long enc, String etapa, long morador, String resposta, int status) throws Exception {
        return corpo(postJson("/encaminhamentos/" + enc + "/etapas/" + etapa + "/respostas",
                "{\"moradorId\":" + morador + ",\"resposta\":\"" + resposta + "\"}").andExpect(status().is(status)));
    }

    private void confirmar(long enc, String etapa, long a, long b) throws Exception {
        responder(enc, etapa, a, "SIM", 201);
        assertThat(responder(enc, etapa, b, "SIM", 201).get("desfecho").asString()).isEqualTo("CONFIRMADA");
    }

    @Test
    void fluxoCompleto() throws Exception {
        String bairro = "Vila Esperança " + UUID.randomUUID();
        jdbc.update("insert into config_regiao (bairro, minimo_confirmacoes) values (?, 2)", bairro);

        // cadastro (CPF fictício do projeto e outros gerados)
        long autor = morador("Dona Lúcia", "969.430.400-81");
        List<Long> outros = new ArrayList<>();
        for (String nome : List.of("Morador Um", "Morador Dois", "Morador Tres")) {
            outros.add(morador(nome, cpfFicticio()));
        }
        long c1 = outros.get(0);
        long c2 = outros.get(1);

        // ocorrência e confirmações
        JsonNode oc = corpo(postJson("/ocorrencias", "{\"categoria\":\"buraco\",\"descricao\":\"Buraco na rua\","
                + "\"endereco\":\"Rua das Flores, 120\",\"bairro\":\"" + bairro + "\",\"autorId\":" + autor + "}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTA")));
        long ocId = oc.get("id").asLong();
        for (long m : outros) {
            postJson("/ocorrencias/" + ocId + "/confirmacoes", "{\"moradorId\":" + m + "}")
                    .andExpect(status().isCreated());
        }
        postJson("/ocorrencias/" + ocId + "/confirmacoes", "{\"moradorId\":" + c1 + "}").andExpect(status().isConflict());
        mvc.perform(get("/ocorrencias/" + ocId)).andExpect(status().isOk());
        mvc.perform(get("/ocorrencias").param("bairro", bairro)).andExpect(status().isOk());

        // encaminhamento a 2 destinatários
        JsonNode encs = corpo(postJson("/ocorrencias/" + ocId + "/encaminhamentos", "{\"destinatarioIds\":[1,2]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(2))));
        long a = encs.get(0).get("id").asLong();
        long b = encs.get(1).get("id").asLong();
        assertThat(encs.get(0).get("protocolo").asString()).isNotEqualTo(encs.get(1).get("protocolo").asString());
        mvc.perform(get("/ocorrencias/" + ocId + "/encaminhamentos")).andExpect(jsonPath("$", hasSize(2)));

        // etapa fora de ordem
        responder(a, "EXECUCAO", autor, "SIM", 409);

        // encaminhamento A: todas as etapas confirmadas, impacto AJUDOU
        confirmar(a, "RECEBIMENTO", autor, c1);
        confirmar(a, "MOBILIZACAO", autor, c1);
        confirmar(a, "EXECUCAO", autor, c1);
        responder(a, "IMPACTO", autor, "AJUDOU", 201);
        JsonNode fim = responder(a, "IMPACTO", c1, "AJUDOU", 201);
        assertThat(fim.get("concluido").asBoolean()).isTrue();
        assertThat(fim.get("resultadoImpacto").asString()).isEqualTo("AJUDOU");

        // encaminhamento B: IMPACTO antes da execução é recusado
        confirmar(b, "RECEBIMENTO", autor, c1);
        responder(b, "IMPACTO", autor, "AJUDOU", 422);

        // maioria de NAO na mobilização reabre o andamento com rodada nova
        responder(b, "MOBILIZACAO", c1, "NAO", 201);
        JsonNode reaberta = responder(b, "MOBILIZACAO", c2, "NAO", 201);
        assertThat(reaberta.get("desfecho").asString()).isEqualTo("REABERTA");
        assertThat(reaberta.get("etapaAtual").asString()).isEqualTo("RECEBIMENTO");
        assertThat(reaberta.get("rodadaAtual").asInt()).isEqualTo(2);

        // na rodada nova os moradores respondem de novo
        confirmar(b, "RECEBIMENTO", c1, c2);
        confirmar(b, "MOBILIZACAO", c1, c2);
        confirmar(b, "EXECUCAO", c1, c2);
        responder(b, "IMPACTO", c1, "AJUDOU_PARCIALMENTE", 201);
        assertThat(responder(b, "IMPACTO", c2, "AJUDOU_PARCIALMENTE", 201).get("concluido").asBoolean()).isTrue();

        // progresso de B: etapa atual, contagens e histórico cronológico com a reabertura
        JsonNode prog = corpo(mvc.perform(get("/encaminhamentos/" + b + "/progresso")).andExpect(status().isOk()));
        assertThat(prog.get("etapaAtual").asString()).isEqualTo("IMPACTO");
        assertThat(prog.at("/respostasPorEtapa/IMPACTO/AJUDOU_PARCIALMENTE").asInt()).isEqualTo(2);
        assertThat(prog.at("/respostasPorEtapa/MOBILIZACAO/SIM").asInt()).isEqualTo(2); // só a rodada atual
        assertThat(prog.at("/respostasPorEtapa/MOBILIZACAO/NAO").asInt()).isZero();
        JsonNode hist = prog.get("historico");
        assertThat(hist.size()).isEqualTo(6); // criação, avanço, reabertura e mais três avanços
        String anterior = "";
        boolean voltouParaRecebimento = false;
        for (int i = 0; i < hist.size(); i++) {
            String data = hist.get(i).get("data").asString();
            assertThat(data.compareTo(anterior)).isGreaterThanOrEqualTo(0);
            anterior = data;
            JsonNode ant = hist.get(i).get("etapaAnterior");
            if (ant != null && "MOBILIZACAO".equals(ant.asString())
                    && "RECEBIMENTO".equals(hist.get(i).get("etapaNova").asString())) {
                voltouParaRecebimento = true;
            }
        }
        assertThat(voltouParaRecebimento).isTrue();
        mvc.perform(get("/encaminhamentos/" + b + "/historico")).andExpect(status().isOk());
        mvc.perform(get("/encaminhamentos/" + b)).andExpect(jsonPath("$.concluido").value(true));

        // indicador do bairro: 1 ajudou + 1 parcial = 50% e 50%, tempo médio presente
        mvc.perform(get("/indicadores/impacto").param("bairro", bairro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(2))
                .andExpect(jsonPath("$.impactosFechados").value(2))
                .andExpect(jsonPath("$.ajudou").value(1))
                .andExpect(jsonPath("$.ajudouParcialmente").value(1))
                .andExpect(jsonPath("$.naoAjudou").value(0))
                .andExpect(jsonPath("$.percentualAjudou").value(50.0))
                .andExpect(jsonPath("$.percentualAjudouParcialmente").value(50.0))
                .andExpect(jsonPath("$.execucoesConfirmadas").value(2))
                .andExpect(jsonPath("$.tempoMedioExecucaoSegundos").isNumber());

        // bairro vazio: zerado, sem erro
        mvc.perform(get("/indicadores/impacto").param("bairro", "Bairro Vazio " + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(0))
                .andExpect(jsonPath("$.impactosFechados").value(0))
                .andExpect(jsonPath("$.percentualAjudou").value(0.0))
                .andExpect(jsonPath("$.percentualAjudouParcialmente").value(0.0))
                .andExpect(jsonPath("$.tempoMedioExecucaoSegundos").value(nullValue()));
    }
}
