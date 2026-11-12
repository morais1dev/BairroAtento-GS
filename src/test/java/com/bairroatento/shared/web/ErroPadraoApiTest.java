package com.bairroatento.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
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

/** Todos os erros da API seguem o formato ErroResponse: timestamp, status, erro, mensagem, caminho, campos. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErroPadraoApiTest {

    @Autowired MockMvc mvc;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions formatoPadrao(ResultActions r, int status, String erro, String caminho) throws Exception {
        return r.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.erro").value(erro))
                .andExpect(jsonPath("$.mensagem").value(notNullValue()))
                .andExpect(jsonPath("$.caminho").value(caminho))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at com."))));
    }

    @Test
    void validacao400ListaCampos() throws Exception {
        formatoPadrao(postJson("/moradores", "{\"nome\":\"\",\"email\":\"x\",\"cpf\":\"1\"}"),
                400, "Bad Request", "/moradores")
                .andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("nome")))
                .andExpect(jsonPath("$.campos[*].campo").value(hasItem("email")));
    }

    @Test
    void jsonMalformado400() throws Exception {
        formatoPadrao(postJson("/moradores", "{nome:"), 400, "Bad Request", "/moradores")
                .andExpect(jsonPath("$.campos", hasSize(0)));
    }

    @Test
    void tipoErradoNoCorpo400() throws Exception {
        formatoPadrao(postJson("/ocorrencias/1/confirmacoes", "{\"moradorId\":\"abc\"}"),
                400, "Bad Request", "/ocorrencias/1/confirmacoes")
                .andExpect(jsonPath("$.campos[0].campo").value("moradorId"));
    }

    @Test
    void tipoErradoNoPath400() throws Exception {
        formatoPadrao(mvc.perform(get("/ocorrencias/abc")), 400, "Bad Request", "/ocorrencias/abc")
                .andExpect(jsonPath("$.campos[0].campo").value("id"));
    }

    @Test
    void tipoErradoNaQuery400() throws Exception {
        formatoPadrao(mvc.perform(get("/ocorrencias").param("pagina", "x")), 400, "Bad Request", "/ocorrencias");
    }

    @Test
    void etapaDesconhecida400() throws Exception {
        formatoPadrao(postJson("/encaminhamentos/1/etapas/XYZ/respostas", "{\"moradorId\":1,\"resposta\":\"SIM\"}"),
                400, "Bad Request", "/encaminhamentos/1/etapas/XYZ/respostas");
    }

    @Test
    void inexistente404() throws Exception {
        formatoPadrao(mvc.perform(get("/ocorrencias/999999")), 404, "Not Found", "/ocorrencias/999999");
        formatoPadrao(mvc.perform(get("/encaminhamentos/999999")), 404, "Not Found", "/encaminhamentos/999999");
    }

    @Test
    void rotaInexistente404() throws Exception {
        formatoPadrao(mvc.perform(get("/nao-existe")), 404, "Not Found", "/nao-existe");
    }

    @Test
    void metodoNaoPermitido405() throws Exception {
        formatoPadrao(mvc.perform(get("/moradores")), 405, "Method Not Allowed", "/moradores");
    }

    @Test
    void duplicidade409() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String body = "{\"nome\":\"Ana\",\"email\":\"" + email + "\",\"cpf\":\"969.430.400-81\"}";
        postJson("/moradores", body).andExpect(status().isCreated());
        formatoPadrao(postJson("/moradores", body), 409, "Conflict", "/moradores")
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void regraDeNegocio422() throws Exception {
        formatoPadrao(mvc.perform(get("/indicadores/impacto").param("periodo", "banana")),
                422, "Unprocessable Content", "/indicadores/impacto");
    }
}
