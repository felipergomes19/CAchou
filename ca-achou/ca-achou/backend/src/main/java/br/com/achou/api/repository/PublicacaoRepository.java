package br.com.achou.api.repository;

import br.com.achou.api.domain.Publicacao;
import br.com.achou.api.domain.Tipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PublicacaoRepository extends JpaRepository<Publicacao, Long>, JpaSpecificationExecutor<Publicacao> {

    @Override
    @EntityGraph(attributePaths = "autor")
    Page<Publicacao> findAll(Specification<Publicacao> spec, Pageable pageable);

    @EntityGraph(attributePaths = "autor")
    Optional<Publicacao> findWithAutorById(Long id);

    long countByAutorId(Long autorId);

    long countByAutorIdAndTipo(Long autorId, Tipo tipo);

    long countByAutorIdAndDevolvidoTrue(Long autorId);
}
