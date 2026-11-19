package gateway.controller;

import gateway.model.Dados;
import gateway.service.JwtService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

@ServerEndpoint("/ws")
public class WsController {
    private static final Logger log = LoggerFactory.getLogger(WsController.class);
    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();
    private static volatile JwtService jwtService;
    private static volatile ObjectMapper objectMapper;

    public static void configurar(JwtService jwt, ObjectMapper mapper) {
        jwtService = jwt;
        objectMapper = mapper;
    }

    @OnOpen
    public void aoAbrir(Session session) {
        List<String> tokens = session.getRequestParameterMap().get("token");
        String token = (tokens != null && !tokens.isEmpty()) ? tokens.get(0) : null;
        try {
            if (jwtService == null || token == null || token.isBlank()) {
                throw new IllegalArgumentException("Token ausente");
            }
            Dados.UsuarioToken usuario = jwtService.validar(token);
            session.getUserProperties().put("usuario", usuario);
            sessions.add(session);
        } catch (IllegalArgumentException e) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Token inválido ou ausente"));
            } catch (IOException ignored) { }
        }
    }

    @OnClose
    public void aoFechar(Session session) {
        sessions.remove(session);
    }

    @OnError
    public void aoErro(Session session, Throwable erro) {
        sessions.remove(session);
    }

    public static void enviarParaTodos(Object payload) {
        String json = serializar(payload);
        if (json == null) return;
        for (Session sessao : sessions) {
            enviar(sessao, json);
        }
    }

    public static void enviarParaUsuario(Long usuarioId, Object payload) {
        if (usuarioId == null) return;
        String json = serializar(payload);
        if (json == null) return;
        for (Session sessao : sessions) {
            Dados.UsuarioToken usuario = (Dados.UsuarioToken) sessao.getUserProperties().get("usuario");
            if (usuario != null && usuarioId.equals(usuario.id())) {
                enviar(sessao, json);
            }
        }
    }

    private static String serializar(Object payload) {
        if (objectMapper == null) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Falha ao serializar payload: {}", e.getMessage());
            return null;
        }
    }

    private static void enviar(Session sessao, String json) {
        if (!sessao.isOpen()) {
            sessions.remove(sessao);
            return;
        }
        try {
            synchronized (sessao) {
                if (sessao.isOpen()) {
                    sessao.getBasicRemote().sendText(json);
                }
            }
        } catch (IOException e) {
            sessions.remove(sessao);
        }
    }
}
