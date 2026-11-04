package br.com.achou.desktop.api;

/** Erro da API (ou de conexão, quando status = 0) já com mensagem amigável para exibir ao usuário. */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
