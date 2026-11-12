package com.bairroatento.shared.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private JsonNode docs() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void listaTodosOsEndpointsReais() throws Exception {
        JsonNode paths = docs().get("paths");
        Map<String, String> esperados = Map.ofEntries(
                Map.entry("/moradores", "post"),
                Map.entry("/ocorrencias", "post,get"),
                Map.entry("/ocorrencias/{id}", "get"),
                Map.entry("/ocorrencias/{id}/confirmacoes", "post"),
                Map.entry("/ocorrencias/{id}/encaminhamentos", "post,get"),
                Map.entry("/encaminhamentos/{id}", "get"),
                Map.entry("/encaminhamentos/{id}/etapas/{etapa}/respostas", "post"),
                Map.entry("/encaminhamentos/{id}/progresso", "get"),
                Map.entry("/encaminhamentos/{id}/historico", "get"),
                Map.entry("/indicadores/impacto", "get"));
        long operacoes = 0;
        for (var e : esperados.entrySet()) {
            assertTrue(paths.has(e.getKey()), "faltou no Swagger: " + e.getKey());
            for (String m : e.getValue().split(",")) {
                JsonNode op = paths.get(e.getKey()).get(m);
                assertTrue(op != null, "faltou " + m + " " + e.getKey());
                assertTrue(op.has("summary") && op.has("tags"), "sem summary/tag: " + m + " " + e.getKey());
                assertTrue(op.get("responses").size() >= 2, "poucas respostas: " + m + " " + e.getKey());
                operacoes++;
            }
        }
        assertEquals(12, operacoes);
        assertEquals(esperados.size(), paths.size(), "há paths a mais no Swagger");
    }

    @Test
    void erroTemSchemaEExemplo() throws Exception {
        JsonNode docs = docs();
        assertTrue(docs.at("/components/schemas/ErroResponse").has("properties"));
        JsonNode r404 = docs.at("/paths/~1ocorrencias~1{id}/get/responses/404/content/application~1json");
        assertTrue(r404.at("/schema/$ref").asString().endsWith("ErroResponse"));
        assertEquals(404, r404.at("/examples/erro/value/status").asInt());
    }

    @Test
    void swaggerUiResponde() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
