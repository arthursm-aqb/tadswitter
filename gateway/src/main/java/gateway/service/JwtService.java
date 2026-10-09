package gateway.service;

import gateway.model.Dados;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String CABECALHO = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
    private static final Pattern PAYLOAD = Pattern.compile("\\{\"sub\":\"(\\d+)\",\"nome64\":\"([A-Za-z0-9_-]+)\",\"exp\":(\\d+)\\}");
    private final byte[] segredo;

    public JwtService(@Value("${jwt.secret}") String segredo) {
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
    }
    public String gerar(Dados.Usuario usuario) {
        long expira = Instant.now().plusSeconds(8 * 60 * 60).getEpochSecond();
        String nome64 = ENCODER.encodeToString(usuario.nome().getBytes(StandardCharsets.UTF_8));
        String json = "{\"sub\":\"" + usuario.id() + "\",\"nome64\":\"" + nome64 + "\",\"exp\":" + expira + "}";
        String corpo = CABECALHO + "." + ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return corpo + "." + assinar(corpo);
    }
    public Dados.UsuarioToken validar(String token) {
        try {
            String[] partes = token.split("\\.", -1);
            if (partes.length != 3 || !CABECALHO.equals(partes[0])) throw new IllegalArgumentException();
            String corpo = partes[0] + "." + partes[1];
            if (!java.security.MessageDigest.isEqual(assinar(corpo).getBytes(StandardCharsets.US_ASCII),
                    partes[2].getBytes(StandardCharsets.US_ASCII))) throw new IllegalArgumentException();
            Matcher dados = PAYLOAD.matcher(new String(DECODER.decode(partes[1]), StandardCharsets.UTF_8));
            if (!dados.matches() || Instant.now().getEpochSecond() >= Long.parseLong(dados.group(3)))
                throw new IllegalArgumentException();
            return new Dados.UsuarioToken(Long.parseLong(dados.group(1)),
                    new String(DECODER.decode(dados.group(2)), StandardCharsets.UTF_8));
        } catch (Exception e) { throw new IllegalArgumentException("Token inválido ou expirado"); }
    }
    private String assinar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) { throw new IllegalStateException("Não foi possível assinar o token", e); }
    }
}
