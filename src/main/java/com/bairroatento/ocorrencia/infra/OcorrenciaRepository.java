package com.bairroatento.ocorrencia.infra;

import com.bairroatento.ocorrencia.domain.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OcorrenciaRepository
        extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {}
