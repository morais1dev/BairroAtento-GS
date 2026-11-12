package com.bairroatento.shared.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.examples.Example;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI bairroAtentoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Bairro Atento API")
                .version("1.0.0")
                .description("""
                        API do **Bairro Atento**: moradores registram e confirmam ocorrências do bairro, \
                        encaminham-nas a destinatários (prefeitura, concessionárias etc.) e acompanham o \
                        andamento em quatro etapas (RECEBIMENTO, MOBILIZACAO, EXECUCAO, IMPACTO), \
                        com indicadores de impacto.

                        Projeto acadêmico fictício, desenvolvido para a Global Solution 2026 da FIAP. \
                        Não representa nenhum serviço real e os dados de exemplo são inventados.

                        **Erros:** todos seguem o mesmo formato (`ErroResponse`): 400 validação/parse, \
                        404 inexistente, 409 duplicidade ou transição inválida, 422 regra de negócio, \
                        500 erro interno.""")
                .contact(new Contact().name("Bairro Atento - FIAP GS 2026"))
                .license(new License().name("Uso acadêmico")));
    }

    /** Registra ErroResponse e preenche o corpo (schema + exemplo) das respostas 4xx/5xx declaradas só com descrição. */
    @Bean
    OpenApiCustomizer respostasDeErro() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ErroResponse.class)
                    .forEach((nome, schema) -> openApi.getComponents().addSchemas(nome, schema));
            openApi.getPaths().values().forEach(item -> item.readOperations().forEach(op ->
                    op.getResponses().forEach((codigo, resposta) -> {
                        String exemplo = switch (codigo) {
                            case "400" -> ExemplosErro.E400;
                            case "404" -> ExemplosErro.E404;
                            case "409" -> ExemplosErro.E409;
                            case "422" -> ExemplosErro.E422;
                            case "500" -> ExemplosErro.E500;
                            default -> null;
                        };
                        if (exemplo == null) return;
                        var midia = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErroResponse"));
                        midia.addExamples("erro", new Example().value(parse(exemplo)));
                        resposta.setContent(new Content().addMediaType("application/json", midia));
                    })));
        };
    }

    private static Object parse(String json) {
        try {
            return new tools.jackson.databind.ObjectMapper().readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }
}
