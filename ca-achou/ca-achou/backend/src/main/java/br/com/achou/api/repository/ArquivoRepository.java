package br.com.achou.api.repository;

import br.com.achou.api.domain.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArquivoRepository extends JpaRepository<Arquivo, String> {
}
