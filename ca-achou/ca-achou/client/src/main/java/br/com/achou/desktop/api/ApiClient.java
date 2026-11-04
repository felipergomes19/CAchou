package br.com.achou.desktop.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP do backend. Todos os métodos são BLOQUEANTES: chame-os sempre fora da thread do JavaFX
 * (use {@code Async.run}). Erros viram {@link ApiException} com mensagem em português.
 */
public final class ApiClient {

    /** Filtros da listagem; valores nulos/vazios são ignorados. */
    public record Filtro(String tipo, String categoria, String subcategoria, String busca,
                         boolean somenteAbertos, boolean minhas) {}

    @FunctionalInterface
    private interface Leitor<T> {
        T ler(byte[] corpo) throws IOException;
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    private final ObjectMapper om = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String base;
    private volatile String token;

    public ApiClient(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public String base() {
        return base;
    }

    public void setToken(String token) {
        this.token = token;
    }

    // ================= Autenticação =================

    public Dtos.AuthResponse login(String rm, String senha) {
        return chamar("POST", "/api/auth/login", Map.of("rm", rm, "senha", senha), Dtos.AuthResponse.class);
    }

    public Dtos.AuthResponse cadastrar(Dtos.Cadastro c) {
        return chamar("POST", "/api/auth/cadastro", c, Dtos.AuthResponse.class);
    }

    public Dtos.Mensagem esqueciSenha(String rm) {
        return chamar("POST", "/api/auth/esqueci-senha", Map.of("rm", rm), Dtos.Mensagem.class);
    }

    public Dtos.Mensagem redefinirSenha(String rm, String codigo, String novaSenha) {
        return chamar("POST", "/api/auth/redefinir-senha",
                Map.of("rm", rm, "codigo", codigo, "novaSenha", novaSenha), Dtos.Mensagem.class);
    }

    public List<String> cursos() {
        return chamar("GET", "/api/cursos", null, new TypeReference<List<String>>() {});
    }

    public List<Dtos.Categoria> categorias() {
        return chamar("GET", "/api/categorias", null, new TypeReference<List<Dtos.Categoria>>() {});
    }

    // ================= Perfil =================

    public Dtos.Usuario me() {
        return chamar("GET", "/api/me", null, Dtos.Usuario.class);
    }

    public Dtos.Usuario atualizarPerfil(Dtos.AtualizarPerfil p) {
        return chamar("PUT", "/api/me", p, Dtos.Usuario.class);
    }

    public Dtos.Mensagem alterarSenha(String senhaAtual, String novaSenha) {
        return chamar("PUT", "/api/me/senha", Map.of("senhaAtual", senhaAtual, "novaSenha", novaSenha),
                Dtos.Mensagem.class);
    }

    public Dtos.Usuario definirFotoPerfil(byte[] imagem) {
        return enviarBytes("PUT", "/api/me/foto", imagem, Dtos.Usuario.class);
    }

    public Dtos.Usuario removerFotoPerfil() {
        return chamar("DELETE", "/api/me/foto", null, Dtos.Usuario.class);
    }

    public Dtos.Estatisticas estatisticas() {
        return chamar("GET", "/api/me/estatisticas", null, Dtos.Estatisticas.class);
    }

    // ================= Publicações =================

    public Dtos.Pagina<Dtos.Publicacao> listar(Filtro f, int pagina, int tamanho) {
        StringBuilder q = new StringBuilder("/api/publicacoes?pagina=").append(pagina).append("&tamanho=").append(tamanho);
        param(q, "tipo", f.tipo());
        param(q, "categoria", f.categoria());
        param(q, "subcategoria", f.subcategoria());
        param(q, "q", f.busca());
        if (f.somenteAbertos()) q.append("&somenteAbertos=true");
        if (f.minhas()) q.append("&minhas=true");
        return chamar("GET", q.toString(), null, new TypeReference<Dtos.Pagina<Dtos.Publicacao>>() {});
    }

    public Dtos.Publicacao criar(Dtos.NovaPublicacao p) {
        return chamar("POST", "/api/publicacoes", p, Dtos.Publicacao.class);
    }

    public Dtos.Publicacao enviarFoto(long publicacaoId, byte[] imagem) {
        return enviarBytes("PUT", "/api/publicacoes/" + publicacaoId + "/foto", imagem, Dtos.Publicacao.class);
    }

    public Dtos.Publicacao marcarDevolvido(long id) {
        return chamar("PATCH", "/api/publicacoes/" + id + "/devolvido", null, Dtos.Publicacao.class);
    }

    public void excluir(long id) {
        enviar(jsonReq("DELETE", "/api/publicacoes/" + id, null), null);
    }

    public List<Dtos.Comentario> comentarios(long publicacaoId) {
        return chamar("GET", "/api/publicacoes/" + publicacaoId + "/comentarios", null,
                new TypeReference<List<Dtos.Comentario>>() {});
    }

    public Dtos.Comentario comentar(long publicacaoId, String texto) {
        return chamar("POST", "/api/publicacoes/" + publicacaoId + "/comentarios", Map.of("texto", texto),
                Dtos.Comentario.class);
    }

    /** Baixa uma foto (caminho relativo devolvido pela API, ex.: /api/arquivos/xxxx.jpg). */
    public byte[] baixar(String caminho) {
        byte[] dados = enviar(req(caminho).GET().build(), b -> b);
        if (dados == null) throw new ApiException(404, "Imagem vazia.");
        return dados;
    }

    // ================= Infraestrutura =================

    private static void param(StringBuilder q, String nome, String valor) {
        if (valor != null && !valor.isBlank()) {
            q.append('&').append(nome).append('=').append(URLEncoder.encode(valor.trim(), StandardCharsets.UTF_8));
        }
    }

    private HttpRequest.Builder req(String caminho) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + caminho))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");
        String t = token;
        if (t != null) b.header("Authorization", "Bearer " + t);
        return b;
    }

    private HttpRequest jsonReq(String metodo, String caminho, Object corpo) {
        try {
            HttpRequest.BodyPublisher pub = corpo == null
                    ? BodyPublishers.noBody()
                    : BodyPublishers.ofByteArray(om.writeValueAsBytes(corpo));
            return req(caminho).header("Content-Type", "application/json").method(metodo, pub).build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao montar requisição", e);
        }
    }

    private <T> T chamar(String metodo, String caminho, Object corpo, Class<T> tipo) {
        return enviar(jsonReq(metodo, caminho, corpo), b -> om.readValue(b, tipo));
    }

    private <T> T chamar(String metodo, String caminho, Object corpo, TypeReference<T> tipo) {
        return enviar(jsonReq(metodo, caminho, corpo), b -> om.readValue(b, tipo));
    }

    private <T> T enviarBytes(String metodo, String caminho, byte[] dados, Class<T> tipo) {
        HttpRequest r = req(caminho).header("Content-Type", "application/octet-stream")
                .method(metodo, BodyPublishers.ofByteArray(dados)).build();
        return enviar(r, b -> om.readValue(b, tipo));
    }

    private <T> T enviar(HttpRequest requisicao, Leitor<T> leitor) {
        try {
            HttpResponse<byte[]> res = http.send(requisicao, HttpResponse.BodyHandlers.ofByteArray());
            int status = res.statusCode();
            if (status / 100 == 2) {
                byte[] corpo = res.body();
                return (leitor == null || corpo == null || corpo.length == 0) ? null : leitor.ler(corpo);
            }
            throw erro(status, res.body());
        } catch (JsonProcessingException e) {
            throw new ApiException(0, "Resposta inesperada do servidor.");
        } catch (IOException e) {
            throw new ApiException(0, "Não foi possível conectar ao servidor (" + base
                    + "). Verifique se o backend está em execução.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Operação interrompida.");
        }
    }

    private ApiException erro(int status, byte[] corpo) {
        String msg = null;
        try {
            JsonNode n = om.readTree(corpo);
            if (n != null && n.hasNonNull("erro")) msg = n.get("erro").asText();
        } catch (IOException ignorado) {
            // corpo não-JSON: usa mensagem padrão abaixo
        }
        if (msg == null) {
            msg = switch (status) {
                case 401 -> "Sessão expirada. Entre novamente.";
                case 403 -> "Você não tem permissão para isso.";
                case 404 -> "Não encontrado.";
                default -> "Erro inesperado no servidor (" + status + ").";
            };
        }
        return new ApiException(status, msg);
    }
}
