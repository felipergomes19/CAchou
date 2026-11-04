package br.com.achou.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "achou")
public record AchouProperties(Jwt jwt, Uploads uploads, Mail mail, Recuperacao recuperacao, List<String> cursos) {

    public record Jwt(String secret, int expiracaoHoras) {}

    public record Uploads(String dir, long tamanhoMaximoBytes) {}

    public record Mail(boolean enabled, String from) {}

    public record Recuperacao(int validadeMinutos, int maxTentativas, int intervaloSegundos) {}
}
