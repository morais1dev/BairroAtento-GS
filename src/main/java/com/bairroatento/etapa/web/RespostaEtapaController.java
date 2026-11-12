package com.bairroatento.etapa.web;

import com.bairroatento.etapa.application.RespostaEtapaService;
import com.bairroatento.etapa.domain.Decisao;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RespostaEtapaController {

    public record RespostaRequest(@NotNull Long moradorId, @NotBlank String resposta) {}

    public record RespostaEtapaResponse(Long encaminhamentoId, Etapa etapaRespondida, int rodadaDaResposta,
                                        OpcaoResposta resposta, Decisao.Desfecho desfecho, Etapa etapaAtual,
                                        int rodadaAtual, boolean concluido, OpcaoResposta resultadoImpacto,
                                        Map<OpcaoResposta, Integer> placar) {
        static RespostaEtapaResponse de(Long encaminhamentoId, Decisao d) {
            Map<OpcaoResposta, Integer> placar = new EnumMap<>(OpcaoResposta.class);
            for (OpcaoResposta o : OpcaoResposta.values()) {
                if (o.validaPara(d.etapaRespondida())) placar.put(o, d.placar().de(o));
            }
            return new RespostaEtapaResponse(encaminhamentoId, d.etapaRespondida(), d.rodadaDaResposta(),
                    d.voto().opcao(), d.desfecho(), d.novo().etapaAtual(), d.novo().rodada(),
                    d.novo().concluido(), d.novo().resultadoImpacto(), placar);
        }
    }

    private final RespostaEtapaService service;

    public RespostaEtapaController(RespostaEtapaService service) {
        this.service = service;
    }

    @PostMapping("/encaminhamentos/{id}/etapas/{etapa}/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaEtapaResponse responder(@PathVariable Long id, @PathVariable String etapa,
                                           @Valid @RequestBody RespostaRequest r) {
        Etapa e = Etapa.de(etapa).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapa desconhecida: " + etapa));
        OpcaoResposta o = OpcaoResposta.de(r.resposta()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta desconhecida: " + r.resposta()));
        return RespostaEtapaResponse.de(id, service.responder(id, e, r.moradorId(), o));
    }

    @GetMapping("/encaminhamentos/{id}/historico")
    public List<HistoricoEtapaResponse> historico(@PathVariable Long id) {
        return service.historico(id).stream().map(HistoricoEtapaResponse::de).toList();
    }
}
