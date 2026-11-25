package br.edu.ifrs.salaprecisa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adiciona cabeçalhos HTTP de segurança a todas as respostas (correção de achado do
 * teste de segurança, ver docs/teste-seguranca.md).
 *
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}: impede o navegador de "adivinhar" o tipo
 *       do conteúdo, evitando que um texto seja interpretado como script;</li>
 *   <li>{@code X-Frame-Options: DENY}: impede que as páginas sejam exibidas dentro de
 *       frames de outros sites (proteção contra clickjacking);</li>
 *   <li>{@code Content-Security-Policy}: só permite carregar recursos do próprio sistema
 *       e bloqueia scripts embutidos, uma segunda barreira contra XSS;</li>
 *   <li>{@code Referrer-Policy: same-origin}: não envia o endereço das páginas a outros sites.</li>
 * </ul>
 */
@Component
public class CabecalhosSegurancaFilter extends OncePerRequestFilter {

    static final String POLITICA_CONTEUDO = "default-src 'self'; object-src 'none'; base-uri 'self'; "
            + "form-action 'self'; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", POLITICA_CONTEUDO);
        response.setHeader("Referrer-Policy", "same-origin");
        chain.doFilter(request, response);
    }
}
