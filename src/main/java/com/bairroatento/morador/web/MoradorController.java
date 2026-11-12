package com.bairroatento.morador.web;

import com.bairroatento.morador.application.MoradorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/moradores")
public class MoradorController {

    public record MoradorRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank String cpf) {}

    public record MoradorResponse(Long id, String nome, String email, String cpf) {}

    private final MoradorService service;

    public MoradorController(MoradorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MoradorResponse cadastrar(@Valid @RequestBody MoradorRequest req) {
        var c = service.cadastrar(req.nome(), req.email(), req.cpf());
        var m = c.morador();
        return new MoradorResponse(m.getId(), m.getNome(), m.getEmail(), c.cpf().mascara());
    }
}
