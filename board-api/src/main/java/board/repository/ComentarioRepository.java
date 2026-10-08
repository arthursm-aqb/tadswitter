package board.repository;

import board.model.Comentario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    List<Comentario> findByPostagemIdOrderByIdAsc(Long postagemId);
}
