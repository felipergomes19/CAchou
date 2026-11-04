package br.com.achou.api.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Código de 6 dígitos enviado por e-mail para redefinir a senha (guardado apenas como hash). */
@Entity
@Table(name = "codigos_recuperacao")
public class CodigoRecuperacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private int tentativas;

    @Column(nullable = false)
    private boolean usado;

    protected CodigoRecuperacao() {}

    public CodigoRecuperacao(Usuario usuario, String codigoHash, Instant expiraEm) {
        this.usuario = usuario;
        this.codigoHash = codigoHash;
        this.expiraEm = expiraEm;
    }

    public Usuario getUsuario() { return usuario; }
    public String getCodigoHash() { return codigoHash; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public int getTentativas() { return tentativas; }
    public boolean isUsado() { return usado; }
    public void registrarTentativa() { this.tentativas++; }
    public void marcarUsado() { this.usado = true; }
}
