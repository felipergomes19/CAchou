package br.com.achou.api.web;

import br.com.achou.api.dto.Dtos.ErroDto;
import br.com.achou.api.service.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErroDto> negocio(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErroDto(e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroDto> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String primeira = campos.values().stream().findFirst().orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(new ErroDto(primeira, campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErroDto> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest().body(new ErroDto("Requisição inválida.", null));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroDto> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroDto("Erro interno. Tente novamente em instantes.", null));
    }
}
