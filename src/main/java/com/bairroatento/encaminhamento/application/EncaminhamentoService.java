package com.bairroatento.encaminhamento.application;

import com.bairroatento.encaminhamento.domain.Destinatario;
import com.bairroatento.encaminhamento.domain.Encaminhamento;
import com.bairroatento.encaminhamento.domain.GeradorProtocolo;
import com.bairroatento.encaminhamento.infra.DestinatarioRepository;
import com.bairroatento.encaminhamento.infra.EncaminhamentoRepository;
import com.bairroatento.etapa.infra.RegistroEtapas;
import com.bairroatento.ocorrencia.infra.OcorrenciaRepository;
import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.NaoEncontradoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EncaminhamentoService {

    private final EncaminhamentoRepository encaminhamentos;
    private final DestinatarioRepository destinatarios;
    private final OcorrenciaRepository ocorrencias;
    private final RegistroEtapas registro;
    private final GeradorProtocolo protocolos;

    public EncaminhamentoService(EncaminhamentoRepository encaminhamentos, DestinatarioRepository destinatarios,
                                 OcorrenciaRepository ocorrencias, RegistroEtapas registro,
                                 GeradorProtocolo protocolos) {
        this.encaminhamentos = encaminhamentos;
        this.destinatarios = destinatarios;
        this.ocorrencias = ocorrencias;
        this.registro = registro;
        this.protocolos = protocolos;
    }

    /**
     * Cria um encaminhamento por destinatário (ids repetidos contam uma vez), cada um com protocolo
     * próprio e começando em RECEBIMENTO. Se algum destinatário não existir, ou se a ocorrência já
     * tiver sido encaminhada a algum deles, o pedido inteiro é recusado.
     */
    @Transactional
    public List<Encaminhamento> encaminhar(Long ocorrenciaId, List<Long> destinatarioIds) {
        if (!ocorrencias.existsById(ocorrenciaId)) {
            throw new NaoEncontradoException("Ocorrência " + ocorrenciaId + " não encontrada");
        }
        if (destinatarioIds == null || destinatarioIds.isEmpty() || destinatarioIds.contains(null)) {
            throw new RegraNegocioException("Informe pelo menos um destinatário");
        }
        Set<Long> ids = new LinkedHashSet<>(destinatarioIds);
        Set<Long> existentes = destinatarios.findAllById(ids).stream()
                .map(Destinatario::getId).collect(Collectors.toSet());
        List<Long> faltando = ids.stream().filter(id -> !existentes.contains(id)).toList();
        if (!faltando.isEmpty()) {
            throw new NaoEncontradoException("Destinatário(s) não encontrado(s): " + faltando);
        }
        List<Long> jaEncaminhados = encaminhamentos.findByOcorrenciaIdAndDestinatarioIdIn(ocorrenciaId, ids)
                .stream().map(Encaminhamento::getDestinatarioId).toList();
        if (!jaEncaminhados.isEmpty()) {
            throw new ConflitoException("Ocorrência já encaminhada ao(s) destinatário(s): " + jaEncaminhados);
        }

        List<Encaminhamento> criados = new ArrayList<>();
        for (Long destinatarioId : ids) {
            Encaminhamento e = encaminhamentos.save(new Encaminhamento(
                    ocorrenciaId, destinatarioId, protocolos.gerar(ocorrenciaId, destinatarioId)));
            registro.registrarHistorico(e.getId(), null, e.getEtapaAtual(), e.getRodadaAtual());
            criados.add(e);
        }
        return criados;
    }

    @Transactional(readOnly = true)
    public Encaminhamento buscar(Long id) {
        return encaminhamentos.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NaoEncontradoException("Encaminhamento " + id + " não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Encaminhamento> listarDaOcorrencia(Long ocorrenciaId) {
        if (!ocorrencias.existsById(ocorrenciaId)) {
            throw new NaoEncontradoException("Ocorrência " + ocorrenciaId + " não encontrada");
        }
        return encaminhamentos.findByOcorrenciaIdOrderByIdAsc(ocorrenciaId);
    }
}
