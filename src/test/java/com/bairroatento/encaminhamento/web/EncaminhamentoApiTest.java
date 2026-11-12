package com.bairroatento.encaminhamento.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EncaminhamentoApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    long criarOcorrencia() throws Exception {
        String r = postJson("/moradores", "{\"nome\":\"Ana\",\"email\":\"" + UUID.randomUUID()
                + "@example.com\",\"cpf\":\"969.430.400-81\"}").andReturn().getResponse().getContentAsString();
        long autor = json.readTree(r).get("id").asLong();
        String o = postJson("/ocorrencias", "{\"categoria\":\"buraco\",\"descricao\":\"Buraco\",\"endereco\":\"Rua B\","
                + "\"bairro\":\"Centro\",\"autorId\":" + autor + "}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(o).get("id").asLong();
    }

    @Test
    void encaminhaParaVariosDestinatariosComProtocolosEAndamentosProprios() throws Exception {
        long oc = criarOcorrencia();
        String r = postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[1,2,3]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].ocorrenciaId").value(oc))
                .andExpect(jsonPath("$[0].destinatarioId").value(1))
                .andExpect(jsonPath("$[2].destinatarioId").value(3))
                .andExpect(jsonPath("$[0].etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$[1].etapaAtual").value("RECEBIMENTO"))
                .andExpect(jsonPath("$[0].rodada").value(1))
                .andExpect(jsonPath("$[0].concluido").value(false))
                .andExpect(jsonPath("$[0].protocolo").value(startsWith("BA-")))
                .andReturn().getResponse().getContentAsString();

        JsonNode lista = json.readTree(r);
        Set<String> protocolos = new HashSet<>();
        for (JsonNode e : lista) protocolos.add(e.get("protocolo").asString());
        assertThat(protocolos).hasSize(3);

        long id = lista.get(0).get("id").asLong();
        mvc.perform(get("/encaminhamentos/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value(lista.get(0).get("protocolo").asString()));
        mvc.perform(get("/ocorrencias/" + oc + "/encaminhamentos")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/encaminhamentos/" + id + "/historico")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].etapaAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].etapaNova").value("RECEBIMENTO"));
    }

    @Test
    void destinatariosRepetidosGeramUmEncaminhamentoCada() throws Exception {
        long oc = criarOcorrencia();
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[2,2]}")
                .andExpect(status().isCreated()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void listaVaziaOuAusenteEh400() throws Exception {
        long oc = criarOcorrencia();
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[]}").andExpect(status().isBadRequest());
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{}").andExpect(status().isBadRequest());
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[null]}").andExpect(status().isBadRequest());
    }

    @Test
    void destinatarioInexistenteRecusaOPedidoInteiro() throws Exception {
        long oc = criarOcorrencia();
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[1,9999]}")
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from encaminhamento where ocorrencia_id = ?",
                Integer.class, oc)).isZero();
    }

    @Test
    void ocorrenciaInexistenteEh404() throws Exception {
        postJson("/ocorrencias/999999/encaminhamentos", "{\"destinatarioIds\":[1]}").andExpect(status().isNotFound());
        mvc.perform(get("/ocorrencias/999999/encaminhamentos")).andExpect(status().isNotFound());
        mvc.perform(get("/encaminhamentos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void reencaminharAoMesmoDestinatarioEhConflito() throws Exception {
        long oc = criarOcorrencia();
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[1]}").andExpect(status().isCreated());
        postJson("/ocorrencias/" + oc + "/encaminhamentos", "{\"destinatarioIds\":[2,1]}").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from encaminhamento where ocorrencia_id = ?",
                Integer.class, oc)).isEqualTo(1);
    }
}
