package com.bairroatento.encaminhamento.infra;

import com.bairroatento.encaminhamento.domain.Destinatario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinatarioRepository extends JpaRepository<Destinatario, Long> {}
