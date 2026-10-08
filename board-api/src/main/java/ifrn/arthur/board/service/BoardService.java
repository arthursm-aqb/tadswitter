package ifrn.arthur.board.service;

import ifrn.arthur.board.model.Comentario;
import ifrn.arthur.board.model.Postagem;
import ifrn.arthur.board.repository.ComentarioRepository;
import ifrn.arthur.board.repository.PostagemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoardService {
    private final PostagemRepository postagens;
    private final ComentarioRepository comentarios;
    public BoardService(PostagemRepository postagens, ComentarioRepository comentarios) {
        this.postagens = postagens;
        this.comentarios = comentarios;
    }
    public List<Postagem> listar() { return postagens.findAllByOrderByIdDesc(); }
    public Postagem buscar(Long id) {
        return postagens.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Postagem não encontrada"));
    }
    public List<Comentario> comentarios(Long postagemId) { return comentarios.findByPostagemIdOrderByIdAsc(postagemId); }
    public Postagem criarPostagem(Long autorId, String autorNome, String texto) {
        validar(texto);
        return postagens.save(new Postagem(autorId, autorNome, texto.trim()));
    }
    public Comentario criarComentario(Long postagemId, Long autorId, String autorNome, String texto) {
        buscar(postagemId);
        validar(texto);
        return comentarios.save(new Comentario(postagemId, autorId, autorNome, texto.trim()));
    }
    private void validar(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto deve ter entre 1 e 5000 caracteres");
    }
}
