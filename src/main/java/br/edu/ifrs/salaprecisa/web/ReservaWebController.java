package br.edu.ifrs.salaprecisa.web;

import br.edu.ifrs.salaprecisa.excecao.ConflitoReservaException;
import br.edu.ifrs.salaprecisa.excecao.RecursoNaoEncontradoException;
import br.edu.ifrs.salaprecisa.excecao.RegraNegocioException;
import br.edu.ifrs.salaprecisa.servico.ReservaService;
import br.edu.ifrs.salaprecisa.servico.SalaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Telas de criação, listagem e cancelamento de reservas.
 */
@Controller
public class ReservaWebController {

    private static final String VIEW_FORMULARIO = "reservas/form";
    private static final String REDIRECT_LISTA = "redirect:/reservas";

    private final ReservaService reservaService;
    private final SalaService salaService;

    public ReservaWebController(ReservaService reservaService, SalaService salaService) {
        this.reservaService = reservaService;
        this.salaService = salaService;
    }

    @GetMapping("/reservas")
    public String listar(Model model) {
        model.addAttribute("reservas", reservaService.listar());
        return "reservas/lista";
    }

    @GetMapping("/reservas/nova")
    public String nova(@RequestParam(required = false) Long salaId, Model model) {
        ReservaForm form = new ReservaForm();
        form.setSalaId(salaId);
        model.addAttribute("reservaForm", form);
        return prepararFormulario(model);
    }

    @PostMapping("/reservas")
    public String criar(@Valid @ModelAttribute("reservaForm") ReservaForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return prepararFormulario(model);
        }
        try {
            reservaService.criar(form.getSalaId(), form.getSolicitante(), form.getInicio(), form.getFim());
        } catch (RegraNegocioException | ConflitoReservaException | RecursoNaoEncontradoException ex) {
            model.addAttribute("erro", ex.getMessage());
            return prepararFormulario(model);
        }
        redirect.addFlashAttribute("sucesso", "Reserva registrada com sucesso.");
        return REDIRECT_LISTA;
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            reservaService.cancelar(id);
            redirect.addFlashAttribute("sucesso", "Reserva cancelada.");
        } catch (RegraNegocioException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return REDIRECT_LISTA;
    }

    private String prepararFormulario(Model model) {
        model.addAttribute("salas", salaService.listarAtivas());
        return VIEW_FORMULARIO;
    }
}
