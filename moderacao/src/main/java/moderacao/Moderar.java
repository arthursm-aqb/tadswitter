package moderacao;

import java.math.BigInteger;
import moderacao.wsdl.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class Moderar {
    private static final ObjectFactory factory = new ObjectFactory();
    private static final Application servico = new ModerarService().getApplication();

    public static void mod(String conteudo) {
        ConteudoCadastro req = factory.createConteudoCadastro();
        req.setConteudo(conteudo);

        RespostaCadastro resp = servico.checkCadastro(req);
        if (resp.getFlag() != null && Boolean.TRUE.equals(resp.getFlag().getValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo ofensivo detectado pela moderação");
        }
    }

    public static void mod(Long id, String conteudo) {
        ConteudoForum req = factory.createConteudoForum();
        req.setId(factory.createConteudoForumId(BigInteger.valueOf(id)));
        req.setConteudo(conteudo);

        RespostaForum resp = servico.checkForum(req);
        if (resp.getFlag() != null && Boolean.TRUE.equals(resp.getFlag().getValue())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo ofensivo detectado pela moderação");
        }
    }
}
