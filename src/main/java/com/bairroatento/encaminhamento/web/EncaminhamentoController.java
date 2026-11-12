package com.bairroatento.encaminhamento.web;

import com.bairroatento.encaminhamento.application.EncaminhamentoService;
import com.bairroatento.encaminhamento.domain.Encaminhamento;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EncaminhamentoController {

    public record EncaminharRequest(@NotEmpty List<@NotNull Long> destinatarioIds) {}

    public record EncaminhamentoResponse(Long id, Long ocorrenciaId, Long destinatarioId, String protocolo,
                                         LocalDateTime dataEnvio, Etapa etapaAtual, int rodada,
                                         boolean concluido, OpcaoResposta resultadoImpacto,
                                         LocalDateTime dataConclusao) {
        public static EncaminhamentoResponse de(Encaminhamento e) {
            return new EncaminhamentoResponse(e.getId(), e.getOcorrenciaId(), e.getDestinatarioId(),
                    e.getProtocolo(), e.getDataEnvio(), e.getEtapaAtual(), e.getRodadaAtual(),
                    e.isConcluido(), e.getResultadoImpacto(), e.getDataConclusao());
        }
    }

    private final EncaminhamentoService service;

    public EncaminhamentoController(EncaminhamentoService service) {
        this.service = service;
    }

    @PostMapping("/ocorrencias/{id}/encaminhamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EncaminhamentoResponse> encaminhar(@PathVariable Long id, @Valid @RequestBody EncaminharRequest r) {
        return service.encaminhar(id, r.destinatarioIds()).stream().map(EncaminhamentoResponse::de).toList();
    }

    @GetMapping("/ocorrencias/{id}/encaminhamentos")
    public List<EncaminhamentoResponse> listar(@PathVariable Long id) {
        return service.listarDaOcorrencia(id).stream().map(EncaminhamentoResponse::de).toList();
    }

    @GetMapping("/encaminhamentos/{id}")
    public EncaminhamentoResponse detalhar(@PathVariable Long id) {
        return EncaminhamentoResponse.de(service.buscar(id));
    }
}
