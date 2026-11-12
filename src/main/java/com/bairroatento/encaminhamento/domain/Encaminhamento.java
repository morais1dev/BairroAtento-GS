package com.bairroatento.encaminhamento.domain;

import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.EstadoAndamento;
import com.bairroatento.etapa.domain.OpcaoResposta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/** Envio de uma ocorrência a um destinatário, com andamento por etapas independente. */
@Entity
@Table(name = "encaminhamento")
public class Encaminhamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ocorrencia_id", nullable = false)
    private Long ocorrenciaId;

    @Column(name = "destinatario_id", nullable = false)
    private Long destinatarioId;

    @Column(nullable = false)
    private String protocolo;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio = LocalDateTime.now();

    // Enums guardados como texto e convertidos nos acessores (as colunas são VARCHAR).
    @Column(name = "etapa_atual", nullable = false)
    private String etapaAtual;

    @Column(name = "rodada_atual", nullable = false)
    private int rodadaAtual;

    @Column(name = "resultado_impacto")
    private String resultadoImpacto;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    protected Encaminhamento() {}

    public Encaminhamento(Long ocorrenciaId, Long destinatarioId, String protocolo) {
        this.ocorrenciaId = Objects.requireNonNull(ocorrenciaId, "ocorrenciaId");
        this.destinatarioId = Objects.requireNonNull(destinatarioId, "destinatarioId");
        this.protocolo = Objects.requireNonNull(protocolo, "protocolo");
        aplicar(EstadoAndamento.inicial());
    }

    public EstadoAndamento estado() {
        return new EstadoAndamento(Etapa.valueOf(etapaAtual), rodadaAtual,
                resultadoImpacto == null ? null : OpcaoResposta.valueOf(resultadoImpacto));
    }

    /** Assume o estado decidido pelo motor de etapas. */
    public void aplicar(EstadoAndamento novo) {
        this.etapaAtual = novo.etapaAtual().name();
        this.rodadaAtual = novo.rodada();
        this.resultadoImpacto = novo.resultadoImpacto() == null ? null : novo.resultadoImpacto().name();
        if (novo.concluido() && dataConclusao == null) dataConclusao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOcorrenciaId() { return ocorrenciaId; }
    public Long getDestinatarioId() { return destinatarioId; }
    public String getProtocolo() { return protocolo; }
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public Etapa getEtapaAtual() { return Etapa.valueOf(etapaAtual); }
    public int getRodadaAtual() { return rodadaAtual; }
    public OpcaoResposta getResultadoImpacto() {
        return resultadoImpacto == null ? null : OpcaoResposta.valueOf(resultadoImpacto);
    }
    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public boolean isConcluido() { return resultadoImpacto != null; }
}
