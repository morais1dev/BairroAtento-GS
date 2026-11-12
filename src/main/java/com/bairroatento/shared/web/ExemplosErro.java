package com.bairroatento.shared.web;

/** Exemplos de corpo de erro reutilizados nas anotações do OpenAPI. */
public final class ExemplosErro {

    private ExemplosErro() {}

    public static final String E400 = """
            {"timestamp":"2026-11-12T10:15:30-03:00","status":400,"erro":"Bad Request","mensagem":"Dados inválidos",\
            "caminho":"/moradores","campos":[{"campo":"email","mensagem":"deve ser um endereço de e-mail bem formado"}]}""";

    public static final String E404 = """
            {"timestamp":"2026-11-12T10:15:30-03:00","status":404,"erro":"Not Found","mensagem":"Ocorrência 99 não encontrada",\
            "caminho":"/ocorrencias/99","campos":[]}""";

    public static final String E409 = """
            {"timestamp":"2026-11-12T10:15:30-03:00","status":409,"erro":"Conflict","mensagem":"E-mail já cadastrado",\
            "caminho":"/moradores","campos":[]}""";

    public static final String E422 = """
            {"timestamp":"2026-11-12T10:15:30-03:00","status":422,"erro":"Unprocessable Content","mensagem":"Categoria inexistente: Xyz",\
            "caminho":"/ocorrencias","campos":[]}""";

    public static final String E500 = """
            {"timestamp":"2026-11-12T10:15:30-03:00","status":500,"erro":"Internal Server Error","mensagem":"Erro interno do servidor",\
            "caminho":"/ocorrencias","campos":[]}""";
}
