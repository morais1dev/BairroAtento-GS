# Bairro Atento

CitizenTech da disciplina de Global Solution (FIAP, 2º semestre de 2026). Entrega de **Arquitetura Orientada a Serviços (SOA) e Web Services**: uma API REST em Java com Spring Boot que permite aos moradores relatar problemas do bairro e acompanhar, de forma coletiva, se o relato chegou ao órgão responsável e se foi resolvido.

> **Aviso:** este é um projeto imaginário. Todos os dados (moradores, bairros, cidade, órgãos, protocolos) são **fictícios** e não há integração com órgãos públicos. O CPF `969.430.400-81` foi **gerado para testes** e não pertence a nenhuma pessoa real; os demais CPFs usados nos testes também são gerados.

## Ficha do Bairro Atento

> As "seis perguntas do briefing" ainda não foram repassadas ao time. Abaixo estão as respostas que o contexto do projeto permite; o que falta está marcado com TODO.

**Problema.** Os problemas do bairro (buracos, lixo acumulado, iluminação falha, calçadas danificadas) são relatados de forma dispersa, e ninguém acompanha se o relato chegou aos órgãos responsáveis nem se o problema foi resolvido.

**Cidadão beneficiado.** O morador de bairro periférico que convive com o problema e não tem como acompanhar o que acontece com o que relatou.

**Persona (fictícia): Dona Lúcia.** Tem 58 anos, pega dois ônibus para trabalhar e desvia do mesmo buraco na rua todos os dias há um ano. Já reclamou para vizinhos e em grupos de mensagens, mas nunca soube se alguém no poder público ficou sabendo. Com o Bairro Atento ela registra o buraco, vê os vizinhos confirmando e acompanha o andamento até saber se o conserto ajudou. Cenário fictício: bairro Vila Esperança, em uma cidade fictícia de porte médio.

**Proposta.** Moradores registram ocorrências com categoria e local. Outros moradores confirmam que também veem o problema. A ocorrência é encaminhada a um ou mais destinatários (órgãos ou ONGs fictícios) e cada encaminhamento passa por quatro etapas, **confirmadas pela própria comunidade**:

1. **Recebimento:** o relato chegou ao órgão?
2. **Mobilização:** há alguma movimentação (vistoria, contato, protocolo)?
3. **Execução:** o trabalho foi realizado?
4. **Impacto:** o trabalho ajudou? (ajudou, ajudou parcialmente, não ajudou)

Cada resposta pode ser "sim", "não" ou "não sei"; "não sei" não conta como confirmação nem como contestação. Um indicador de impacto por bairro mostra quanto do que foi encaminhado de fato ajudou.

**ODS 11: Cidades e comunidades sustentáveis.** O projeto atua na meta de tornar as cidades mais inclusivas e resilientes: transforma relatos soltos sobre infraestrutura urbana básica (vias, iluminação, limpeza, calçadas) em um fluxo rastreável, dando a bairros periféricos uma forma organizada de cobrar e medir a melhoria dos serviços urbanos.

**ODS 16: Paz, justiça e instituições eficazes.** O projeto atua na transparência e na prestação de contas das instituições e na participação da população nas decisões: cada encaminhamento tem protocolo, etapas verificáveis pela comunidade e histórico cronológico, e o indicador de impacto expõe se as instituições estão respondendo.

**Perguntas do briefing que faltam:**

- TODO: pergunta 1 do briefing e resposta.
- TODO: pergunta 2 do briefing e resposta.
- TODO: pergunta 3 do briefing e resposta.
- TODO: pergunta 4 do briefing e resposta.
- TODO: pergunta 5 do briefing e resposta.
- TODO: pergunta 6 do briefing e resposta.

## Arquitetura (SOA)

Monólito modular em camadas, organizado em serviços de negócio com contratos REST, pacote base `com.bairroatento`:

| Pacote | Serviço | Responsabilidade |
|---|---|---|
| `morador` | Moradores | Cadastro; validação de CPF no domínio; CPF guardado só como hash e exibido mascarado |
| `ocorrencia` | Ocorrências | Registro, listagem, detalhe e confirmação de ocorrências; categorias |
| `encaminhamento` | Encaminhamentos | Envio a destinatários, com protocolo próprio por encaminhamento |
| `etapa` | Etapas | Respostas dos moradores, motor de etapas (confirmação, reabertura, rodadas) e histórico |
| `indicador` | Progresso e indicador | Progresso por encaminhamento e indicador de impacto por bairro |
| `shared` | Transversal | Exceções de domínio, handler global de erros, configuração do OpenAPI |

Cada pacote de domínio tem quatro camadas:

- `domain`: entidades e regras puras (testáveis sem banco);
- `application`: serviços que orquestram os casos de uso;
- `web`: controllers e DTOs (nenhuma entidade é exposta diretamente);
- `infra`: repositórios e configuração.

Stack: Java 21, Spring Boot 4.1, Spring Web, Spring Data JPA, Bean Validation, Flyway (migrações V1 a V6), H2, springdoc-openapi (Swagger) e JUnit 5 com MockMvc.

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/moradores` | Cadastra morador (nome, e-mail único, CPF) |
| POST | `/ocorrencias` | Registra ocorrência (categoria e local obrigatórios) |
| GET | `/ocorrencias?bairro=&categoria=&status=&pagina=` | Lista ocorrências com filtros e paginação |
| GET | `/ocorrencias/{id}` | Detalhe da ocorrência |
| POST | `/ocorrencias/{id}/confirmacoes` | Morador confirma a ocorrência (uma vez) |
| POST | `/ocorrencias/{id}/encaminhamentos` | Encaminha a um ou mais destinatários |
| GET | `/ocorrencias/{id}/encaminhamentos` | *(consulta extra)* Lista os encaminhamentos da ocorrência |
| GET | `/encaminhamentos/{id}` | *(consulta extra)* Detalhe de um encaminhamento |
| POST | `/encaminhamentos/{id}/etapas/{etapa}/respostas` | Morador responde uma etapa (`RECEBIMENTO`, `MOBILIZACAO`, `EXECUCAO`, `IMPACTO`) |
| GET | `/encaminhamentos/{id}/historico` | *(consulta extra)* Histórico de mudanças de etapa |
| GET | `/encaminhamentos/{id}/progresso` | Etapa atual, respostas por etapa (rodada atual) e histórico cronológico |
| GET | `/indicadores/impacto?bairro=&periodo=` | Indicador de impacto (`periodo`: `AAAA-MM` ou `AAAA-MM-DD/AAAA-MM-DD`) |

Os destinatários fictícios do seed são: 1 Saúde pública, 2 Mobilidade e transporte, 3 Vigilância sanitária, 4 Outros. As categorias são: buraco, iluminação, lixo, calçada e outro. O status inicial de uma ocorrência é `ABERTA`.

### Formato de erro

Todo erro da API (400, 404, 405, 409, 415, 422, 500) tem o mesmo corpo JSON, sem stack trace nem detalhes internos:

```json
{
  "timestamp": "2026-11-12T10:15:30-03:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "caminho": "/moradores",
  "campos": [{ "campo": "email", "mensagem": "deve ser um endereço de e-mail bem formado" }]
}
```

| Código | Quando |
|---|---|
| 400 | Validação, JSON malformado, tipo ou valor inválido |
| 404 | Ocorrência, encaminhamento, morador ou rota inexistente |
| 409 | Duplicidade (e-mail, confirmação, resposta) ou transição inválida (etapa fora de ordem) |
| 415 | Tipo de mídia não suportado (a API aceita `application/json`) |
| 422 | Regra de negócio violada (por exemplo, morador sem vínculo com a ocorrência, impacto antes da execução, período inválido) |
| 500 | Erro interno (mensagem genérica; o detalhe só vai para o log) |

## Como rodar

Pré-requisitos: **JDK 21** ou superior e Maven 3.9+. Se o `java` padrão da máquina for mais antigo, aponte o `JAVA_HOME` para o JDK 21:

```bash
# Linux/macOS/Git Bash
export JAVA_HOME=/caminho/para/jdk-21
# PowerShell
$env:JAVA_HOME = "C:\caminho\para\jdk-21"
```

Comandos:

```bash
mvn verify             # compila e roda todos os testes (H2 em memória, perfil test)
mvn spring-boot:run    # sobe a API em http://localhost:8080 (perfil dev, H2 em arquivo)
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html (OpenAPI em `/v3/api-docs`).
- **Console do H2** (perfil `dev`): http://localhost:8080/h2-console, com JDBC URL `jdbc:h2:file:./data/bairroatento-dev`, usuário `sa` e senha vazia.
- As migrações Flyway rodam sozinhas na subida, em banco vazio. Nunca edite uma migração já aplicada: mudanças entram em uma nova versão.
- Os testes cobrem o domínio (CPF, ocorrência, motor de etapas, indicador), a API de cada serviço, o formato de erro e o fluxo completo ponta a ponta (`FluxoCompletoTest`).

## Regras de negócio (resumo)

- **Morador:** nome, e-mail único e CPF fictício. O CPF tem formato e dígitos verificadores validados no domínio, é guardado apenas como hash e aparece nas respostas mascarado (`***.***.***-81`).
- **Ocorrência:** categoria e local obrigatórios; nasce `ABERTA`. Cada morador confirma a mesma ocorrência uma única vez (duplicidade retorna 409) e cada confirmação aumenta o contador.
- **Encaminhamento:** exige ao menos um destinatário existente; cada destinatário gera um encaminhamento separado, com protocolo fictício próprio e andamento de etapas independente.
- **Etapas:** respondidas em ordem, sem pular. Só quem registrou ou confirmou a ocorrência responde, uma vez por etapa e rodada. Uma etapa é confirmada com pelo menos **N respostas "sim"** de moradores distintos e mais "sim" que "não". **N é configurável por região**: o padrão é 10 (`bairroatento.etapas.minimo-padrao`) e pode ser sobrescrito por bairro na tabela `config_regiao`.
- **Reabertura:** com pelo menos N respostas "não" e mais "não" que "sim", a etapa é reaberta e o andamento volta para a etapa anterior, com uma **nova rodada** (as respostas antigas ficam no histórico, a contagem recomeça e os moradores podem responder de novo). "Não recebi" é o "não" da etapa de recebimento: como é a primeira etapa, o encaminhamento permanece em recebimento, em rodada nova.
- **Impacto:** só é aceito depois da execução confirmada. A etapa fecha com N respostas e vale a opção mais votada (ajudou, ajudou parcialmente ou não ajudou). "Não ajudou" não reabre nenhuma etapa.
- **Progresso:** mostra a etapa atual, as respostas por etapa (da rodada atual) e o histórico de mudanças em ordem cronológica. Ocorrência ou encaminhamento inexistente retorna 404.
- **Indicador de impacto:** percentual de "ajudou" entre os encaminhamentos com impacto apurado; "ajudou parcialmente" aparece separado e não entra no percentual positivo; tempo médio entre o envio e a execução confirmada. Bairro sem encaminhamentos retorna o indicador zerado, sem erro.

## Dados fictícios

Nenhum dado real é usado ou armazenado. O CPF `969.430.400-81` (presente no seed e nos testes) foi gerado para testes; os outros CPFs dos testes são gerados por algoritmo. E-mails usam o domínio reservado `example.com`.
