package com.bairroatento.encaminhamento.infra;

import com.bairroatento.encaminhamento.domain.Encaminhamento;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, Long> {

    List<Encaminhamento> findByOcorrenciaIdOrderByIdAsc(Long ocorrenciaId);

    List<Encaminhamento> findByOcorrenciaIdAndDestinatarioIdIn(Long ocorrenciaId, Collection<Long> destinatarioIds);

    /** Bloqueia a linha para serializar respostas concorrentes do mesmo encaminhamento. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Encaminhamento e where e.id = :id")
    Optional<Encaminhamento> buscarParaAtualizar(@Param("id") Long id);
}
