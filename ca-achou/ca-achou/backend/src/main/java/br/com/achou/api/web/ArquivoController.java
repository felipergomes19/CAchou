package br.com.achou.api.web;

import br.com.achou.api.service.ArquivoService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Serve as fotos. Exige login (fotos de alunos não ficam públicas). Nomes são UUIDs e nunca mudam. */
@RestController
@RequestMapping("/api/arquivos")
public class ArquivoController {

    private final ArquivoService arquivos;

    public ArquivoController(ArquivoService arquivos) {
        this.arquivos = arquivos;
    }

    @GetMapping("/{nome:.+}")
    public ResponseEntity<Resource> baixar(@PathVariable String nome) {
        Resource r = arquivos.carregar(nome);
        return ResponseEntity.ok()
                .contentType(arquivos.tipoDe(nome))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(r);
    }
}
