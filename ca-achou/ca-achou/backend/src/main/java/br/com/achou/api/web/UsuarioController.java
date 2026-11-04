package br.com.achou.api.web;

import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.security.UserPrincipal;
import br.com.achou.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping
    public UsuarioDto perfil(@AuthenticationPrincipal UserPrincipal u) {
        return usuarios.perfil(u.id());
    }

    @PutMapping
    public UsuarioDto atualizar(@AuthenticationPrincipal UserPrincipal u, @Valid @RequestBody AtualizarPerfilRequest r) {
        return usuarios.atualizar(u.id(), r);
    }

    @PutMapping("/senha")
    public MensagemDto alterarSenha(@AuthenticationPrincipal UserPrincipal u, @Valid @RequestBody AlterarSenhaRequest r) {
        return usuarios.alterarSenha(u.id(), r);
    }

    /** Corpo = bytes da imagem (JPG, PNG ou WebP). */
    @PutMapping("/foto")
    public UsuarioDto definirFoto(@AuthenticationPrincipal UserPrincipal u, @RequestBody byte[] dados) {
        return usuarios.definirFoto(u.id(), dados);
    }

    @DeleteMapping("/foto")
    public UsuarioDto removerFoto(@AuthenticationPrincipal UserPrincipal u) {
        return usuarios.removerFoto(u.id());
    }

    @GetMapping("/estatisticas")
    public EstatisticasDto estatisticas(@AuthenticationPrincipal UserPrincipal u) {
        return usuarios.estatisticas(u.id());
    }
}
