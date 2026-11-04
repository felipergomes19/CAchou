package br.com.achou.api.repository;

import br.com.achou.api.domain.CodigoRecuperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoRecuperacaoRepository extends JpaRepository<CodigoRecuperacao, Long> {

    Optional<CodigoRecuperacao> findFirstByUsuarioIdAndUsadoFalseOrderByCriadoEmDesc(Long usuarioId);

    @Modifying
    @Query("delete from CodigoRecuperacao c where c.usuario.id = :id")
    void apagarDoUsuario(@Param("id") Long id);
}
