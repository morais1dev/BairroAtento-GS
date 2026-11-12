package com.bairroatento.etapa.infra;

import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import com.bairroatento.etapa.domain.Voto;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistência das tabelas de apoio do andamento (respostas, confirmações, histórico,
 * configuração por bairro e confirmadores da ocorrência).
 */
@Repository
public class RegistroEtapas {

    public record ItemHistorico(Etapa etapaAnterior, Etapa etapaNova, int rodada, LocalDateTime data) {}

    private final JdbcTemplate jdbc;

    public RegistroEtapas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Voto> votos(long encaminhamentoId, Etapa etapa, int rodada) {
        return jdbc.query("""
                select morador_id, resposta from resposta_etapa
                where encaminhamento_id = ? and etapa = ? and rodada = ? order by id""",
                (rs, i) -> new Voto(rs.getLong("morador_id"), OpcaoResposta.valueOf(rs.getString("resposta"))),
                encaminhamentoId, etapa.name(), rodada);
    }

    public void salvarResposta(long encaminhamentoId, Etapa etapa, int rodada, Voto voto) {
        jdbc.update("""
                insert into resposta_etapa (encaminhamento_id, etapa, rodada, morador_id, resposta, data)
                values (?, ?, ?, ?, ?, ?)""",
                encaminhamentoId, etapa.name(), rodada, voto.moradorId(), voto.opcao().name(), agora());
    }

    public void registrarConfirmacao(long encaminhamentoId, Etapa etapa, int rodada) {
        jdbc.update("insert into etapa_confirmacao (encaminhamento_id, etapa, rodada, confirmada_em) values (?, ?, ?, ?)",
                encaminhamentoId, etapa.name(), rodada, agora());
    }

    public void registrarHistorico(long encaminhamentoId, Etapa anterior, Etapa nova, int rodada) {
        jdbc.update("""
                insert into historico_etapa (encaminhamento_id, etapa_anterior, etapa_nova, rodada, data)
                values (?, ?, ?, ?, ?)""",
                encaminhamentoId, anterior == null ? null : anterior.name(), nova.name(), rodada, agora());
    }

    public List<ItemHistorico> historico(long encaminhamentoId) {
        return jdbc.query("""
                select etapa_anterior, etapa_nova, rodada, data from historico_etapa
                where encaminhamento_id = ? order by id""",
                (rs, i) -> new ItemHistorico(
                        rs.getString("etapa_anterior") == null ? null : Etapa.valueOf(rs.getString("etapa_anterior")),
                        Etapa.valueOf(rs.getString("etapa_nova")),
                        rs.getInt("rodada"),
                        rs.getTimestamp("data").toLocalDateTime()),
                encaminhamentoId);
    }

    /** Moradores que confirmaram a ocorrência (tabela confirmacao_ocorrencia). */
    public Set<Long> confirmadores(long ocorrenciaId) {
        return new HashSet<>(jdbc.queryForList(
                "select morador_id from confirmacao_ocorrencia where ocorrencia_id = ?", Long.class, ocorrenciaId));
    }

    /** Mínimo configurado para o bairro em config_regiao, se houver linha. */
    public Optional<Integer> minimoDoBairro(String bairro) {
        return jdbc.queryForList("select minimo_confirmacoes from config_regiao where bairro = ?", Integer.class, bairro)
                .stream().findFirst();
    }

    private static Timestamp agora() {
        return Timestamp.valueOf(LocalDateTime.now());
    }
}
