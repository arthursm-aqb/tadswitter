package usuarios.service;

import usuarios.model.Usuario;
import usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository repository) { this.repository = repository; }

    public Usuario cadastrar(String nome, String login, String senha) {
        if (vazio(nome) || vazio(login) || vazio(senha))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha nome, login e senha");
        login = login.trim().toLowerCase();
        if (repository.existsByLogin(login))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login já cadastrado");
        return repository.save(new Usuario(nome.trim(), login, encoder.encode(senha)));
    }

    public Usuario autenticar(String login, String senha) {
        if (vazio(login) || vazio(senha))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        Usuario usuario = repository.findByLogin(login.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
        if (!encoder.matches(senha, usuario.getSenha()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        return usuario;
    }

    private boolean vazio(String valor) { return valor == null || valor.isBlank(); }
}
