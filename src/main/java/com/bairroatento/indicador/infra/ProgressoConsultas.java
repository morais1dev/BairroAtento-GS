package com.bairroatento.indicador.infra;

import com.bairroatento.indicador.domain.Progresso;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Consultas somente-leitura do progresso de um encaminhamento. */
@Repository
public class ProgressoConsultas {

    private final JdbcClient jdbc;

    public ProgressoConsultas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<String> etapaAtual(long encaminhamentoId) {
        return jdbc.sql("SELECT etapa_atual FROM encaminhamento WHERE id = :id")
                .param("id", encaminhamentoId).query(String.class).optional();
    }

    /** Contagem bruta etapa → (resposta → n), apenas da rodada atual do encaminhamento. */
    public Map<String, Map<String, Long>> respostas(long encaminhamentoId) {
        Map<String, Map<String, Long>> out = new HashMap<>();
        jdbc.sql("SELECT etapa, resposta, COUNT(*) AS n FROM resposta_etapa "
                        + "WHERE encaminhamento_id = :id "
                        + "AND rodada = (SELECT rodada_atual FROM encaminhamento WHERE id = :id) "
                        + "GROUP BY etapa, resposta")
                .param("id", encaminhamentoId)
                .query((rs, i) -> {
                    out.computeIfAbsent(rs.getString("etapa"), k -> new HashMap<>())
                            .put(rs.getString("resposta"), rs.getLong("n"));
                    return null;
                }).list();
        return out;
    }

    public List<Progresso.MudancaEtapa> historico(long encaminhamentoId) {
        return jdbc.sql("SELECT etapa_anterior, etapa_nova, data FROM historico_etapa "
                        + "WHERE encaminhamento_id = :id ORDER BY data, id")
                .param("id", encaminhamentoId)
                .query((rs, i) -> new Progresso.MudancaEtapa(rs.getString("etapa_anterior"),
                        rs.getString("etapa_nova"), rs.getTimestamp("data").toLocalDateTime()))
                .list();
    }
}
