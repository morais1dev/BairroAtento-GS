package com.bairroatento.indicador.application;

import com.bairroatento.indicador.domain.Progresso;
import com.bairroatento.indicador.infra.ProgressoConsultas;
import com.bairroatento.shared.domain.NaoEncontradoException;
import org.springframework.stereotype.Service;

@Service
public class ProgressoService {

    private final ProgressoConsultas consultas;

    public ProgressoService(ProgressoConsultas consultas) {
        this.consultas = consultas;
    }

    public Progresso progresso(long encaminhamentoId) {
        String etapa = consultas.etapaAtual(encaminhamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Encaminhamento não encontrado: " + encaminhamentoId));
        return new Progresso(encaminhamentoId, etapa,
                Progresso.contagem(consultas.respostas(encaminhamentoId)),
                consultas.historico(encaminhamentoId));
    }
}
