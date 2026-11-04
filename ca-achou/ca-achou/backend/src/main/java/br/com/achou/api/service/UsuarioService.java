package br.com.achou.api.service;

import br.com.achou.api.domain.Tipo;
import br.com.achou.api.domain.Usuario;
import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.repository.ComentarioRepository;
import br.com.achou.api.repository.PublicacaoRepository;
import br.com.achou.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PublicacaoRepository publicacoes;
    private final ComentarioRepository comentarios;
    private final PasswordEncoder encoder;
    private final ArquivoService arquivos;
    private final Mapper mapper;

    public UsuarioService(UsuarioRepository usuarios, PublicacaoRepository publicacoes,
                          ComentarioRepository comentarios, PasswordEncoder encoder,
                          ArquivoService arquivos, Mapper mapper) {
        this.usuarios = usuarios;
        this.publicacoes = publicacoes;
        this.comentarios = comentarios;
        this.encoder = encoder;
        this.arquivos = arquivos;
        this.mapper = mapper;
    }

    /** Token válido de um usuário que não existe mais deve ser tratado como sessão inválida. */
    public Usuario obter(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> ApiException.naoAutenticado("Sessão inválida. Entre novamente."));
    }

    @Transactional(readOnly = true)
    public UsuarioDto perfil(Long id) {
        return mapper.usuario(obter(id));
    }

    @Transactional
    public UsuarioDto atualizar(Long id, AtualizarPerfilRequest r) {
        Usuario u = obter(id);
        u.setNome(r.nome().trim());
        u.setSobrenome(r.sobrenome().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setCurso(r.curso().trim());
        u.setAno(r.ano());
        return mapper.usuario(usuarios.save(u));
    }

    @Transactional
    public MensagemDto alterarSenha(Long id, AlterarSenhaRequest r) {
        Usuario u = obter(id);
        if (!encoder.matches(r.senhaAtual(), u.getSenhaHash())) {
            throw ApiException.invalido("A senha atual está incorreta.");
        }
        u.setSenhaHash(encoder.encode(r.novaSenha()));
        usuarios.save(u);
        return new MensagemDto("Senha alterada com sucesso.");
    }

    @Transactional
    public UsuarioDto definirFoto(Long id, byte[] dados) {
        Usuario u = obter(id);
        String novo = arquivos.salvar(dados);
        String antigo = u.getFoto();
        u.setFoto(novo);
        usuarios.save(u);
        arquivos.excluir(antigo);
        return mapper.usuario(u);
    }

    @Transactional
    public UsuarioDto removerFoto(Long id) {
        Usuario u = obter(id);
        String antigo = u.getFoto();
        u.setFoto(null);
        usuarios.save(u);
        arquivos.excluir(antigo);
        return mapper.usuario(u);
    }

    @Transactional(readOnly = true)
    public EstatisticasDto estatisticas(Long id) {
        return new EstatisticasDto(
                publicacoes.countByAutorId(id),
                publicacoes.countByAutorIdAndTipo(id, Tipo.PERDI),
                publicacoes.countByAutorIdAndTipo(id, Tipo.ACHEI),
                publicacoes.countByAutorIdAndDevolvidoTrue(id),
                comentarios.countByAutorId(id));
    }
}
