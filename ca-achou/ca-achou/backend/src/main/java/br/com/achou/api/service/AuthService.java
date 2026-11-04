package br.com.achou.api.service;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.CodigoRecuperacao;
import br.com.achou.api.domain.Usuario;
import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.repository.CodigoRecuperacaoRepository;
import br.com.achou.api.repository.UsuarioRepository;
import br.com.achou.api.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {

    private static final String MSG_GENERICA_RESET =
            "Se o RM estiver cadastrado, enviamos um código para o e-mail informado no cadastro.";

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacaoRepository codigos;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Mapper mapper;
    private final EmailService email;
    private final TentativasService tentativas;
    private final AchouProperties props;
    private final SecureRandom random = new SecureRandom();
    /** Hash descartável para igualar o tempo de resposta quando o RM não existe. */
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, CodigoRecuperacaoRepository codigos, PasswordEncoder encoder,
                       JwtService jwt, Mapper mapper, EmailService email, TentativasService tentativas,
                       AchouProperties props) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mapper = mapper;
        this.email = email;
        this.tentativas = tentativas;
        this.props = props;
        this.hashFalso = encoder.encode("senha-falsa-para-timing");
    }

    @Transactional
    public AuthResponse cadastrar(CadastroRequest r) {
        String rm = r.rm().trim();
        if (usuarios.existsByRm(rm)) {
            throw ApiException.conflito("Este RM já possui cadastro. Entre ou use \"Esqueci minha senha\".");
        }
        Usuario u = new Usuario();
        u.setRm(rm);
        u.setNome(r.nome().trim());
        u.setSobrenome(r.sobrenome().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setCurso(r.curso().trim());
        u.setAno(r.ano());
        u.setSenhaHash(encoder.encode(r.senha()));
        usuarios.save(u);
        return new AuthResponse(jwt.gerar(u), mapper.usuario(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse entrar(LoginRequest r) {
        String rm = r.rm().trim();
        tentativas.verificar(rm);
        Optional<Usuario> opt = usuarios.findByRm(rm);
        boolean ok = opt.map(u -> encoder.matches(r.senha(), u.getSenhaHash()))
                .orElseGet(() -> { encoder.matches(r.senha(), hashFalso); return false; });
        if (!ok) {
            tentativas.falhou(rm);
            throw ApiException.naoAutenticado("RM ou senha incorretos.");
        }
        tentativas.sucesso(rm);
        Usuario u = opt.get();
        return new AuthResponse(jwt.gerar(u), mapper.usuario(u));
    }

    /** Gera e envia um código de 6 dígitos. A resposta é sempre a mesma, exista o RM ou não. */
    @Transactional
    public MensagemDto esqueciSenha(EsqueciSenhaRequest r) {
        usuarios.findByRm(r.rm().trim()).ifPresent(u -> {
            var recente = codigos.findFirstByUsuarioIdAndUsadoFalseOrderByCriadoEmDesc(u.getId());
            Duration intervalo = Duration.ofSeconds(props.recuperacao().intervaloSegundos());
            if (recente.isPresent() && recente.get().getCriadoEm().plus(intervalo).isAfter(Instant.now())) {
                return; // pedido muito recente: ignora silenciosamente
            }
            codigos.apagarDoUsuario(u.getId());
            String codigo = String.format("%06d", random.nextInt(1_000_000));
            Instant expira = Instant.now().plus(Duration.ofMinutes(props.recuperacao().validadeMinutos()));
            codigos.save(new CodigoRecuperacao(u, encoder.encode(codigo), expira));
            email.enviarCodigoRecuperacao(u, codigo, props.recuperacao().validadeMinutos());
        });
        return new MensagemDto(MSG_GENERICA_RESET);
    }

    /** noRollbackFor: o contador de tentativas inválidas precisa ser gravado mesmo quando lançamos o erro. */
    @Transactional(noRollbackFor = ApiException.class)
    public MensagemDto redefinirSenha(RedefinirSenhaRequest r) {
        final String erroGenerico = "Código inválido ou expirado. Solicite um novo código.";
        Usuario u = usuarios.findByRm(r.rm().trim()).orElseThrow(() -> ApiException.invalido(erroGenerico));
        CodigoRecuperacao c = codigos.findFirstByUsuarioIdAndUsadoFalseOrderByCriadoEmDesc(u.getId())
                .orElseThrow(() -> ApiException.invalido(erroGenerico));

        if (c.getExpiraEm().isBefore(Instant.now()) || c.getTentativas() >= props.recuperacao().maxTentativas()) {
            throw ApiException.invalido(erroGenerico);
        }
        if (!encoder.matches(r.codigo(), c.getCodigoHash())) {
            c.registrarTentativa();
            codigos.save(c);
            throw ApiException.invalido(erroGenerico);
        }
        u.setSenhaHash(encoder.encode(r.novaSenha()));
        usuarios.save(u);
        c.marcarUsado();
        codigos.apagarDoUsuario(u.getId());
        tentativas.sucesso(u.getRm());
        return new MensagemDto("Senha redefinida com sucesso. Agora você já pode entrar.");
    }
}
