package com.bairroatento.indicador.infra;

import com.bairroatento.indicador.domain.EncaminhamentoResumo;
import com.bairroatento.indicador.domain.ResultadoImpacto;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
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
                SELECT e.data_envio, e.resultado_impacto,
                       (SELECT MIN(c.confirmada_em) FROM etapa_confirmacao c
                         WHERE c.encaminhamento_id = e.id AND c.etapa = 'EXECUCAO') AS execucao
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
        return jdbc.sql(sql.toString()).params(p).query((rs, n) -> {
            Timestamp envio = rs.getTimestamp("data_envio");
            Timestamp exec = rs.getTimestamp("execucao");
            Duration tempo = exec == null ? null : Duration.between(envio.toInstant(), exec.toInstant());
            String res = rs.getString("resultado_impacto");
            return new EncaminhamentoResumo(res == null ? null : ResultadoImpacto.valueOf(res), tempo);
        }).list();
    }
}
