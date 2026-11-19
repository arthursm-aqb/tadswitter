package board.repository;

import board.model.Postagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostagemRepository extends JpaRepository<Postagem, Long> {
    @Query("select p from Postagem p where p.status = board.model.StatusModeracao.APROVADO or p.status is null order by p.id desc")
    List<Postagem> findAprovadasOrderByIdDesc();
}
