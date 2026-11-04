package br.com.achou.api.web;

import br.com.achou.api.domain.Categoria;
import br.com.achou.api.domain.Tipo;
import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.security.UserPrincipal;
import br.com.achou.api.service.PublicacaoService;
import br.com.achou.api.service.PublicacaoService.Filtro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/publicacoes")
public class PublicacaoController {

    private final PublicacaoService service;

    public PublicacaoController(PublicacaoService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaDto<PublicacaoDto> listar(
            @AuthenticationPrincipal UserPrincipal u,
            @RequestParam(required = false) Tipo tipo,
            @RequestParam(required = false) Categoria categoria,
            @RequestParam(required = false) String subcategoria,
            @RequestParam(required = false, name = "q") String busca,
            @RequestParam(defaultValue = "false") boolean somenteAbertos,
            @RequestParam(defaultValue = "false") boolean minhas,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return service.listar(u.id(), new Filtro(tipo, categoria, subcategoria, busca, somenteAbertos, minhas),
                pagina, tamanho);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicacaoDto criar(@AuthenticationPrincipal UserPrincipal u, @Valid @RequestBody PublicacaoRequest r) {
        return service.criar(u.id(), r);
    }

    /** Corpo = bytes da imagem (JPG, PNG ou WebP). */
    @PutMapping("/{id}/foto")
    public PublicacaoDto foto(@AuthenticationPrincipal UserPrincipal u, @PathVariable Long id, @RequestBody byte[] dados) {
        return service.definirFoto(u.id(), id, dados);
    }

    @PatchMapping("/{id}/devolvido")
    public PublicacaoDto devolvido(@AuthenticationPrincipal UserPrincipal u, @PathVariable Long id) {
        return service.marcarDevolvido(u.id(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@AuthenticationPrincipal UserPrincipal u, @PathVariable Long id) {
        service.excluir(u.id(), id);
    }

    @GetMapping("/{id}/comentarios")
    public List<ComentarioDto> comentarios(@AuthenticationPrincipal UserPrincipal u, @PathVariable Long id) {
        return service.listarComentarios(u.id(), id);
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioDto comentar(@AuthenticationPrincipal UserPrincipal u, @PathVariable Long id,
                                  @Valid @RequestBody ComentarioRequest r) {
        return service.comentar(u.id(), id, r);
    }
}
