package com.bairroatento.encaminhamento.web;

import com.bairroatento.encaminhamento.application.EncaminhamentoService;
import com.bairroatento.encaminhamento.domain.Encaminhamento;
import com.bairroatento.etapa.domain.Etapa;
import com.bairroatento.etapa.domain.OpcaoResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Encaminhamentos", description = "Envio de ocorrências a destinatários e consulta do andamento")
public class EncaminhamentoController {

    static final String EX = """
            {"id":3,"ocorrenciaId":7,"destinatarioId":1,"protocolo":"BA-2026-0003A1","dataEnvio":"2026-11-12T10:20:00",\
            "etapaAtual":"RECEBIMENTO","rodada":1,"concluido":false,"resultadoImpacto":null,"dataConclusao":null}""";

    @Schema(name = "EncaminharRequest")
    public record EncaminharRequest(
            @Schema(description = "Ids dos destinatários (ao menos um)", example = "[1, 2]")
            @NotEmpty List<@NotNull Long> destinatarioIds) {}

    @Schema(name = "EncaminhamentoResponse")
    public record EncaminhamentoResponse(
            @Schema(example = "3") Long id,
            @Schema(example = "7") Long ocorrenciaId,
            @Schema(example = "1") Long destinatarioId,
            @Schema(example = "BA-2026-0003A1") String protocolo,
            @Schema(example = "2026-11-12T10:20:00") LocalDateTime dataEnvio,
            @Schema(description = "Etapa em que o andamento está") Etapa etapaAtual,
            @Schema(description = "Rodada atual (sobe a cada reabertura)", example = "1") int rodada,
            @Schema(example = "false") boolean concluido,
            @Schema(description = "Resultado apurado em IMPACTO; nulo enquanto não concluído",
                    nullable = true) OpcaoResposta resultadoImpacto,
            @Schema(nullable = true, example = "null") LocalDateTime dataConclusao) {
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
    @Operation(summary = "Encaminhar ocorrência",
            description = "Cria um encaminhamento (com protocolo) para cada destinatário informado, "
                    + "iniciando na etapa RECEBIMENTO.")
    @ApiResponse(responseCode = "201", description = "Encaminhamentos criados", content = @Content(
            array = @ArraySchema(schema = @Schema(implementation = EncaminhamentoResponse.class)),
            examples = @ExampleObject(value = "[" + EX + "]")))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado")
    @ApiResponse(responseCode = "404", description = "Ocorrência ou destinatário inexistente")
    @ApiResponse(responseCode = "409", description = "Ocorrência já encaminhada a algum destinatário")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public List<EncaminhamentoResponse> encaminhar(@PathVariable Long id, @Valid @RequestBody EncaminharRequest r) {
        return service.encaminhar(id, r.destinatarioIds()).stream().map(EncaminhamentoResponse::de).toList();
    }

    @GetMapping("/ocorrencias/{id}/encaminhamentos")
    @Operation(summary = "Listar encaminhamentos da ocorrência")
    @ApiResponse(responseCode = "200", description = "Encaminhamentos da ocorrência", content = @Content(
            array = @ArraySchema(schema = @Schema(implementation = EncaminhamentoResponse.class)),
            examples = @ExampleObject(value = "[" + EX + "]")))
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Ocorrência inexistente")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public List<EncaminhamentoResponse> listar(@PathVariable Long id) {
        return service.listarDaOcorrencia(id).stream().map(EncaminhamentoResponse::de).toList();
    }

    @GetMapping("/encaminhamentos/{id}")
    @Operation(summary = "Detalhar encaminhamento")
    @ApiResponse(responseCode = "200", description = "Encaminhamento encontrado", content = @Content(
            schema = @Schema(implementation = EncaminhamentoResponse.class), examples = @ExampleObject(value = EX)))
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Encaminhamento inexistente")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public EncaminhamentoResponse detalhar(@PathVariable Long id) {
        return EncaminhamentoResponse.de(service.buscar(id));
    }
}
