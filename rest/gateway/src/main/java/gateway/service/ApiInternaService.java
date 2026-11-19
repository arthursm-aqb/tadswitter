package gateway.service;

import gateway.model.Dados;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiInternaService {
    private final RestClient usuarios;
    private final RestClient board;
    public ApiInternaService(@Value("${services.usuarios}") String urlUsuarios,
                            @Value("${services.board}") String urlBoard) {
        usuarios = RestClient.builder().baseUrl(urlUsuarios).build();
        board = RestClient.builder().baseUrl(urlBoard).build();
    }
    public Dados.Usuario cadastrar(Dados.Cadastro dados) {
        try { return usuarios.post().uri("/usuarios").body(dados).retrieve().body(Dados.Usuario.class); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    public Dados.Usuario autenticar(Dados.Credenciais dados) {
        try { return usuarios.post().uri("/usuarios/autenticar").body(dados).retrieve().body(Dados.Usuario.class); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    public List<Dados.Postagem> listar() {
        try { return board.get().uri("/postagens").retrieve().body(new ParameterizedTypeReference<>() { }); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    public Dados.Postagem buscar(Long id) {
        try { return board.get().uri("/postagens/{id}", id).retrieve().body(Dados.Postagem.class); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    public Dados.Postagem criar(Dados.PostagemInterna dados) {
        try { return board.post().uri("/postagens").body(dados).retrieve().body(Dados.Postagem.class); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    public Dados.Comentario comentar(Long id, Dados.MensagemInterna dados) {
        try { return board.post().uri("/postagens/{id}/comentarios", id).body(dados).retrieve().body(Dados.Comentario.class); }
        catch (RestClientResponseException e) { throw erro(e); }
        catch (ResourceAccessException e) { throw indisponivel(); }
    }
    private ResponseStatusException erro(RestClientResponseException e) {
        return new ResponseStatusException(e.getStatusCode(), "Erro da API interna");
    }
    private ResponseStatusException indisponivel() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "API interna indisponível");
    }
}
