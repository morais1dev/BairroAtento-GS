package com.bairroatento.indicador.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IndicadorApiTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;

    /** Cria ocorrência + encaminhamento direto no banco (o motor de etapas é de outro pacote). */
    private long encaminhamento(String bairro, String etapa) {
        jdbc.sql("INSERT INTO ocorrencia (categoria, descricao, endereco, bairro, status, autor_id) "
                + "VALUES ('buraco','d','r',:b,'ABERTA',1)").param("b", bairro).update();
        long oc = jdbc.sql("SELECT MAX(id) FROM ocorrencia").query(Long.class).single();
        jdbc.sql("INSERT INTO encaminhamento (ocorrencia_id, destinatario_id, protocolo, data_envio, etapa_atual) "
                + "VALUES (:o,1,:p,'2026-03-10 08:00:00',:e)")
                .param("o", oc).param("p", UUID.randomUUID().toString()).param("e", etapa).update();
        return jdbc.sql("SELECT MAX(id) FROM encaminhamento").query(Long.class).single();
    }

    private void morador(long id) {
        jdbc.sql("INSERT INTO morador (id, nome, email, cpf_hash) VALUES (:i,'m',:e,'h')")
                .param("i", id).param("e", UUID.randomUUID() + "@x.com").update();
    }

    @Test
    void progressoDeEncaminhamentoInexistenteDa404() throws Exception {
        mvc.perform(get("/encaminhamentos/999999/progresso")).andExpect(status().isNotFound());
    }

    @Test
    void progressoMostraContagemEHistorico() throws Exception {
        long enc = encaminhamento("Progresso", "MOBILIZACAO");
        long m1 = 9001, m2 = 9002;
        morador(m1);
        morador(m2);
        jdbc.sql("INSERT INTO resposta_etapa (encaminhamento_id, etapa, morador_id, resposta) VALUES (:e,'RECEBIMENTO',:m,'SIM')")
                .param("e", enc).param("m", m1).update();
        jdbc.sql("INSERT INTO resposta_etapa (encaminhamento_id, etapa, morador_id, resposta) VALUES (:e,'RECEBIMENTO',:m,'NAO')")
                .param("e", enc).param("m", m2).update();
        jdbc.sql("INSERT INTO historico_etapa (encaminhamento_id, etapa_anterior, etapa_nova, data) VALUES (:e,NULL,'RECEBIMENTO','2026-03-10 08:00:00')")
                .param("e", enc).update();
        jdbc.sql("INSERT INTO historico_etapa (encaminhamento_id, etapa_anterior, etapa_nova, data) VALUES (:e,'RECEBIMENTO','MOBILIZACAO','2026-03-11 08:00:00')")
                .param("e", enc).update();
        mvc.perform(get("/encaminhamentos/" + enc + "/progresso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapaAtual").value("MOBILIZACAO"))
                .andExpect(jsonPath("$.respostasPorEtapa.RECEBIMENTO.SIM").value(1))
                .andExpect(jsonPath("$.respostasPorEtapa.RECEBIMENTO.NAO").value(1))
                .andExpect(jsonPath("$.respostasPorEtapa.RECEBIMENTO.NAO_SEI").value(0))
                .andExpect(jsonPath("$.respostasPorEtapa.IMPACTO.AJUDOU").value(0))
                .andExpect(jsonPath("$.historico[0].etapaNova").value("RECEBIMENTO"))
                .andExpect(jsonPath("$.historico[1].etapaNova").value("MOBILIZACAO"));
    }

    @Test
    void bairroSemEncaminhamentosRetornaZerado() throws Exception {
        mvc.perform(get("/indicadores/impacto").param("bairro", "Bairro Inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(0))
                .andExpect(jsonPath("$.percentualAjudou").value(0.0))
                .andExpect(jsonPath("$.tempoMedioExecucaoSegundos").doesNotExist());
    }

    @Test
    void indicadorCalculaComDadosEPeriodo() throws Exception {
        String bairro = "Indicador-" + UUID.randomUUID();
        long a = encaminhamento(bairro, "IMPACTO");
        long b = encaminhamento(bairro, "IMPACTO");
        long c = encaminhamento(bairro, "EXECUCAO");
        long m = 9100 + (a % 1000);
        morador(m);
        // a: AJUDOU, execução em 10h; b: PARCIALMENTE, execução em 20h; c: sem impacto, execução em 30h
        fechar(a, m, "AJUDOU", "2026-03-10 18:00:00");
        fechar(b, m, "AJUDOU_PARCIALMENTE", "2026-03-10 04:00:00");
        jdbc.sql("UPDATE etapa_confirmacao SET confirmada_em = '2026-03-11 04:00:00' WHERE encaminhamento_id = :e AND etapa = 'EXECUCAO'")
                .param("e", b).update();
        jdbc.sql("INSERT INTO etapa_confirmacao (encaminhamento_id, etapa, confirmada_em) VALUES (:e,'EXECUCAO','2026-03-11 14:00:00')")
                .param("e", c).update();
        mvc.perform(get("/indicadores/impacto").param("bairro", bairro).param("periodo", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(3))
                .andExpect(jsonPath("$.impactosFechados").value(2))
                .andExpect(jsonPath("$.percentualAjudou").value(50.0))
                .andExpect(jsonPath("$.percentualAjudouParcialmente").value(50.0))
                .andExpect(jsonPath("$.tempoMedioExecucaoSegundos").value(20 * 3600));
        mvc.perform(get("/indicadores/impacto").param("bairro", bairro).param("periodo", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaminhamentos").value(0));
        mvc.perform(get("/indicadores/impacto").param("bairro", bairro)
                        .param("periodo", "2026-03-10/2026-03-10"))
                .andExpect(jsonPath("$.totalEncaminhamentos").value(3));
    }

    private void fechar(long enc, long morador, String resposta, String execucao) {
        jdbc.sql("INSERT INTO resposta_etapa (encaminhamento_id, etapa, morador_id, resposta) VALUES (:e,'IMPACTO',:m,:r)")
                .param("e", enc).param("m", morador).param("r", resposta).update();
        jdbc.sql("INSERT INTO etapa_confirmacao (encaminhamento_id, etapa, confirmada_em) VALUES (:e,'IMPACTO','2026-03-20 00:00:00')")
                .param("e", enc).update();
        jdbc.sql("INSERT INTO etapa_confirmacao (encaminhamento_id, etapa, confirmada_em) VALUES (:e,'EXECUCAO',:t)")
                .param("e", enc).param("t", java.sql.Timestamp.valueOf(execucao)).update();
    }

    @Test
    void periodoInvalidoDa422() throws Exception {
        mvc.perform(get("/indicadores/impacto").param("periodo", "marco")).andExpect(status().isUnprocessableEntity());
    }
}
