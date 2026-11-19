package board.repository;

import board.model.Comentario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query("select c from Comentario c where c.postagemId = :postagemId and (c.status = board.model.StatusModeracao.APROVADO or c.status is null) order by c.id asc")
    List<Comentario> findAprovadosByPostagemIdOrderByIdAsc(@Param("postagemId") Long postagemId);
}
