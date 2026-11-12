package com.bairroatento.ocorrencia.web;

import com.bairroatento.ocorrencia.application.OcorrenciaService;
import com.bairroatento.ocorrencia.domain.Ocorrencia;
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
public class OcorrenciaController {

    public record OcorrenciaRequest(
            @NotBlank String categoria,
            @NotBlank String descricao,
            @NotBlank String endereco,
            @NotBlank String bairro,
            Double latitude,
            Double longitude,
            @NotNull Long autorId) {}

    public record ConfirmacaoRequest(@NotNull Long moradorId) {}

    public record OcorrenciaResponse(Long id, String categoria, String descricao, String endereco,
                                     String bairro, Double latitude, Double longitude, String status,
                                     Long autorId, LocalDateTime dataCriacao, int contadorConfirmacoes) {
        static OcorrenciaResponse de(Ocorrencia o) {
            return new OcorrenciaResponse(o.getId(), o.getCategoria(), o.getDescricao(), o.getEndereco(),
                    o.getBairro(), o.getLatitude(), o.getLongitude(), o.getStatus(), o.getAutorId(),
                    o.getDataCriacao(), o.getContadorConfirmacoes());
        }
    }

    public record PaginaResponse(List<OcorrenciaResponse> conteudo, int pagina, int tamanho,
                                 long totalElementos, int totalPaginas) {}

    private final OcorrenciaService service;

    public OcorrenciaController(OcorrenciaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OcorrenciaResponse> criar(@Valid @RequestBody OcorrenciaRequest r) {
        Ocorrencia o = service.registrar(r.categoria(), r.descricao(), r.endereco(), r.bairro(),
                r.latitude(), r.longitude(), r.autorId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/ocorrencias/" + o.getId())
                .body(OcorrenciaResponse.de(o));
    }

    @GetMapping
    public PaginaResponse listar(@RequestParam(required = false) String bairro,
                                 @RequestParam(required = false) String categoria,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int pagina) {
        Page<Ocorrencia> p = service.listar(bairro, categoria, status, pagina);
        return new PaginaResponse(p.getContent().stream().map(OcorrenciaResponse::de).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @GetMapping("/{id}")
    public OcorrenciaResponse detalhe(@PathVariable Long id) {
        return OcorrenciaResponse.de(service.buscar(id));
    }

    @PostMapping("/{id}/confirmacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public OcorrenciaResponse confirmar(@PathVariable Long id, @Valid @RequestBody ConfirmacaoRequest r) {
        return OcorrenciaResponse.de(service.confirmar(id, r.moradorId()));
    }
}
