package ifrn.arthur.board.repository;

import ifrn.arthur.board.model.Postagem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostagemRepository extends JpaRepository<Postagem, Long> {
    List<Postagem> findAllByOrderByIdDesc();
}
