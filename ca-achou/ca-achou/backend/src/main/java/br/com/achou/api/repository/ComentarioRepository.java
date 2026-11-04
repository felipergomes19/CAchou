package br.com.achou.api.repository;

import br.com.achou.api.domain.Comentario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    @EntityGraph(attributePaths = "autor")
    List<Comentario> findByPublicacaoIdOrderByCriadoEmAscIdAsc(Long publicacaoId);

    long countByAutorId(Long autorId);

    long countByPublicacaoId(Long publicacaoId);

    @Query("select c.publicacao.id, count(c) from Comentario c where c.publicacao.id in :ids group by c.publicacao.id")
    List<Object[]> contarPorPublicacao(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from Comentario c where c.publicacao.id = :id")
    void apagarDaPublicacao(@Param("id") Long id);
}
