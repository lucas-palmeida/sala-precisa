package br.edu.ifrs.salaprecisa.api;

import java.util.List;

/**
 * Corpo padronizado das respostas de erro da API.
 *
 * @param status   código HTTP
 * @param mensagem descrição do erro para o usuário
 * @param detalhes lista de problemas de validação por campo (pode ser vazia)
 */
public record ErroResponse(int status, String mensagem, List<String> detalhes) {
}
