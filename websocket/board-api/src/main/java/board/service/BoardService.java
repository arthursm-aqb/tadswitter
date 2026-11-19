package board.service;

import board.model.Comentario;
import board.model.Postagem;
import board.model.StatusModeracao;
import board.repository.ComentarioRepository;
import board.repository.PostagemRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BoardService {
    private final PostagemRepository postagens;
    private final ComentarioRepository comentarios;
    private final ModeracaoAsyncService moderacaoAsync;

    public BoardService(PostagemRepository postagens, ComentarioRepository comentarios,
                        ModeracaoAsyncService moderacaoAsync) {
        this.postagens = postagens;
        this.comentarios = comentarios;
        this.moderacaoAsync = moderacaoAsync;
    }

    public List<Postagem> listar() { return postagens.findAprovadasOrderByIdDesc(); }

    public Postagem buscar(Long id) {
        Postagem postagem = postagens.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Postagem não encontrada"));
        if (postagem.getStatus() != null && postagem.getStatus() != StatusModeracao.APROVADO) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postagem não encontrada");
        }
        return postagem;
    }

    public List<Comentario> comentarios(Long postagemId) {
        return comentarios.findAprovadosByPostagemIdOrderByIdAsc(postagemId);
    }

    public Postagem criarPostagem(Long autorId, String autorNome, String titulo, String texto) {
        if (titulo == null || titulo.isBlank() || titulo.length() > 150)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título deve ter entre 1 e 150 caracteres");
        validar(texto);
        Postagem salva = postagens.save(new Postagem(autorId, autorNome, titulo.trim(), texto.trim()));
        moderacaoAsync.moderarPostagem(salva.getId());
        return salva;
    }

    public Comentario criarComentario(Long postagemId, Long autorId, String autorNome, String texto) {
        buscar(postagemId);
        validar(texto);
        Comentario salvo = comentarios.save(new Comentario(postagemId, autorId, autorNome, texto.trim()));
        moderacaoAsync.moderarComentario(salvo.getId());
        return salvo;
    }

    private void validar(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Texto deve ter entre 1 e 5000 caracteres");
    }
}
