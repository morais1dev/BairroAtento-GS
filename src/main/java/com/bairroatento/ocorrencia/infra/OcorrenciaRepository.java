package com.bairroatento.ocorrencia.infra;

import com.bairroatento.ocorrencia.domain.Ocorrencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcorrenciaRepository
        extends JpaRepository<Ocorrencia, Long>, JpaSpecificationExecutor<Ocorrencia> {

    /** Incremento atômico no banco: sem perda de contagem em confirmações simultâneas. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Ocorrencia o set o.contadorConfirmacoes = o.contadorConfirmacoes + 1 where o.id = :id")
    int incrementarContador(@Param("id") Long id);
}
