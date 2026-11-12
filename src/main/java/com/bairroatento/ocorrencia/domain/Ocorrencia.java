package com.bairroatento.ocorrencia.domain;

import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ocorrencia")
public class Ocorrencia {

    public static final String STATUS_INICIAL = "ABERTA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private String endereco;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String status = STATUS_INICIAL;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    // Persistido apenas por UPDATE atômico (OcorrenciaRepository.incrementarContador).
    @Column(name = "contador_confirmacoes", nullable = false, updatable = false)
    private int contadorConfirmacoes;

    @ElementCollection
    @CollectionTable(name = "confirmacao_ocorrencia", joinColumns = @JoinColumn(name = "ocorrencia_id"))
    @Column(name = "morador_id")
    private Set<Long> confirmadoPor = new HashSet<>();

    protected Ocorrencia() {}

    public Ocorrencia(String categoria, String descricao, String endereco, String bairro,
                      Double latitude, Double longitude, Long autorId) {
        this.categoria = obrigatorio(categoria, "Categoria");
        this.descricao = obrigatorio(descricao, "Descrição");
        this.endereco = obrigatorio(endereco, "Endereço");
        this.bairro = obrigatorio(bairro, "Bairro");
        if (autorId == null) throw new RegraNegocioException("Autor é obrigatório");
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new RegraNegocioException("Latitude fora do intervalo [-90, 90]");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new RegraNegocioException("Longitude fora do intervalo [-180, 180]");
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.autorId = autorId;
    }

    private static String obrigatorio(String v, String campo) {
        if (v == null || v.isBlank()) throw new RegraNegocioException(campo + " é obrigatório(a)");
        return v.trim();
    }

    /** Cada morador confirma uma única vez; a confirmação incrementa o contador. */
    public void confirmar(Long moradorId) {
        if (!confirmadoPor.add(moradorId)) {
            throw new ConflitoException("Morador já confirmou esta ocorrência");
        }
        contadorConfirmacoes++;
    }

    public Long getId() { return id; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public String getEndereco() { return endereco; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getBairro() { return bairro; }
    public String getStatus() { return status; }
    public Long getAutorId() { return autorId; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public int getContadorConfirmacoes() { return contadorConfirmacoes; }
}
