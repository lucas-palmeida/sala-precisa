package br.edu.ifrs.salaprecisa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita tarefas agendadas (usadas para encerrar automaticamente as reservas vencidas).
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
