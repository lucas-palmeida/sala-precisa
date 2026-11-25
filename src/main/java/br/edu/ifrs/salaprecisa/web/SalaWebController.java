package br.edu.ifrs.salaprecisa.web;

import br.edu.ifrs.salaprecisa.dominio.Sala;
import br.edu.ifrs.salaprecisa.dominio.TipoSala;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de cadastro e listagem de salas.
 */
@Controller
public class SalaWebController {

    private static final String URL_SALAS = "/salas";
    private static final String VIEW_FORMULARIO = "salas/form";
    private static final String REDIRECT_LISTA = "redirect:" + URL_SALAS;
    private static final String ATRIBUTO_FORM = "salaForm";
    private static final String ATRIBUTO_SUCESSO = "sucesso";
    private static final String ATRIBUTO_ERRO = "erro";
    private static final String TITULO_NOVA = "Nova sala";
    private static final String TITULO_EDITAR = "Editar sala";

    private final SalaService salaService;

    public SalaWebController(SalaService salaService) {
        this.salaService = salaService;
    }

    @GetMapping("/")
    public String inicio() {
        return REDIRECT_LISTA;
    }

    @GetMapping(URL_SALAS)
    public String listar(Model model) {
        model.addAttribute("salas", salaService.listar());
        model.addAttribute("idsOcupadas", salaService.idsSalasOcupadasAgora());
        return "salas/lista";
    }

    @GetMapping(URL_SALAS + "/nova")
    public String nova(Model model) {
        model.addAttribute(ATRIBUTO_FORM, new SalaForm());
        return prepararFormulario(model, TITULO_NOVA, URL_SALAS);
    }

    @PostMapping(URL_SALAS)
    public String cadastrar(@Valid @ModelAttribute(ATRIBUTO_FORM) SalaForm form, BindingResult resultado,
                            Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, TITULO_NOVA, URL_SALAS);
        }
        try {
            salaService.cadastrar(form.getNome(), form.getTipo(), form.getCapacidade());
        } catch (RegraNegocioException ex) {
            model.addAttribute(ATRIBUTO_ERRO, ex.getMessage());
            return prepararFormulario(model, TITULO_NOVA, URL_SALAS);
        }
        redirect.addFlashAttribute(ATRIBUTO_SUCESSO, "Sala cadastrada com sucesso.");
        return REDIRECT_LISTA;
    }

    @GetMapping(URL_SALAS + "/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Sala sala = salaService.buscarPorId(id);
        model.addAttribute(ATRIBUTO_FORM, SalaForm.de(sala));
        return prepararFormulario(model, TITULO_EDITAR, urlDaSala(id));
    }

    @PostMapping(URL_SALAS + "/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute(ATRIBUTO_FORM) SalaForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model, TITULO_EDITAR, urlDaSala(id));
        }
        try {
            salaService.atualizar(id, form.getNome(), form.getTipo(), form.getCapacidade());
        } catch (RegraNegocioException ex) {
            model.addAttribute(ATRIBUTO_ERRO, ex.getMessage());
            return prepararFormulario(model, TITULO_EDITAR, urlDaSala(id));
        }
        redirect.addFlashAttribute(ATRIBUTO_SUCESSO, "Sala atualizada com sucesso.");
        return REDIRECT_LISTA;
    }

    @PostMapping(URL_SALAS + "/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        salaService.desativar(id);
        redirect.addFlashAttribute(ATRIBUTO_SUCESSO, "Sala desativada.");
        return REDIRECT_LISTA;
    }

    @PostMapping(URL_SALAS + "/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        salaService.ativar(id);
        redirect.addFlashAttribute(ATRIBUTO_SUCESSO, "Sala reativada.");
        return REDIRECT_LISTA;
    }

    private static String urlDaSala(Long id) {
        return URL_SALAS + "/" + id;
    }

    private String prepararFormulario(Model model, String titulo, String acao) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("acao", acao);
        model.addAttribute("tipos", TipoSala.values());
        return VIEW_FORMULARIO;
    }
}
