package com.bairroatento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BairroAtentoApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void apiDocsResponde200() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void migracoesAplicadasComSeed() {
        assertThat(jdbc.queryForObject("select count(*) from categoria", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from destinatario", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from morador", Integer.class)).isEqualTo(1);
    }
}
