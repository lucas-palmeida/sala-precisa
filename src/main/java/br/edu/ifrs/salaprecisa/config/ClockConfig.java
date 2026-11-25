package br.edu.ifrs.salaprecisa.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disponibiliza o relógio do sistema como um bean.
 *
 * <p>Os serviços obtêm a data/hora atual a partir deste {@link Clock} em vez de
 * chamar {@code LocalDateTime.now()} diretamente. Isso permite que os testes
 * substituam o relógio por um relógio fixo (ou por um mock) e verifiquem as regras
 * dependentes de tempo de forma determinística.</p>
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
