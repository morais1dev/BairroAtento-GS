package com.bairroatento.morador.infra;

import com.bairroatento.morador.domain.Morador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoradorRepository extends JpaRepository<Morador, Long> {
    boolean existsByEmail(String email);
}
