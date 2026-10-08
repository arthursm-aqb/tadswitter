package gateway.controller;

import gateway.model.Dados;
import gateway.service.ApiInternaService;
import gateway.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ApiInternaService api;
    private final JwtService jwt;
    public AuthController(ApiInternaService api, JwtService jwt) { this.api = api; this.jwt = jwt; }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar usuário")
    public EntityModel<Dados.Usuario> cadastrar(@RequestBody Dados.Cadastro dados) {
        Dados.Usuario usuario = api.cadastrar(dados);
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return EntityModel.of(usuario, Link.of(base + "/api/auth/login").withRel("login"));
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar e receber JWT válido por 8 horas")
    public EntityModel<Dados.LoginResposta> login(@RequestBody Dados.Credenciais dados) {
        Dados.Usuario usuario = api.autenticar(dados);
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return EntityModel.of(new Dados.LoginResposta(jwt.gerar(usuario), "Bearer", usuario),
                Link.of(base + "/api/postagens").withRel("board"));
    }
}
