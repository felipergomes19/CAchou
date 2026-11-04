package br.com.achou.api.repository;

import br.com.achou.api.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByRm(String rm);

    boolean existsByRm(String rm);
}
