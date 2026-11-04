package br.com.achou.desktop.api;

import java.time.Instant;
import java.util.List;

/** Espelho dos contratos JSON do backend (Spring Boot). */
public final class Dtos {

    private Dtos() {}

    // ----- respostas -----
    public record Usuario(Long id, String rm, String nome, String sobrenome, String nomeCompleto, String email,
                          String curso, int ano, String turma, String fotoUrl, Instant criadoEm) {}

    public record Autor(Long id, String nome, String sobrenome, String nomeCompleto, String turma, String fotoUrl) {}

    public record AuthResponse(String token, Usuario usuario) {}

    public record Publicacao(Long id, String tipo, String titulo, String descricao, String categoria,
                             String categoriaRotulo, String subcategoria, String local, Autor autor,
                             Instant criadoEm, boolean devolvido, String fotoUrl, long comentarios, boolean minha) {}

    public record Comentario(Long id, Autor autor, String texto, Instant criadoEm, boolean meu) {}

    public record Pagina<T>(List<T> itens, int pagina, int totalPaginas, long total) {}

    public record Categoria(String codigo, String rotulo, List<String> subcategorias) {}

    public record Estatisticas(long publicacoes, long perdidos, long achados, long devolvidos, long comentarios) {}

    public record Mensagem(String mensagem) {}

    // ----- requisições -----
    public record Cadastro(String nome, String sobrenome, String rm, String email, String curso, int ano,
                           String senha) {}

    public record AtualizarPerfil(String nome, String sobrenome, String email, String curso, int ano) {}

    public record NovaPublicacao(String tipo, String titulo, String descricao, String categoria,
                                 String subcategoria, String local) {}
}
