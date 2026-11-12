package com.bairroatento.etapa.application;

import com.bairroatento.encaminhamento.domain.Encaminhamento;
import com.bairroatento.encaminhamento.infra.EncaminhamentoRepository;
import com.bairroatento.etapa.domain.Decisao;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.MotorEtapas;
import com.bairroatento.etapa.domain.OpcaoResposta;
import com.bairroatento.etapa.domain.Voto;
import com.bairroatento.etapa.infra.EtapasProperties;
import com.bairroatento.etapa.infra.RegistroEtapas;
import com.bairroatento.morador.infra.MoradorRepository;
import com.bairroatento.ocorrencia.domain.Ocorrencia;
import com.bairroatento.ocorrencia.infra.OcorrenciaRepository;
import com.bairroatento.shared.domain.NaoEncontradoException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o andamento, consulta o motor de etapas e grava o que ele decidir. */
@Service
public class RespostaEtapaService {

    private final EncaminhamentoRepository encaminhamentos;
    private final OcorrenciaRepository ocorrencias;
    private final MoradorRepository moradores;
    private final RegistroEtapas registro;
    private final EtapasProperties propriedades;

    public RespostaEtapaService(EncaminhamentoRepository encaminhamentos, OcorrenciaRepository ocorrencias,
                                MoradorRepository moradores, RegistroEtapas registro,
                                EtapasProperties propriedades) {
        this.encaminhamentos = encaminhamentos;
        this.ocorrencias = ocorrencias;
        this.moradores = moradores;
        this.registro = registro;
        this.propriedades = propriedades;
    }

    @Transactional
    public Decisao responder(Long encaminhamentoId, Etapa etapa, Long moradorId, OpcaoResposta opcao) {
        Encaminhamento enc = encaminhamentos.buscarParaAtualizar(encaminhamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Encaminhamento " + encaminhamentoId + " não encontrado"));
        if (!moradores.existsById(moradorId)) {
            throw new NaoEncontradoException("Morador " + moradorId + " não encontrado");
        }
        Ocorrencia ocorrencia = ocorrencias.findById(enc.getOcorrenciaId())
                .orElseThrow(() -> new NaoEncontradoException("Ocorrência " + enc.getOcorrenciaId() + " não encontrada"));

        Set<Long> autorizados = new HashSet<>(registro.confirmadores(ocorrencia.getId()));
        autorizados.add(ocorrencia.getAutorId());
        int minimo = minimoPara(ocorrencia.getBairro());
        var estado = enc.estado();
        List<Voto> votos = registro.votos(enc.getId(), estado.etapaAtual(), estado.rodada());

        Decisao d = MotorEtapas.responder(estado, votos, etapa, new Voto(moradorId, opcao), autorizados, minimo);

        registro.salvarResposta(enc.getId(), d.etapaRespondida(), d.rodadaDaResposta(), d.voto());
        switch (d.desfecho()) {
            case CONFIRMADA, CONCLUIDA -> registro.registrarConfirmacao(enc.getId(), d.etapaRespondida(), d.rodadaDaResposta());
            case REABERTA, PENDENTE -> { }
        }
        if (d.gerouTransicao()) {
            registro.registrarHistorico(enc.getId(), d.anterior().etapaAtual(), d.novo().etapaAtual(), d.novo().rodada());
        }
        enc.aplicar(d.novo());
        return d;
    }

    @Transactional(readOnly = true)
    public List<RegistroEtapas.ItemHistorico> historico(Long encaminhamentoId) {
        if (!encaminhamentos.existsById(encaminhamentoId)) {
            throw new NaoEncontradoException("Encaminhamento " + encaminhamentoId + " não encontrado");
        }
        return registro.historico(encaminhamentoId);
    }

    /**
     * Mínimo do bairro em config_regiao, ou o padrão configurado quando não há linha ou o valor
     * gravado é inválido (&lt; 1).
     */
    int minimoPara(String bairro) {
        return registro.minimoDoBairro(bairro).filter(m -> m >= 1).orElse(propriedades.minimoPadrao());
    }
}
