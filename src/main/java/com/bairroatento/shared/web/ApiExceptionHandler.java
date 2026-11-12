package com.bairroatento.shared.web;

import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.NaoEncontradoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import com.bairroatento.shared.web.ErroResponse.CampoErro;
import tools.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Handler global: todo erro da API sai no formato {@link ErroResponse}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<ErroResponse> naoEncontrado(NaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroResponse> conflito(ConflitoException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, "Conflito de dados", req, List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroResponse> regra(RegraNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<CampoErro> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new CampoErro(f.getField(), f.getDefaultMessage())).toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", req, campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        if (e.getCause() instanceof InvalidFormatException f && !f.getPath().isEmpty()) {
            String campo = f.getPath().get(f.getPath().size() - 1).getPropertyName();
            String msg = "valor inválido: " + f.getValue();
            if (f.getTargetType() != null && f.getTargetType().isEnum()) {
                msg += " (valores aceitos: " + String.join(", ",
                        Arrays.stream(f.getTargetType().getEnumConstants()).map(Object::toString).toList()) + ")";
            }
            return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", req,
                    List.of(new CampoErro(campo, msg)));
        }
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroResponse> tipoErrado(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido", req,
                List.of(new CampoErro(e.getName(), "valor inválido: " + e.getValue())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErroResponse> parametroFaltando(MissingServletRequestParameterException e,
                                                   HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente", req,
                List.of(new CampoErro(e.getParameterName(), "obrigatório")));
    }

    /** ResponseStatusException e erros do próprio Spring (rota inexistente, método, mídia): mantêm o status. */
    @ExceptionHandler({ErrorResponseException.class, NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ErroResponse> comStatusProprio(Exception e, HttpServletRequest req) {
        ErrorResponse er = (ErrorResponse) e;
        HttpStatus status = HttpStatus.resolve(er.getStatusCode().value());
        if (status == null || status.is5xxServerError()) {
            log.error("Erro em {} {}", req.getMethod(), req.getRequestURI(), e);
            return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, List.of());
        }
        String detalhe = e instanceof org.springframework.web.server.ResponseStatusException r
                ? r.getReason() : er.getBody().getDetail();
        return resposta(status, detalhe != null ? detalhe : status.getReasonPhrase(), req, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", req, List.of());
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String msg, HttpServletRequest req,
                                                  List<CampoErro> campos) {
        var corpo = new ErroResponse(OffsetDateTime.now(), status.value(), status.getReasonPhrase(), msg,
                req.getRequestURI(), campos);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }
}
