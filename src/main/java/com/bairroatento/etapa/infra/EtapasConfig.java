package com.bairroatento.etapa.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EtapasProperties.class)
public class EtapasConfig {}
