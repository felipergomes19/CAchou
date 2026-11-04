package br.com.achou.api.service;

import org.springframework.http.HttpStatus;

/** Erro de negócio com status HTTP e mensagem amigável (em português) para o usuário. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException invalido(String m) { return new ApiException(HttpStatus.BAD_REQUEST, m); }
    public static ApiException naoAutenticado(String m) { return new ApiException(HttpStatus.UNAUTHORIZED, m); }
    public static ApiException proibido(String m) { return new ApiException(HttpStatus.FORBIDDEN, m); }
    public static ApiException naoEncontrado(String m) { return new ApiException(HttpStatus.NOT_FOUND, m); }
    public static ApiException conflito(String m) { return new ApiException(HttpStatus.CONFLICT, m); }
    public static ApiException muitasTentativas(String m) { return new ApiException(HttpStatus.TOO_MANY_REQUESTS, m); }
}
