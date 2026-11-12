package com.bairroatento.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Formato padrão para 415 e 500 (o 500 usa um controller que só existe nos testes). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ErroPadrao415e500Test.ControllerQueFalha.class)
class ErroPadrao415e500Test {

    @RestController
    static class ControllerQueFalha {
        @GetMapping("/teste/falha")
        String falha() {
            throw new IllegalStateException("detalhe interno secreto");
        }
    }

    @Autowired MockMvc mvc;

    @Test
    void midiaNaoSuportada415() throws Exception {
        mvc.perform(post("/moradores").contentType(MediaType.TEXT_PLAIN).content("nome=Ana"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.erro").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.mensagem").value(notNullValue()))
                .andExpect(jsonPath("$.caminho").value("/moradores"))
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void erroInterno500SemVazarDetalhes() throws Exception {
        mvc.perform(get("/teste/falha"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.erro").value("Internal Server Error"))
                .andExpect(jsonPath("$.mensagem").value("Erro interno do servidor"))
                .andExpect(jsonPath("$.caminho").value("/teste/falha"))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(content().string(not(containsString("secreto"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
