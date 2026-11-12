package com.bairroatento.etapa.infra;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do motor de etapas.
 *
 * @param minimoPadrao mínimo de respostas usado quando o bairro não tem linha em config_regiao
 */
@Validated
@ConfigurationProperties("bairroatento.etapas")
public record EtapasProperties(@Min(1) int minimoPadrao) {}
