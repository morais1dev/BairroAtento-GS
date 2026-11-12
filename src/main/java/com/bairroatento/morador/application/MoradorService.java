package com.bairroatento.morador.application;

import com.bairroatento.morador.domain.Cpf;
import com.bairroatento.morador.domain.Morador;
import com.bairroatento.morador.infra.MoradorRepository;
import com.bairroatento.shared.domain.ConflitoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoradorService {

    private final MoradorRepository repo;

    public MoradorService(MoradorRepository repo) {
        this.repo = repo;
    }

    public record Cadastro(Morador morador, Cpf cpf) {}

    @Transactional
    public Cadastro cadastrar(String nome, String email, String cpfTexto) {
        Cpf cpf = Cpf.de(cpfTexto);
        if (repo.existsByEmail(Morador.normalizarEmail(email))) {
            throw new ConflitoException("E-mail já cadastrado");
        }
        return new Cadastro(repo.save(new Morador(nome, email, cpf)), cpf);
    }
}
