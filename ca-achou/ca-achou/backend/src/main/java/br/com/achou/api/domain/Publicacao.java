package br.com.achou.api.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "publicacoes", indexes = {
        @Index(name = "idx_pub_criado", columnList = "criado_em"),
        @Index(name = "idx_pub_categoria", columnList = "categoria"),
        @Index(name = "idx_pub_autor", columnList = "autor_id")
})
public class Publicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false, length = 60)
    private String titulo;

    @Column(length = 300)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Categoria categoria;

    @Column(length = 40)
    private String subcategoria;

    @Column(name = "local_item", nullable = false, length = 50)
    private String local;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(nullable = false)
    private boolean devolvido;

    @Column(name = "devolvido_em")
    private Instant devolvidoEm;

    @Column(length = 60)
    private String foto;

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public String getSubcategoria() { return subcategoria; }
    public void setSubcategoria(String subcategoria) { this.subcategoria = subcategoria; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public Usuario getAutor() { return autor; }
    public void setAutor(Usuario autor) { this.autor = autor; }
    public Instant getCriadoEm() { return criadoEm; }
    public boolean isDevolvido() { return devolvido; }
    public Instant getDevolvidoEm() { return devolvidoEm; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public void marcarDevolvido() {
        this.devolvido = true;
        this.devolvidoEm = Instant.now();
    }
}
