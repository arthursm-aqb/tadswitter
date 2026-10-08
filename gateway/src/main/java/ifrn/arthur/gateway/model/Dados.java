package ifrn.arthur.gateway.model;

import java.time.Instant;
import java.util.List;
import org.springframework.hateoas.EntityModel;

public class Dados {
    public record Cadastro(String nome, String login, String senha) { }
    public record Credenciais(String login, String senha) { }
    public record Usuario(Long id, String nome, String login) { }
    public record LoginResposta(String token, String tipo, Usuario usuario) { }
    public record Texto(String texto) { }
    public record MensagemInterna(Long autorId, String autorNome, String texto) { }
    public record Comentario(Long id, Long postagemId, Long autorId, String autorNome, String texto, Instant criadoEm) { }
    public record Postagem(Long id, Long autorId, String autorNome, String texto, Instant criadoEm, List<Comentario> comentarios) { }
    public record PostagemPublica(Long id, Long autorId, String autorNome, String texto, Instant criadoEm,
                                  List<EntityModel<Comentario>> comentarios) { }
    public record UsuarioToken(Long id, String nome) { }
}
