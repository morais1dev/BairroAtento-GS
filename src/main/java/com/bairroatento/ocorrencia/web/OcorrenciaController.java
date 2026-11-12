package com.bairroatento.ocorrencia.web;

import com.bairroatento.ocorrencia.application.OcorrenciaService;
import com.bairroatento.ocorrencia.domain.Ocorrencia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ocorrencias")
@Tag(name = "Ocorrências", description = "Registro, consulta e confirmação de ocorrências do bairro")
public class OcorrenciaController {

    static final String EX = """
            {"id":7,"categoria":"buraco","descricao":"Buraco grande na pista","endereco":"Rua das Flores, 120",\
            "bairro":"Centro","latitude":-23.55,"longitude":-46.63,"status":"ABERTA","autorId":1,\
            "dataCriacao":"2026-11-12T10:15:30","contadorConfirmacoes":0}""";

    @Schema(name = "OcorrenciaRequest")
    public record OcorrenciaRequest(
            @Schema(description = "Nome de uma categoria existente", example = "buraco") @NotBlank String categoria,
            @Schema(example = "Buraco grande na pista") @NotBlank String descricao,
            @Schema(example = "Rua das Flores, 120") @NotBlank String endereco,
            @Schema(example = "Centro") @NotBlank String bairro,
            @Schema(description = "Entre -90 e 90", example = "-23.55") Double latitude,
            @Schema(description = "Entre -180 e 180", example = "-46.63") Double longitude,
            @Schema(description = "Id do morador autor", example = "1") @NotNull Long autorId) {}

    @Schema(name = "ConfirmacaoRequest")
    public record ConfirmacaoRequest(
            @Schema(description = "Id do morador que confirma", example = "2") @NotNull Long moradorId) {}

    @Schema(name = "OcorrenciaResponse")
    public record OcorrenciaResponse(
            @Schema(example = "7") Long id,
            @Schema(example = "buraco") String categoria,
            @Schema(example = "Buraco grande na pista") String descricao,
            @Schema(example = "Rua das Flores, 120") String endereco,
            @Schema(example = "Centro") String bairro,
            @Schema(example = "-23.55") Double latitude,
            @Schema(example = "-46.63") Double longitude,
            @Schema(example = "ABERTA") String status,
            @Schema(example = "1") Long autorId,
            @Schema(example = "2026-11-12T10:15:30") LocalDateTime dataCriacao,
            @Schema(example = "0") int contadorConfirmacoes) {
        static OcorrenciaResponse de(Ocorrencia o) {
            return new OcorrenciaResponse(o.getId(), o.getCategoria(), o.getDescricao(), o.getEndereco(),
                    o.getBairro(), o.getLatitude(), o.getLongitude(), o.getStatus(), o.getAutorId(),
                    o.getDataCriacao(), o.getContadorConfirmacoes());
        }
    }

    @Schema(name = "PaginaOcorrenciasResponse")
    public record PaginaResponse(List<OcorrenciaResponse> conteudo,
                                 @Schema(example = "0") int pagina,
                                 @Schema(example = "20") int tamanho,
                                 @Schema(example = "1") long totalElementos,
                                 @Schema(example = "1") int totalPaginas) {}

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registrar ocorrência",
            description = "Registra uma ocorrência com status ABERTA. Responde com o cabeçalho Location.")
    @ApiResponse(responseCode = "201", description = "Ocorrência criada", content = @Content(
            schema = @Schema(implementation = OcorrenciaResponse.class), examples = @ExampleObject(value = EX)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado")
    @ApiResponse(responseCode = "422", description = "Categoria ou autor inexistente, ou coordenadas fora do intervalo")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public ResponseEntity<OcorrenciaResponse> criar(@Valid @RequestBody OcorrenciaRequest r) {
        Ocorrencia o = service.registrar(r.categoria(), r.descricao(), r.endereco(), r.bairro(),
                r.latitude(), r.longitude(), r.autorId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/ocorrencias/" + o.getId())
                .body(OcorrenciaResponse.de(o));
    }

    @GetMapping
    @Operation(summary = "Listar ocorrências",
            description = "Lista paginada (mais recentes primeiro), com filtros opcionais.")
    @ApiResponse(responseCode = "200", description = "Página de ocorrências")
    @ApiResponse(responseCode = "400", description = "Parâmetro inválido (ex.: pagina não numérica)")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public PaginaResponse listar(@Parameter(description = "Filtra pelo bairro", example = "Centro")
                                 @RequestParam(required = false) String bairro,
                                 @Parameter(description = "Filtra pela categoria", example = "buraco")
                                 @RequestParam(required = false) String categoria,
                                 @Parameter(description = "Filtra pelo status", example = "ABERTA")
                                 @RequestParam(required = false) String status,
                                 @Parameter(description = "Número da página, a partir de 0", example = "0")
                                 @RequestParam(defaultValue = "0") int pagina) {
        Page<Ocorrencia> p = service.listar(bairro, categoria, status, pagina);
        return new PaginaResponse(p.getContent().stream().map(OcorrenciaResponse::de).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar ocorrência")
    @ApiResponse(responseCode = "200", description = "Ocorrência encontrada", content = @Content(
            schema = @Schema(implementation = OcorrenciaResponse.class), examples = @ExampleObject(value = EX)))
    @ApiResponse(responseCode = "400", description = "Id inválido")
    @ApiResponse(responseCode = "404", description = "Ocorrência inexistente")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public OcorrenciaResponse detalhe(@PathVariable Long id) {
        return OcorrenciaResponse.de(service.buscar(id));
    }

    @PostMapping("/{id}/confirmacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirmar ocorrência",
            description = "Um morador confirma que a ocorrência existe; cada morador confirma uma única vez.")
    @ApiResponse(responseCode = "201", description = "Confirmação registrada; devolve a ocorrência atualizada",
            content = @Content(schema = @Schema(implementation = OcorrenciaResponse.class),
                    examples = @ExampleObject(value = EX)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado")
    @ApiResponse(responseCode = "404", description = "Ocorrência ou morador inexistente")
    @ApiResponse(responseCode = "409", description = "Morador já confirmou esta ocorrência")
    @ApiResponse(responseCode = "500", description = "Erro interno")
    public OcorrenciaResponse confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmacaoRequest r) {
        return OcorrenciaResponse.de(service.confirmar(id, r.moradorId()));
    }
}
