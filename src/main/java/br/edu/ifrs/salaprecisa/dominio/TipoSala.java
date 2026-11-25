package br.edu.ifrs.salaprecisa.dominio;

/**
 * Tipos de ambiente que podem ser cadastrados no sistema.
 */
public enum TipoSala {

    SALA_REUNIAO("Sala de reunião"),
    AUDITORIO("Auditório"),
    LABORATORIO("Laboratório"),
    OUTRO("Outro");

    private final String descricao;

    TipoSala(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
