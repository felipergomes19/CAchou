package br.com.achou.api.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Foto armazenada no banco (coluna bytea no PostgreSQL). Guardar no banco deixa o servidor sem estado:
 * ele pode ser reiniciado ou recriado na nuvem sem perder imagens (disco de container é descartável).
 */
@Entity
@Table(name = "arquivos")
public class Arquivo {

    /** UUID + extensão (ex.: 3f2a...c1.jpg). É o nome usado na URL /api/arquivos/{nome}. */
    @Id
    @Column(length = 60)
    private String nome;

    /** O tamanho do length acompanha achou.uploads.tamanho-maximo-bytes (5 MB). Sem @Lob: no Postgres @Lob vira "oid". */
    @Column(nullable = false, length = 5 * 1024 * 1024)
    private byte[] dados;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Arquivo() {}

    public Arquivo(String nome, byte[] dados) {
        this.nome = nome;
        this.dados = dados;
    }

    public String getNome() { return nome; }
    public byte[] getDados() { return dados; }
    public Instant getCriadoEm() { return criadoEm; }
}
