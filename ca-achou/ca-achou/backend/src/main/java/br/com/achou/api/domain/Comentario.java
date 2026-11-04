package br.com.achou.api.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comentarios", indexes = @Index(name = "idx_com_pub", columnList = "publicacao_id"))
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacao_id", nullable = false)
    private Publicacao publicacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(nullable = false, length = 200)
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Comentario() {}

    public Comentario(Publicacao publicacao, Usuario autor, String texto) {
        this.publicacao = publicacao;
        this.autor = autor;
        this.texto = texto;
    }

    public Long getId() { return id; }
    public Publicacao getPublicacao() { return publicacao; }
    public Usuario getAutor() { return autor; }
    public String getTexto() { return texto; }
    public Instant getCriadoEm() { return criadoEm; }
}
