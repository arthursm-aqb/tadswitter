package gateway.service;

import gateway.model.Dados;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFiltro extends OncePerRequestFilter {
    private final JwtService jwt;
    public JwtFiltro(JwtService jwt) { this.jwt = jwt; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return !caminho.startsWith("/api/") || caminho.startsWith("/api/auth/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        try {
            if (cabecalho == null || !cabecalho.startsWith("Bearer ")) throw new IllegalArgumentException();
            Dados.UsuarioToken usuario = jwt.validar(cabecalho.substring(7));
            request.setAttribute("usuario", usuario);
            chain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido ou ausente");
        }
    }
}
