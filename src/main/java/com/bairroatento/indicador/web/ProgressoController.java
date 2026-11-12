package com.bairroatento.indicador.web;

import com.bairroatento.indicador.application.ProgressoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Encaminhamentos")
public class ProgressoController {

    private final ProgressoService service;

    public ProgressoController(ProgressoService service) {
        this.service = service;
    }

    @GetMapping("/encaminhamentos/{id}/progresso")
    @Operation(summary = "Progresso do encaminhamento",
            description = "Etapa atual, contagem de respostas por etapa e histórico de mudanças.")
    @ApiResponse(responseCode = "200", description = "Progresso", content = @Content(
            schema = @Schema(implementation = ProgressoResponse.class),
            examples = @ExampleObject(value = """
                    {"encaminhamentoId":3,"etapaAtual":"MOBILIZACAO",\
                    "respostasPorEtapa":{"RECEBIMENTO":{"SIM":10,"NAO":0,"NAO_SEI":1},\
                    "MOBILIZACAO":{"SIM":2,"NAO":0,"NAO_SEI":0},"EXECUCAO":{"SIM":0,"NAO":0,"NAO_SEI":0},\
                    "IMPACTO":{"AJUDOU":0,"AJUDOU_PARCIALMENTE":0,"NAO_AJUDOU":0}},\
                    "historico":[{"etapaAnterior":null,"etapaNova":"RECEBIMENTO","data":"2026-11-12T10:20:00"},\
                    {"etapaAnterior":"RECEBIMENTO","etapaNova":"MOBILIZACAO","data":"2026-11-12T11:00:00"}]}""")))
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Encaminhamento inexistente")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public ProgressoResponse progresso(@PathVariable Long id) {
        return ProgressoResponse.de(service.progresso(id));
    }
}
