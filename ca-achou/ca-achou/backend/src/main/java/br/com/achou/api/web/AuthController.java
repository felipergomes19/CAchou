package br.com.achou.api.web;

import br.com.achou.api.config.AchouProperties;
import br.com.achou.api.domain.Categoria;
import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;
    private final AchouProperties props;

    public AuthController(AuthService auth, AchouProperties props) {
        this.auth = auth;
        this.props = props;
    }

    @PostMapping("/auth/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastro(@Valid @RequestBody CadastroRequest r) {
        return auth.cadastrar(r);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.entrar(r);
    }

    @PostMapping("/auth/esqueci-senha")
    public MensagemDto esqueciSenha(@Valid @RequestBody EsqueciSenhaRequest r) {
        return auth.esqueciSenha(r);
    }

    @PostMapping("/auth/redefinir-senha")
    public MensagemDto redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest r) {
        return auth.redefinirSenha(r);
    }

    /** Lista pública de categorias e seus subtipos (usada nos filtros e no formulário de publicação). */
    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return Arrays.stream(Categoria.values())
                .map(c -> new CategoriaDto(c, c.getRotulo(), c.getSubcategorias()))
                .toList();
    }

    /** Cursos exibidos no cadastro (configuráveis em application.yml). */
    @GetMapping("/cursos")
    public List<String> cursos() {
        return props.cursos();
    }
}
