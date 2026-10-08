package ifrn.arthur.board.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Postagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long autorId;
    private String autorNome;
    @Column(length = 5000)
    private String texto;
    private Instant criadoEm;

    protected Postagem() { }
    public Postagem(Long autorId, String autorNome, String texto) {
        this.autorId = autorId;
        this.autorNome = autorNome;
        this.texto = texto;
        this.criadoEm = Instant.now();
    }
    public Long getId() { return id; }
    public Long getAutorId() { return autorId; }
    public String getAutorNome() { return autorNome; }
    public String getTexto() { return texto; }
    public Instant getCriadoEm() { return criadoEm; }
}
