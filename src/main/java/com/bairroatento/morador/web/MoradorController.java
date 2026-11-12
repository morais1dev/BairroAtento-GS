package com.bairroatento.morador.web;

import com.bairroatento.morador.application.MoradorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moradores")
@Tag(name = "Moradores", description = "Cadastro de moradores")
public class MoradorController {

    @Schema(name = "MoradorRequest")
    public record MoradorRequest(
            @Schema(example = "Maria Souza") @NotBlank @Size(max = 150) String nome,
            @Schema(example = "maria.souza@example.com") @NotBlank @Email @Size(max = 255) String email,
            @Schema(description = "CPF com ou sem pontuação", example = "969.430.400-81") @NotBlank String cpf) {}

    @Schema(name = "MoradorResponse")
    public record MoradorResponse(
            @Schema(example = "1") Long id,
            @Schema(example = "Maria Souza") String nome,
            @Schema(example = "maria.souza@example.com") String email,
            @Schema(description = "CPF mascarado (só os dois últimos dígitos aparecem)", example = "***.***.***-81") String cpf) {}

    private final MoradorService service;

    public MoradorController(MoradorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar morador",
            description = "Cria um morador. O CPF é validado e devolvido mascarado; e-mail e CPF não podem repetir.")
    @ApiResponse(responseCode = "201", description = "Morador criado", content = @Content(
            schema = @Schema(implementation = MoradorResponse.class),
            examples = @ExampleObject(value = "{\"id\":1,\"nome\":\"Maria Souza\",\"email\":\"maria.souza@example.com\",\"cpf\":\"***.***.***-81\"}")))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado")
    @ApiResponse(responseCode = "409", description = "E-mail ou CPF já cadastrado")
    @ApiResponse(responseCode = "422", description = "CPF inválido")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public MoradorResponse cadastrar(@Valid @RequestBody MoradorRequest req) {
        var c = service.cadastrar(req.nome(), req.email(), req.cpf());
        var m = c.morador();
        return new MoradorResponse(m.getId(), m.getNome(), m.getEmail(), c.cpf().mascara());
    }
}
