package board.controller;

import board.model.Comentario;
import board.model.Postagem;
import board.service.BoardService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/postagens")
public class BoardController {
    private final BoardService service;
    public BoardController(BoardService service) { this.service = service; }

    public record NovaPostagem(Long autorId, String autorNome, String titulo, String texto) { }
    public record NovaMensagem(Long autorId, String autorNome, String texto) { }
    public record ComentarioResposta(Long id, Long postagemId, Long autorId, String autorNome, String texto, Instant criadoEm) {
        public ComentarioResposta(Comentario c) { this(c.getId(), c.getPostagemId(), c.getAutorId(), c.getAutorNome(), c.getTexto(), c.getCriadoEm()); }
    }
    public record PostagemResposta(Long id, Long autorId, String autorNome, String titulo, String texto, Instant criadoEm, List<ComentarioResposta> comentarios) { }
    private PostagemResposta resposta(Postagem p) {
        return new PostagemResposta(p.getId(), p.getAutorId(), p.getAutorNome(), p.getTitulo(), p.getTexto(), p.getCriadoEm(),
                service.comentarios(p.getId()).stream().map(ComentarioResposta::new).toList());
    }

    // Listar postagens
    @GetMapping
    public List<PostagemResposta> listar() { return service.listar().stream().map(this::resposta).toList(); }

    // Abrir uma postagem
    @GetMapping("/{id}")
    public PostagemResposta buscar(@PathVariable Long id) { return resposta(service.buscar(id)); }

    // Criar uma postagem
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostagemResposta criar(@RequestBody NovaPostagem dados) {
        return resposta(service.criarPostagem(dados.autorId(), dados.autorNome(), dados.titulo(), dados.texto()));
    }

    // Comentar uma postagem
    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioResposta comentar(@PathVariable Long id, @RequestBody NovaMensagem dados) {
        return new ComentarioResposta(service.criarComentario(id, dados.autorId(), dados.autorNome(), dados.texto()));
    }
}
