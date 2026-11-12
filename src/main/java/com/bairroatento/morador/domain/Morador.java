package com.bairroatento.morador.domain;

import com.bairroatento.shared.domain.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "morador")
public class Morador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(name = "cpf_hash", nullable = false)
    private String cpfHash;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    protected Morador() {}

    public Morador(String nome, String email, Cpf cpf) {
        if (nome == null || nome.isBlank()) throw new RegraNegocioException("Nome é obrigatório");
        if (email == null || email.isBlank()) throw new RegraNegocioException("E-mail é obrigatório");
        this.nome = nome.trim();
        this.email = normalizarEmail(email);
        this.cpfHash = cpf.hash();
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getCpfHash() { return cpfHash; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
}
