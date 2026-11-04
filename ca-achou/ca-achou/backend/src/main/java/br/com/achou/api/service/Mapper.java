package br.com.achou.api.service;

import br.com.achou.api.domain.Comentario;
import br.com.achou.api.domain.Publicacao;
import br.com.achou.api.domain.Usuario;
import br.com.achou.api.dto.Dtos.*;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

    public static final String ARQUIVOS = "/api/arquivos/";

    private String url(String arquivo) {
        return arquivo == null ? null : ARQUIVOS + arquivo;
    }

    public UsuarioDto usuario(Usuario u) {
        return new UsuarioDto(u.getId(), u.getRm(), u.getNome(), u.getSobrenome(), u.nomeCompleto(), u.getEmail(),
                u.getCurso(), u.getAno(), u.turma(), url(u.getFoto()), u.getCriadoEm());
    }

    public AutorDto autor(Usuario u) {
        return new AutorDto(u.getId(), u.getNome(), u.getSobrenome(), u.nomeCompleto(), u.turma(), url(u.getFoto()));
    }

    public PublicacaoDto publicacao(Publicacao p, long comentarios, Long usuarioLogadoId) {
        return new PublicacaoDto(p.getId(), p.getTipo(), p.getTitulo(), p.getDescricao(), p.getCategoria(),
                p.getCategoria().getRotulo(), p.getSubcategoria(), p.getLocal(), autor(p.getAutor()),
                p.getCriadoEm(), p.isDevolvido(), url(p.getFoto()), comentarios,
                p.getAutor().getId().equals(usuarioLogadoId));
    }

    public ComentarioDto comentario(Comentario c, Long usuarioLogadoId) {
        return new ComentarioDto(c.getId(), autor(c.getAutor()), c.getTexto(), c.getCriadoEm(),
                c.getAutor().getId().equals(usuarioLogadoId));
    }
}
