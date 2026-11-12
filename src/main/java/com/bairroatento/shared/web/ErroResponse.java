package com.bairroatento.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Formato único de erro de toda a API. */
@Schema(name = "ErroResponse", description = "Formato padrão de erro da API")
public record ErroResponse(
        @Schema(description = "Instante do erro", example = "2026-11-12T10:15:30-03:00") OffsetDateTime timestamp,
        @Schema(description = "Código HTTP", example = "400") int status,
        @Schema(description = "Descrição curta do status HTTP", example = "Bad Request") String erro,
        @Schema(description = "Explicação do problema", example = "Dados inválidos") String mensagem,
        @Schema(description = "Caminho da requisição", example = "/moradores") String caminho,
        @Schema(description = "Erros por campo (vazio quando não se aplica)") List<CampoErro> campos) {

    @Schema(name = "CampoErro")
    public record CampoErro(
            @Schema(example = "email") String campo,
            @Schema(example = "deve ser um endereço de e-mail bem formado") String mensagem) {}
}
