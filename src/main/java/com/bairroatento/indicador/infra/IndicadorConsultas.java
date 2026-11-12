package com.bairroatento.indicador.infra;

import com.bairroatento.indicador.domain.EncaminhamentoResumo;
import com.bairroatento.indicador.domain.ResultadoImpacto;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Consultas somente-leitura sobre as tabelas do motor de etapas. */
@Repository
public class IndicadorConsultas {

    private final JdbcClient jdbc;

    public IndicadorConsultas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<EncaminhamentoResumo> resumos(String bairro, LocalDate inicio, LocalDate fim) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.data_envio,
                       (SELECT c.confirmada_em FROM etapa_confirmacao c
                         WHERE c.encaminhamento_id = e.id AND c.etapa = 'EXECUCAO') AS execucao,
                       (SELECT COUNT(*) FROM etapa_confirmacao c
                         WHERE c.encaminhamento_id = e.id AND c.etapa = 'IMPACTO') AS impacto_fechado
                  FROM encaminhamento e JOIN ocorrencia o ON o.id = e.ocorrencia_id
                 WHERE 1 = 1
                """);
        Map<String, Object> p = new HashMap<>();
        if (bairro != null && !bairro.isBlank()) {
            sql.append(" AND LOWER(o.bairro) = LOWER(:bairro)");
            p.put("bairro", bairro.trim());
        }
        if (inicio != null) {
            sql.append(" AND e.data_envio >= :inicio");
            p.put("inicio", Timestamp.valueOf(inicio.atStartOfDay()));
        }
        if (fim != null) {
            sql.append(" AND e.data_envio < :fim");
            p.put("fim", Timestamp.valueOf(fim.plusDays(1).atStartOfDay()));
        }
        List<Object[]> linhas = jdbc.sql(sql.toString()).params(p).query((rs, n) -> new Object[] {
                rs.getLong("id"), rs.getTimestamp("data_envio"), rs.getTimestamp("execucao"),
                rs.getLong("impacto_fechado") > 0}).list();
        List<EncaminhamentoResumo> out = new ArrayList<>();
        for (Object[] l : linhas) {
            Timestamp envio = (Timestamp) l[1];
            Timestamp exec = (Timestamp) l[2];
            Duration tempo = exec == null ? null : Duration.between(envio.toInstant(), exec.toInstant());
            ResultadoImpacto r = (Boolean) l[3] ? resultadoImpacto((Long) l[0]) : null;
            out.add(new EncaminhamentoResumo(r, tempo));
        }
        return out;
    }

    private ResultadoImpacto resultadoImpacto(long encaminhamentoId) {
        Map<ResultadoImpacto, Long> votos = new EnumMap<>(ResultadoImpacto.class);
        jdbc.sql("SELECT resposta, COUNT(*) AS n FROM resposta_etapa "
                        + "WHERE encaminhamento_id = :id AND etapa = 'IMPACTO' GROUP BY resposta")
                .param("id", encaminhamentoId)
                .query((rs, i) -> {
                    try {
                        votos.put(ResultadoImpacto.valueOf(rs.getString("resposta")), rs.getLong("n"));
                    } catch (IllegalArgumentException ignorada) {
                        // resposta fora das opções de impacto: não conta
                    }
                    return null;
                }).list();
        return ResultadoImpacto.maisVotado(votos).orElse(null);
    }
}
