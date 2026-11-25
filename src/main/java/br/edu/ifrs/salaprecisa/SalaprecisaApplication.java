package br.edu.ifrs.salaprecisa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicação SalaPrecisa.
 *
 * <p>As supressões abaixo são justificadas: o Spring instancia esta classe como bean de
 * configuração, portanto ela não pode ter construtor privado, embora só tenha o método
 * estático {@code main} (ver docs/analise-estatica.md).</p>
 */
@SpringBootApplication
@SuppressWarnings({"PMD.UseUtilityClass", "checkstyle:HideUtilityClassConstructor"})
public class SalaprecisaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalaprecisaApplication.class, args);
    }

}
