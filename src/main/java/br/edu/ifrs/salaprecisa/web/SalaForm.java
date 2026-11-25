package br.edu.ifrs.salaprecisa.web;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Objeto de formulário (tela) para cadastro e edição de sala.
 *
 * <p>É intencionalmente uma classe de dados: o Spring MVC preenche os campos pelos
 * setters e o Thymeleaf os lê pelos getters (supressão justificada em docs/analise-estatica.md).</p>
 */
@SuppressWarnings("PMD.DataClass")
public class SalaForm {

    @NotBlank(message = "Informe o nome da sala.")
    @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotNull(message = "Selecione o tipo da sala.")
    private TipoSala tipo;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade deve ser de no mínimo 1.")
    @Max(value = SalaService.CAPACIDADE_MAXIMA, message = "A capacidade deve ser de no máximo 1000.")
    private Integer capacidade;

    public static SalaForm de(Sala sala) {
        SalaForm form = new SalaForm();
        form.setNome(sala.getNome());
        form.setTipo(sala.getTipo());
        form.setCapacidade(sala.getCapacidade());
        return form;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoSala getTipo() {
        return tipo;
    }

    public void setTipo(TipoSala tipo) {
        this.tipo = tipo;
    }

    public Integer getCapacidade() {
        return capacidade;
    }

    public void setCapacidade(Integer capacidade) {
        this.capacidade = capacidade;
    }
}
