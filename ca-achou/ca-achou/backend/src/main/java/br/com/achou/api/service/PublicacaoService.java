package br.com.achou.api.service;

import br.com.achou.api.domain.*;
import br.com.achou.api.dto.Dtos.*;
import br.com.achou.api.repository.ComentarioRepository;
import br.com.achou.api.repository.PublicacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PublicacaoService {

    private final PublicacaoRepository publicacoes;
    private final ComentarioRepository comentarios;
    private final UsuarioService usuarios;
    private final ArquivoService arquivos;
    private final Mapper mapper;

    public PublicacaoService(PublicacaoRepository publicacoes, ComentarioRepository comentarios,
                             UsuarioService usuarios, ArquivoService arquivos, Mapper mapper) {
        this.publicacoes = publicacoes;
        this.comentarios = comentarios;
        this.usuarios = usuarios;
        this.arquivos = arquivos;
        this.mapper = mapper;
    }

    /** Filtros opcionais da listagem. */
    public record Filtro(Tipo tipo, Categoria categoria, String subcategoria, String busca,
                         boolean somenteAbertos, boolean minhas) {}

    @Transactional(readOnly = true)
    public PaginaDto<PublicacaoDto> listar(Long usuarioId, Filtro f, int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), 50);
        var pageable = PageRequest.of(Math.max(pagina, 0), tam, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        Page<Publicacao> page = publicacoes.findAll(especificacao(usuarioId, f), pageable);

        Map<Long, Long> contagens = new HashMap<>();
        if (!page.isEmpty()) {
            var ids = page.getContent().stream().map(Publicacao::getId).toList();
            for (Object[] linha : comentarios.contarPorPublicacao(ids)) {
                contagens.put((Long) linha[0], (Long) linha[1]);
            }
        }
        List<PublicacaoDto> itens = page.getContent().stream()
                .map(p -> mapper.publicacao(p, contagens.getOrDefault(p.getId(), 0L), usuarioId))
                .collect(Collectors.toList());
        return new PaginaDto<>(itens, page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    private Specification<Publicacao> especificacao(Long usuarioId, Filtro f) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (f.tipo() != null) ps.add(cb.equal(root.get("tipo"), f.tipo()));
            if (f.categoria() != null) ps.add(cb.equal(root.get("categoria"), f.categoria()));
            if (f.subcategoria() != null && !f.subcategoria().isBlank()) {
                ps.add(cb.equal(root.get("subcategoria"), f.subcategoria().trim()));
            }
            if (f.somenteAbertos()) ps.add(cb.isFalse(root.get("devolvido")));
            if (f.minhas()) ps.add(cb.equal(root.get("autor").get("id"), usuarioId));
            if (f.busca() != null && !f.busca().isBlank()) {
                String padrao = "%" + escaparLike(f.busca().trim().toLowerCase()) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), padrao, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("descricao"), "")), padrao, '\\'),
                        cb.like(cb.lower(root.get("local")), padrao, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("subcategoria"), "")), padrao, '\\')));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static String escaparLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional
    public PublicacaoDto criar(Long usuarioId, PublicacaoRequest r) {
        Usuario autor = usuarios.obter(usuarioId);
        String sub = r.subcategoria() == null || r.subcategoria().isBlank() ? null : r.subcategoria().trim();
        if (sub != null && !r.categoria().aceitaSubcategoria(sub)) {
            throw ApiException.invalido("Tipo de item inválido para a categoria " + r.categoria().getRotulo() + ".");
        }
        Publicacao p = new Publicacao();
        p.setTipo(r.tipo());
        p.setTitulo(r.titulo().trim());
        p.setDescricao(r.descricao() == null || r.descricao().isBlank() ? null : r.descricao().trim());
        p.setCategoria(r.categoria());
        p.setSubcategoria(sub);
        p.setLocal(r.local().trim());
        p.setAutor(autor);
        return mapper.publicacao(publicacoes.save(p), 0, usuarioId);
    }

    @Transactional
    public PublicacaoDto definirFoto(Long usuarioId, Long id, byte[] dados) {
        Publicacao p = daAutor(usuarioId, id);
        String novo = arquivos.salvar(dados);
        String antigo = p.getFoto();
        p.setFoto(novo);
        publicacoes.save(p);
        arquivos.excluir(antigo);
        return mapper.publicacao(p, comentarios.countByPublicacaoId(id), usuarioId);
    }

    @Transactional
    public PublicacaoDto marcarDevolvido(Long usuarioId, Long id) {
        Publicacao p = daAutor(usuarioId, id);
        if (!p.isDevolvido()) p.marcarDevolvido();
        publicacoes.save(p);
        return mapper.publicacao(p, comentarios.countByPublicacaoId(id), usuarioId);
    }

    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Publicacao p = daAutor(usuarioId, id);
        comentarios.apagarDaPublicacao(id);
        publicacoes.delete(p);
        arquivos.excluir(p.getFoto());
    }

    @Transactional(readOnly = true)
    public List<ComentarioDto> listarComentarios(Long usuarioId, Long publicacaoId) {
        if (!publicacoes.existsById(publicacaoId)) throw ApiException.naoEncontrado("Publicação não encontrada.");
        return comentarios.findByPublicacaoIdOrderByCriadoEmAscIdAsc(publicacaoId).stream()
                .map(c -> mapper.comentario(c, usuarioId)).toList();
    }

    @Transactional
    public ComentarioDto comentar(Long usuarioId, Long publicacaoId, ComentarioRequest r) {
        Publicacao p = publicacoes.findById(publicacaoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Publicação não encontrada."));
        Usuario autor = usuarios.obter(usuarioId);
        Comentario c = comentarios.save(new Comentario(p, autor, r.texto().trim()));
        return mapper.comentario(c, usuarioId);
    }

    /** Carrega a publicação garantindo que pertence ao usuário (404 se não existir, 403 se for de outro). */
    private Publicacao daAutor(Long usuarioId, Long id) {
        Publicacao p = publicacoes.findWithAutorById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Publicação não encontrada."));
        if (!p.getAutor().getId().equals(usuarioId)) {
            throw ApiException.proibido("Apenas quem publicou o item pode alterá-lo.");
        }
        return p;
    }
}
