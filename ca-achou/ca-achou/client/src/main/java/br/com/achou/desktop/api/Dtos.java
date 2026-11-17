package br.com.achou.desktop.api;

import java.time.Instant;
import java.util.List;

/** Espelho dos contratos JSON do backend (Spring Boot). */
public final class Dtos {

    private Dtos() {}

    /** Espelho de {@code br.com.achou.api.domain.Turno}: o nome trafega na API, o rótulo aparece na tela. */
    public enum Turno {
        MANHA("Manhã"), TARDE("Tarde"), NOITE("Noite");

        public final String rotulo;

        Turno(String rotulo) {
            this.rotulo = rotulo;
        }

        /** O ComboBox usa toString() para exibir cada opção. */
        @Override
        public String toString() {
            return rotulo;
        }
    }

    // ----- respostas -----
    /** {@code turno}/{@code turnoRotulo} são nulos em contas antigas, criadas antes do campo existir. */
    public record Usuario(Long id, String rm, String nome, String sobrenome, String nomeCompleto, String email,
                          String curso, int ano, String turma, String turno, String turnoRotulo,
                          String fotoUrl, Instant criadoEm) {}

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
                           Turno turno, String senha) {}

    public record AtualizarPerfil(String nome, String sobrenome, String email, String curso, int ano,
                                  Turno turno) {}

    public record NovaPublicacao(String tipo, String titulo, String descricao, String categoria,
                                 String subcategoria, String local) {}
}
