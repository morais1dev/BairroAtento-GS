package com.bairroatento.indicador.web;

import com.bairroatento.indicador.application.IndicadorService;
import com.bairroatento.indicador.domain.IndicadorImpacto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indicadores")
public class IndicadorController {

    private final IndicadorService service;

    public IndicadorController(IndicadorService service) {
        this.service = service;
    }

    /** periodo: AAAA-MM ou AAAA-MM-DD/AAAA-MM-DD (sobre data_envio). */
    @GetMapping("/impacto")
    public IndicadorImpacto impacto(@RequestParam(required = false) String bairro,
                                    @RequestParam(required = false) String periodo) {
        return service.impacto(bairro, periodo);
    }
}
