package br.com.achou.api.dto;

import br.com.achou.api.domain.Categoria;
import br.com.achou.api.domain.Tipo;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/** Todos os contratos da API (JSON em português, igual ao restante do projeto). */
public final class Dtos {

    private Dtos() {}

    // ---------- Entrada ----------

    public record CadastroRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 40) String nome,
            @NotBlank(message = "Informe seu sobrenome.") @Size(max = 60) String sobrenome,
            @NotBlank(message = "Informe seu RM.") @Pattern(regexp = "\\d{4,8}", message = "O RM deve ter de 4 a 8 dígitos.") String rm,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = "Informe seu curso.") @Size(max = 60) String curso,
            @Min(value = 1, message = "Ano inválido.") @Max(value = 4, message = "Ano inválido.") int ano,
            @NotBlank(message = "Informe uma senha.") @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres.") String senha) {}

    public record LoginRequest(
            @NotBlank(message = "Informe seu RM.") String rm,
            @NotBlank(message = "Informe sua senha.") String senha) {}

    public record EsqueciSenhaRequest(@NotBlank(message = "Informe seu RM.") String rm) {}

    public record RedefinirSenhaRequest(
            @NotBlank String rm,
            @NotBlank(message = "Informe o código recebido.") @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos.") String codigo,
            @NotBlank @Size(min = 6, max = 72, message = "A senha deve ter de 6 a 72 caracteres.") String novaSenha) {}

    public record AtualizarPerfilRequest(
            @NotBlank(message = "Informe seu nome.") @Size(max = 40) String nome,
            @NotBlank(message = "Informe seu sobrenome.") @Size(max = 60) String sobrenome,
            @NotBlank(message = "Informe seu e-mail.") @Email(message = "E-mail inválido.") @Size(max = 120) String email,
            @NotBlank(message = "Informe seu curso.") @Size(max = 60) String curso,
            @Min(1) @Max(4) int ano) {}

    public record AlterarSenhaRequest(
            @NotBlank(message = "Informe a senha atual.") String senhaAtual,
            @NotBlank @Size(min = 6, max = 72, message = "A nova senha deve ter de 6 a 72 caracteres.") String novaSenha) {}

    public record PublicacaoRequest(
            @NotNull(message = "Informe se perdeu ou achou.") Tipo tipo,
            @NotBlank(message = "Informe o título.") @Size(max = 60) String titulo,
            @Size(max = 300) String descricao,
            @NotNull(message = "Escolha uma categoria.") Categoria categoria,
            @Size(max = 40) String subcategoria,
            @NotBlank(message = "Informe o local aproximado.") @Size(max = 50) String local) {}

    public record ComentarioRequest(
            @NotBlank(message = "Escreva um comentário.") @Size(max = 200) String texto) {}

    // ---------- Saída ----------

    public record MensagemDto(String mensagem) {}

    public record ErroDto(String erro, java.util.Map<String, String> campos) {}

    /** Dados do próprio usuário (inclui RM e e-mail; nunca é enviado para outros usuários). */
    public record UsuarioDto(Long id, String rm, String nome, String sobrenome, String nomeCompleto, String email,
                             String curso, int ano, String turma, String fotoUrl, Instant criadoEm) {}

    /** Dados públicos do autor de uma publicação/comentário (sem RM nem e-mail). */
    public record AutorDto(Long id, String nome, String sobrenome, String nomeCompleto, String turma, String fotoUrl) {}

    public record AuthResponse(String token, UsuarioDto usuario) {}

    public record PublicacaoDto(Long id, Tipo tipo, String titulo, String descricao, Categoria categoria,
                                String categoriaRotulo, String subcategoria, String local, AutorDto autor,
                                Instant criadoEm, boolean devolvido, String fotoUrl, long comentarios, boolean minha) {}

    public record ComentarioDto(Long id, AutorDto autor, String texto, Instant criadoEm, boolean meu) {}

    public record PaginaDto<T>(List<T> itens, int pagina, int totalPaginas, long total) {}

    public record CategoriaDto(Categoria codigo, String rotulo, List<String> subcategorias) {}

    public record EstatisticasDto(long publicacoes, long perdidos, long achados, long devolvidos, long comentarios) {}
}
