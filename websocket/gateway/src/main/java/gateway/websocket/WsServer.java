package gateway.websocket;

import gateway.controller.WsController;
import gateway.service.JwtService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class WsServer {
    private static final Logger log = LoggerFactory.getLogger(WsServer.class);
    private final Server server;
    private final String host;
    private final int port;

    public WsServer(
            JwtService jwt,
            ObjectMapper objectMapper,
            @Value("${websocket.host:0.0.0.0}") String host,
            @Value("${websocket.port:9083}") int port) {
        this.host = host;
        this.port = port;
        WsController.configurar(jwt, objectMapper);
        this.server = new Server(host, port, "", null, WsController.class);
    }

    @PostConstruct
    public void iniciar() {
        try {
            server.start();
            log.info("Servidor WebSocket Tyrus iniciado em ws://{}:{}/ws", host, port);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível iniciar o servidor WebSocket Tyrus", e);
        }
    }

    @PreDestroy
    public void parar() {
        server.stop();
    }
}
