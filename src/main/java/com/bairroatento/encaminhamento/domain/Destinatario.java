package com.bairroatento.encaminhamento.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Órgão (fictício) que recebe encaminhamentos. Cadastrado por seed. */
@Entity
@Table(name = "destinatario")
public class Destinatario {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String tipo;

    @Column(name = "area_atuacao")
    private String areaAtuacao;

    protected Destinatario() {}

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getAreaAtuacao() { return areaAtuacao; }
}
