package br.com.achou.api.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_rm", columnNames = "rm"))
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de RM do aluno na Etec (usado como login). */
    @Column(nullable = false, length = 8)
    private String rm;

    @Column(nullable = false, length = 40)
    private String nome;

    @Column(nullable = false, length = 60)
    private String sobrenome;

    /** Usado apenas para contato / recuperação de senha. */
    @Column(nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 60)
    private String curso;

    @Column(nullable = false)
    private int ano;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    /** Nome do arquivo da foto de perfil (ou null). */
    @Column(length = 60)
    private String foto;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    public String nomeCompleto() {
        return nome + " " + sobrenome;
    }

    public String turma() {
        return ano + "º " + curso;
    }

    public Long getId() { return id; }
    public String getRm() { return rm; }
    public void setRm(String rm) { this.rm = rm; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }
    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }
    public Instant getCriadoEm() { return criadoEm; }
}
