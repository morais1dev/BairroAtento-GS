package com.bairroatento.indicador.web;

import com.bairroatento.indicador.application.ProgressoService;
import com.bairroatento.indicador.domain.Progresso;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressoController {

    private final ProgressoService service;

    public ProgressoController(ProgressoService service) {
        this.service = service;
    }

    @GetMapping("/encaminhamentos/{id}/progresso")
    public Progresso progresso(@PathVariable Long id) {
        return service.progresso(id);
    }
}
