package gateway.controller;

import gateway.model.Dados;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/internal/moderacao")
public class ModeracaoInternaController {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void notificar(@RequestBody Dados.EventoModeracao evento) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("evento", "MODERACAO_RESULTADO");
        resultado.put("tipo", evento.tipo());
        resultado.put("aprovado", evento.aprovado());
        resultado.put("postagemId", evento.postagemId());
        resultado.put("comentarioId", evento.comentario() != null ? evento.comentario().id() : null);
        resultado.put("mensagem", evento.mensagem());
        WsController.enviarParaUsuario(evento.autorId(), resultado);

        if (!evento.aprovado()) return;

        if ("POSTAGEM".equals(evento.tipo()) && evento.postagem() != null) {
            WsController.enviarParaTodos(Map.of(
                    "evento", "NOVA_POSTAGEM",
                    "postagem", evento.postagem()));
        } else if ("COMENTARIO".equals(evento.tipo()) && evento.comentario() != null) {
            WsController.enviarParaTodos(Map.of(
                    "evento", "NOVO_COMENTARIO",
                    "postagemId", evento.postagemId(),
                    "comentario", evento.comentario()));
        }
    }
}
