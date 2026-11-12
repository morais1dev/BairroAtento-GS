package com.bairroatento.shared.web;

import com.bairroatento.shared.domain.ConflitoException;
import com.bairroatento.shared.domain.NaoEncontradoException;
import com.bairroatento.shared.domain.RegraNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Tratamento mínimo de erros; será padronizado depois. */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record Erro(int status, String mensagem, List<String> detalhes) {}

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<Erro> naoEncontrado(NaoEncontradoException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<Erro> conflito(ConflitoException e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Erro> integridade(DataIntegrityViolationException e) {
        return resposta(HttpStatus.CONFLICT, "Conflito de dados", List.of());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<Erro> regra(RegraNegocioException e) {
        return resposta(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Erro> validacao(MethodArgumentNotValidException e) {
        List<String> detalhes = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhes);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Erro> corpoIlegivel(HttpMessageNotReadableException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", List.of());
    }

    private ResponseEntity<Erro> resposta(HttpStatus status, String msg, List<String> detalhes) {
        return ResponseEntity.status(status).body(new Erro(status.value(), msg, detalhes));
    }
}
