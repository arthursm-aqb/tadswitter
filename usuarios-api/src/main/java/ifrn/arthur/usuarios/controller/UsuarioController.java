package ifrn.arthur.usuarios.controller;

import ifrn.arthur.usuarios.model.Usuario;
import ifrn.arthur.usuarios.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    public record Credenciais(String login, String senha) { }
    public record Cadastro(String nome, String login, String senha) { }
    public record UsuarioPublico(Long id, String nome, String login) {
        public UsuarioPublico(Usuario usuario) { this(usuario.getId(), usuario.getNome(), usuario.getLogin()); }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioPublico cadastrar(@RequestBody Cadastro dados) {
        return new UsuarioPublico(service.cadastrar(dados.nome(), dados.login(), dados.senha()));
    }

    @PostMapping("/autenticar")
    public UsuarioPublico autenticar(@RequestBody Credenciais dados) {
        return new UsuarioPublico(service.autenticar(dados.login(), dados.senha()));
    }
}
