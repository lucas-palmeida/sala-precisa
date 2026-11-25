package br.edu.ifrs.salaprecisa.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Ambiente que pode ser reservado (sala de reunião, auditório, laboratório etc.).
 */
@Entity
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSala tipo;

    @Column(nullable = false)
    private int capacidade;

    @Column(nullable = false)
    private boolean ativa = true;

    /** Construtor exigido pela JPA. */
    protected Sala() {
    }

    public Sala(String nome, TipoSala tipo, int capacidade) {
        this.nome = nome;
        this.tipo = tipo;
        this.capacidade = capacidade;
    }

    /** Altera os dados cadastrais da sala. */
    public void atualizar(String novoNome, TipoSala novoTipo, int novaCapacidade) {
        this.nome = novoNome;
        this.tipo = novoTipo;
        this.capacidade = novaCapacidade;
    }

    /** Desativa a sala: ela deixa de aceitar novas reservas. */
    public void desativar() {
        this.ativa = false;
    }

    /** Reativa a sala, permitindo novas reservas. */
    public void ativar() {
        this.ativa = true;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    public int getCapacidade() {
        return capacidade;
    }

    public boolean isAtiva() {
        return ativa;
    }
}
