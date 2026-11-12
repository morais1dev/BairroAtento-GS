package com.bairroatento.ocorrencia.infra;

import com.bairroatento.ocorrencia.domain.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNome(String nome);
}
