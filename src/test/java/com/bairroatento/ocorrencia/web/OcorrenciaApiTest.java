package com.bairroatento.ocorrencia.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OcorrenciaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long criarMorador() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String r = postJson("/moradores",
                "{\"nome\":\"Maria\",\"email\":\"" + email + "\",\"cpf\":\"969.430.400-81\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asLong();
    }

    private String corpoOcorrencia(long autor, String categoria, String bairro) {
        return "{\"categoria\":\"" + categoria + "\",\"descricao\":\"Algo\",\"endereco\":\"Rua A\","
                + "\"bairro\":\"" + bairro + "\",\"autorId\":" + autor + "}";
    }

    private long criarOcorrencia(long autor, String bairro) throws Exception {
        String r = postJson("/ocorrencias", corpoOcorrencia(autor, "buraco", bairro))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(r).get("id").asLong();
    }

    @Test
    void moradorMascaraCpfEEmailDuplicado() throws Exception {
        String body = "{\"nome\":\"Joao\",\"email\":\"dup@example.com\",\"cpf\":\"96943040081\"}";
        postJson("/moradores", body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("***.***.***-81"))
                .andExpect(content().string(not(containsString("969430400"))));
        postJson("/moradores", body).andExpect(status().isConflict());
    }

    @Test
    void moradorInvalido() throws Exception {
        postJson("/moradores", "{\"nome\":\"X\",\"email\":\"x1@example.com\",\"cpf\":\"969.430.400-82\"}")
                .andExpect(status().isUnprocessableContent());
        postJson("/moradores", "{\"nome\":\"\",\"email\":\"ruim\",\"cpf\":\"\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void criaOcorrenciaEDetalha() throws Exception {
        long autor = criarMorador();
        postJson("/ocorrencias", corpoOcorrencia(autor, "buraco", "Centro"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.id").exists());
        long id = criarOcorrencia(autor, "Centro");
        mvc.perform(get("/ocorrencias/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.contadorConfirmacoes").value(0));
        mvc.perform(get("/ocorrencias/999999")).andExpect(status().isNotFound());
    }

    @Test
    void validacoesDeOcorrencia() throws Exception {
        long autor = criarMorador();
        postJson("/ocorrencias", corpoOcorrencia(autor, "inexistente", "Centro"))
                .andExpect(status().isUnprocessableContent());
        postJson("/ocorrencias", "{\"categoria\":\"buraco\",\"autorId\":" + autor + "}")
                .andExpect(status().isBadRequest());
        postJson("/ocorrencias", corpoOcorrencia(999999, "buraco", "Centro"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void confirmacoesSimultaneasNaoPerdemIncremento() throws Exception {
        long autor = criarMorador();
        long id = criarOcorrencia(autor, "Centro");
        int n = 8;
        java.util.List<Long> moradores = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) moradores.add(criarMorador());
        var pool = java.util.concurrent.Executors.newFixedThreadPool(n);
        var largada = new java.util.concurrent.CountDownLatch(1);
        var futuros = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        try {
            for (long m : moradores) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return postJson("/ocorrencias/" + id + "/confirmacoes", "{\"moradorId\":" + m + "}")
                            .andExpect(status().isCreated());
                }));
            }
            largada.countDown();
            for (var f : futuros) f.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            largada.countDown();
            pool.shutdownNow();
        }
        mvc.perform(get("/ocorrencias/" + id)).andExpect(jsonPath("$.contadorConfirmacoes").value(n));
    }

    @Test
    void listaComFiltroEPaginacao() throws Exception {
        long autor = criarMorador();
        String bairro = "Bairro-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) criarOcorrencia(autor, bairro);
        mvc.perform(get("/ocorrencias").param("bairro", bairro).param("categoria", "buraco")
                        .param("status", "ABERTA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(3)))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get("/ocorrencias").param("bairro", bairro).param("pagina", "1"))
                .andExpect(jsonPath("$.conteudo", hasSize(0)))
                .andExpect(jsonPath("$.pagina").value(1));
    }

    @Test
    void confirmacoes() throws Exception {
        long autor = criarMorador();
        long outro = criarMorador();
        long id = criarOcorrencia(autor, "Centro");
        String url = "/ocorrencias/" + id + "/confirmacoes";
        postJson(url, "{\"moradorId\":" + outro + "}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.contadorConfirmacoes").value(1));
        postJson(url, "{\"moradorId\":" + outro + "}").andExpect(status().isConflict());
        postJson(url, "{\"moradorId\":999999}").andExpect(status().isNotFound());
        postJson("/ocorrencias/999999/confirmacoes", "{\"moradorId\":" + outro + "}")
                .andExpect(status().isNotFound());
        mvc.perform(get("/ocorrencias/" + id)).andExpect(jsonPath("$.contadorConfirmacoes").value(1));
    }
}
