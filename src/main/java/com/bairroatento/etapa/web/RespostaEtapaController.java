package com.bairroatento.etapa.web;

import com.bairroatento.etapa.application.RespostaEtapaService;
import com.bairroatento.etapa.domain.Decisao;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Etapas", description = "Respostas dos moradores às etapas do encaminhamento e histórico")
public class RespostaEtapaController {

    @Schema(name = "RespostaRequest")
    public record RespostaRequest(
            @Schema(description = "Id do morador que responde", example = "1") @NotNull Long moradorId,
            @Schema(description = "SIM, NAO ou NAO_SEI (RECEBIMENTO, MOBILIZACAO, EXECUCAO); "
                    + "AJUDOU, AJUDOU_PARCIALMENTE ou NAO_AJUDOU (IMPACTO). Sem diferenciar maiúsculas.",
                    example = "SIM") @NotBlank String resposta) {}

    static final String EX = """
            {"encaminhamentoId":3,"etapaRespondida":"RECEBIMENTO","rodadaDaResposta":1,"resposta":"SIM",            "desfecho":"PENDENTE","etapaAtual":"RECEBIMENTO","rodadaAtual":1,"concluido":false,            "resultadoImpacto":null,"placar":{"SIM":1,"NAO":0,"NAO_SEI":0}}""";

    @Schema(name = "RespostaEtapaResponse")
    public record RespostaEtapaResponse(Long encaminhamentoId, Etapa etapaRespondida, int rodadaDaResposta,
                                        OpcaoResposta resposta,
                                        @Schema(description = "PENDENTE, CONFIRMADA, REABERTA ou CONCLUIDA")
                                        Decisao.Desfecho desfecho, Etapa etapaAtual,
                                        int rodadaAtual, boolean concluido,
                                        @Schema(nullable = true) OpcaoResposta resultadoImpacto,
                                        @Schema(description = "Contagem de votos da etapa respondida, por opção")
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
    @Operation(summary = "Responder uma etapa",
            description = "Registra o voto de um morador na etapa atual do encaminhamento. Ao atingir o mínimo "
                    + "de respostas do bairro a etapa é confirmada (avança) ou reaberta; IMPACTO conclui o andamento.")
    @ApiResponse(responseCode = "201", description = "Resposta registrada", content = @Content(
            schema = @Schema(implementation = RespostaEtapaResponse.class), examples = @ExampleObject(value = EX)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos, etapa ou resposta desconhecida, JSON malformado")
    @ApiResponse(responseCode = "404", description = "Encaminhamento ou morador inexistente")
    @ApiResponse(responseCode = "409", description = "Etapa fora de ordem, resposta duplicada ou andamento concluído")
    @ApiResponse(responseCode = "422", description = "Morador não autorizado, opção inválida para a etapa "
            + "ou IMPACTO antes de EXECUCAO confirmada")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public RespostaEtapaResponse responder(@PathVariable Long id,
                                           @Parameter(description = "RECEBIMENTO, MOBILIZACAO, EXECUCAO ou IMPACTO",
                                                   example = "RECEBIMENTO") @PathVariable String etapa,
                                           @Valid @RequestBody RespostaRequest r) {
        Etapa e = Etapa.de(etapa).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapa desconhecida: " + etapa));
        OpcaoResposta o = OpcaoResposta.de(r.resposta()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta desconhecida: " + r.resposta()));
        return RespostaEtapaResponse.de(id, service.responder(id, e, r.moradorId(), o));
    }

    @GetMapping("/encaminhamentos/{id}/historico")
    @Operation(summary = "Histórico de mudanças de etapa",
            description = "Lista, em ordem, a criação do encaminhamento e cada confirmação ou reabertura de etapa.")
    @ApiResponse(responseCode = "200", description = "Histórico", content = @Content(
            array = @ArraySchema(schema = @Schema(implementation = HistoricoEtapaResponse.class)),
            examples = @ExampleObject(value = "[{\"etapaAnterior\":null,\"etapaNova\":\"RECEBIMENTO\",\"rodada\":1,"
                    + "\"data\":\"2026-11-12T10:20:00\"},{\"etapaAnterior\":\"RECEBIMENTO\",\"etapaNova\":\"MOBILIZACAO\","
                    + "\"rodada\":1,\"data\":\"2026-11-12T11:00:00\"}]")))
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Encaminhamento inexistente")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public List<HistoricoEtapaResponse> historico(@PathVariable Long id) {
        return service.historico(id).stream().map(HistoricoEtapaResponse::de).toList();
    }
}
