package gateway.controller;

import gateway.model.Dados;
import gateway.service.ApiInternaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/postagens")
@SecurityRequirement(name = "bearerAuth")
public class BoardController {
    private final ApiInternaService api;
    public BoardController(ApiInternaService api) { this.api = api; }
    private String base() { return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString(); }
    private Dados.UsuarioToken usuario(HttpServletRequest request) { return (Dados.UsuarioToken) request.getAttribute("usuario"); }

    private EntityModel<Dados.Comentario> comentario(Dados.Comentario c) {
        return EntityModel.of(c, Link.of(base() + "/api/postagens/" + c.postagemId()).withRel("postagem"));
    }
    private EntityModel<Dados.PostagemPublica> postagem(Dados.Postagem p) {
        List<EntityModel<Dados.Comentario>> comentarios = p.comentarios().stream().map(this::comentario).toList();
        return EntityModel.of(new Dados.PostagemPublica(p.id(), p.autorId(), p.autorNome(), p.titulo(), p.texto(), p.criadoEm(), comentarios),
                Link.of(base() + "/api/postagens/" + p.id()).withSelfRel(),
                Link.of(base() + "/api/postagens/" + p.id() + "/comentarios").withRel("comentar"),
                Link.of(base() + "/api/postagens").withRel("board"));
    }

    @GetMapping
    @Operation(summary = "Board com todas as postagens; comentários do mais antigo ao mais recente")
    public CollectionModel<EntityModel<Dados.PostagemPublica>> listar() {
        return CollectionModel.of(api.listar().stream().map(this::postagem).toList(),
                Link.of(base() + "/api/postagens").withSelfRel(),
                Link.of(base() + "/api/postagens").withRel("criar"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar uma postagem")
    public EntityModel<Dados.PostagemPublica> buscar(@PathVariable Long id) { return postagem(api.buscar(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar postagem")
    public EntityModel<Dados.PostagemPublica> criar(@RequestBody Dados.NovaPostagem dados, HttpServletRequest request) {
        Dados.UsuarioToken u = usuario(request);
        return postagem(api.criar(new Dados.PostagemInterna(u.id(), u.nome(), dados.titulo(), dados.texto())));
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comentar uma postagem")
    public EntityModel<Dados.Comentario> comentar(@PathVariable Long id, @RequestBody Dados.Texto dados,
                                                   HttpServletRequest request) {
        Dados.UsuarioToken u = usuario(request);
        return comentario(api.comentar(id, new Dados.MensagemInterna(u.id(), u.nome(), dados.texto())));
    }
}
