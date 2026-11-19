package board.service;

import board.controller.BoardController.ComentarioResposta;
import board.controller.BoardController.PostagemResposta;
import board.model.Comentario;
import board.model.Postagem;
import board.model.StatusModeracao;
import board.repository.ComentarioRepository;
import board.repository.PostagemRepository;
import java.util.List;
import moderacao.Moderar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModeracaoAsyncService {
    private static final Logger log = LoggerFactory.getLogger(ModeracaoAsyncService.class);

    public record EventoModeracao(
            String tipo,
            boolean aprovado,
            Long autorId,
            Long postagemId,
            PostagemResposta postagem,
            ComentarioResposta comentario,
            String mensagem) { }

    private final PostagemRepository postagens;
    private final ComentarioRepository comentarios;
    private final RestClient gateway;

    public ModeracaoAsyncService(
            PostagemRepository postagens,
            ComentarioRepository comentarios,
            @Value("${services.gateway:http://127.0.0.1:9080}") String urlGateway) {
        this.postagens = postagens;
        this.comentarios = comentarios;
        this.gateway = RestClient.builder().baseUrl(urlGateway).build();
    }

    @Async
    public void moderarPostagem(Long postagemId) {
        Postagem postagem = postagens.findById(postagemId).orElse(null);
        if (postagem == null) return;

        try {
            Moderar.mod(postagem.getAutorId(), postagem.getTexto());
            Moderar.mod(postagem.getAutorId(), postagem.getTitulo());
            postagem.setStatus(StatusModeracao.APROVADO);
            postagem = postagens.save(postagem);

            PostagemResposta resposta = new PostagemResposta(
                    postagem.getId(), postagem.getAutorId(), postagem.getAutorNome(),
                    postagem.getTitulo(), postagem.getTexto(), postagem.getCriadoEm(),
                    postagem.getStatus().name(), List.of());

            notificarGateway(new EventoModeracao(
                    "POSTAGEM", true, postagem.getAutorId(), postagem.getId(),
                    resposta, null, "Postagem aprovada e publicada."));
        } catch (ResponseStatusException e) {
            reprovarPostagem(postagem, "Conteúdo ofensivo bloqueado pela moderação.");
        } catch (Exception e) {
            log.warn("Erro ao moderar postagem {}: {}", postagemId, e.getMessage());
            reprovarPostagem(postagem, "Não foi possível concluir a moderação da postagem.");
        }
    }

    @Async
    public void moderarComentario(Long comentarioId) {
        Comentario comentario = comentarios.findById(comentarioId).orElse(null);
        if (comentario == null) return;

        try {
            Moderar.mod(comentario.getAutorId(), comentario.getTexto());
            comentario.setStatus(StatusModeracao.APROVADO);
            comentario = comentarios.save(comentario);

            notificarGateway(new EventoModeracao(
                    "COMENTARIO", true, comentario.getAutorId(), comentario.getPostagemId(),
                    null, new ComentarioResposta(comentario), "Comentário aprovado e publicado."));
        } catch (ResponseStatusException e) {
            reprovarComentario(comentario, "Conteúdo ofensivo bloqueado pela moderação.");
        } catch (Exception e) {
            log.warn("Erro ao moderar comentário {}: {}", comentarioId, e.getMessage());
            reprovarComentario(comentario, "Não foi possível concluir a moderação do comentário.");
        }
    }

    private void reprovarPostagem(Postagem postagem, String mensagem) {
        postagem.setStatus(StatusModeracao.REPROVADO);
        postagens.save(postagem);
        notificarGateway(new EventoModeracao(
                "POSTAGEM", false, postagem.getAutorId(), postagem.getId(),
                null, null, mensagem));
    }

    private void reprovarComentario(Comentario comentario, String mensagem) {
        comentario.setStatus(StatusModeracao.REPROVADO);
        comentarios.save(comentario);
        notificarGateway(new EventoModeracao(
                "COMENTARIO", false, comentario.getAutorId(), comentario.getPostagemId(),
                null, null, mensagem));
    }

    private void notificarGateway(EventoModeracao evento) {
        try {
            gateway.post().uri("/internal/moderacao").body(evento).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Falha ao notificar gateway sobre moderação ({} {}): {}",
                    evento.tipo(), evento.postagemId(), e.getMessage());
        }
    }
}
