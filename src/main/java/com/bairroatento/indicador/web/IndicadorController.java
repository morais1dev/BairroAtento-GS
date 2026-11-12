package com.bairroatento.indicador.web;

import com.bairroatento.indicador.application.IndicadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores")
@Tag(name = "Indicadores", description = "Indicadores de impacto dos encaminhamentos")
public class IndicadorController {

    private final IndicadorService service;

    public IndicadorController(IndicadorService service) {
        this.service = service;
    }

    @GetMapping("/impacto")
    @Operation(summary = "Indicador de impacto",
            description = "Resultados apurados na etapa IMPACTO e tempo médio até a execução, "
                    + "com filtros opcionais por bairro e período (sobre a data de envio).")
    @ApiResponse(responseCode = "200", description = "Indicador calculado", content = @Content(
            schema = @Schema(implementation = IndicadorImpactoResponse.class),
            examples = @ExampleObject(value = """
                    {"totalEncaminhamentos":12,"impactosFechados":5,"ajudou":3,"ajudouParcialmente":1,\
                    "naoAjudou":1,"percentualAjudou":60.0,"percentualAjudouParcialmente":20.0,\
                    "execucoesConfirmadas":7,"tempoMedioExecucaoSegundos":86400}""")))
    @ApiResponse(responseCode = "422", description = "Período inválido (formato ou fim anterior ao início)")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public IndicadorImpactoResponse impacto(
            @Parameter(description = "Filtra pelo bairro da ocorrência", example = "Centro")
            @RequestParam(required = false) String bairro,
            @Parameter(description = "AAAA-MM ou AAAA-MM-DD/AAAA-MM-DD (intervalo inclusivo)",
                    example = "2026-11") @RequestParam(required = false) String periodo) {
        return IndicadorImpactoResponse.de(service.impacto(bairro, periodo));
    }
}
