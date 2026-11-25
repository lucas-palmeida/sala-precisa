# Plano de Teste — SalaPrecisa

| | |
|---|---|
| **Sistema** | SalaPrecisa — gerenciamento de reservas de salas |
| **Disciplina** | Verificação e Validação de Sistemas — Tecnologia em Sistemas para Internet (IFRS) |
| **Autor** | Lucas Pacheco |
| **Versão do documento** | 1.0 — 22/11/2026 |

## 1. Introdução

### 1.1 Objetivo

Este documento define o planejamento das atividades de verificação e validação (V&V) do
sistema SalaPrecisa: o que será testado, com quais técnicas e ferramentas, em que
condições os testes começam e terminam, quais riscos podem comprometer a atividade e como
os resultados são registrados. O plano também registra a execução realizada e seus
resultados (seção 11), servindo como relatório final dos testes.

### 1.2 Descrição do sistema

O SalaPrecisa é uma aplicação web para gerenciamento de reservas de salas (salas de
reunião, auditórios, laboratórios e outros ambientes). O sistema permite cadastrar salas,
registrar reservas associadas a um solicitante e a um período de uso, impede que uma sala
seja reservada novamente em um período já ocupado e encerra automaticamente as reservas
ao final do período, liberando a sala.

O sistema oferece duas interfaces sobre as mesmas regras de negócio:

- **Interface web** (telas HTML geradas com Thymeleaf), destinada ao usuário final;
- **API REST** (JSON), em `/api/salas` e `/api/reservas`.

Arquitetura em camadas:

```
Telas (web) ─┐
             ├─► Serviços (regras de negócio) ─► Repositórios (Spring Data JPA) ─► H2
API (api)  ──┘          ▲
                        └── Tarefa agendada de encerramento (a cada 60 s)
```

### 1.3 Requisitos sob teste

| ID | Requisito |
|---|---|
| **RF01** | Cadastrar sala com nome, tipo (sala de reunião, auditório, laboratório ou outro) e capacidade |
| **RF02** | Listar e consultar salas, indicando a situação atual (Livre, Ocupada ou Inativa) |
| **RF03** | Editar, desativar e reativar sala. Sala inativa não aceita reservas |
| **RF04** | Registrar reserva de uma sala para um solicitante em um período (início e fim) |
| **RF05** | Impedir reserva cujo período se sobreponha a outra reserva ativa da mesma sala. Períodos adjacentes (um termina quando o outro começa) são permitidos |
| **RF06** | Cancelar reserva ativa, liberando o período |
| **RF07** | Encerrar automaticamente as reservas cujo período terminou, liberando a sala |
| **RN01** | A data/hora de fim deve ser posterior à de início |
| **RN02** | A reserva não pode começar no passado |
| **RN03** | A duração máxima de uma reserva é de 8 horas |
| **RN04** | O solicitante é obrigatório (até 100 caracteres) |
| **RN05** | O nome da sala é obrigatório (até 100 caracteres) e único, sem diferenciar maiúsculas de minúsculas |
| **RN06** | A capacidade da sala deve estar entre 1 e 1000 |
| **RNF01** | O sistema deve resistir a ataques básicos (injeção, XSS) e não expor informações internas |

## 2. Escopo

### 2.1 O que será testado

| Item | Níveis de teste |
|---|---|
| Entidades de domínio (`Sala`, `Reserva`) e suas regras (sobreposição de períodos, transições de status) | Unitário |
| Serviços (`SalaService`, `ReservaService`) e tarefa agendada (`EncerramentoReservasJob`) | Unitário |
| Repositórios e consultas JPQL contra o banco de dados | Componente |
| API REST (status HTTP, corpo JSON, validações, tratamento de erros) | Componente |
| Camada web (controllers, templates Thymeleaf, formulários e mensagens) | Componente |
| Fluxos completos do usuário no navegador | Sistema |
| Qualidade do código-fonte (padrões, más práticas, duplicação) | Análise estática |
| Vulnerabilidades básicas (RNF01) | Segurança (extra) |

### 2.2 O que não será testado

| Item | Justificativa |
|---|---|
| Autenticação e autorização | O sistema não possui controle de acesso. Fora do escopo definido na Etapa 1 (ver `docs/teste-seguranca.md`, achado SEG-05) |
| Desempenho e carga | Atividade extra opcional não escolhida para este trabalho |
| Usabilidade e acessibilidade | Exigiriam avaliação com usuários reais. A interface é simples e funcional |
| Compatibilidade entre navegadores | Os testes de sistema rodam no Microsoft Edge. Chrome e Firefox são suportados pelo código de teste, mas não foram executados sistematicamente |
| Concorrência (reservas simultâneas do mesmo horário) | Exigiria infraestrutura de teste específica. O risco está registrado (R11) |
| Banco de dados de produção | O sistema usa H2 em memória, conforme definido na Etapa 1 |
| Código gerado ou de infraestrutura (classe `main`, configuração do Maven) | Sem lógica de negócio |

## 3. Estratégia de teste

A estratégia segue a pirâmide de testes: muitos testes unitários rápidos na base, testes
de componente em quantidade intermediária e poucos testes de sistema, mais lentos, no
topo. A análise estática atua antes da execução, sobre o código-fonte.

```
                 ▲   Sistema (Selenium)            15 testes   lentos, ponta a ponta
                ▲▲▲  Segurança (RestAssured/Selenium) 36 testes
              ▲▲▲▲▲▲ Componente (RestAssured, MockMvc, DataJpaTest)   89 testes
          ▲▲▲▲▲▲▲▲▲▲ Unitário (JUnit + Mockito)    91 testes   rápidos, isolados
   ═══════════════════ Análise estática (PMD, CPD, Checkstyle) — todo o código de produção
```

### 3.1 Análise estática

- **Objetivo:** identificar defeitos, más práticas e desvios de padrão sem executar o
  código.
- **Ferramentas:** PMD (regras de boas práticas, propensão a erros, design e segurança),
  CPD (código duplicado) e Checkstyle (padrão de codificação baseado nas convenções Sun).
- **Integração:** fase `verify` do Maven. O build falha diante de qualquer violação.
- **Tratamento:** cada apontamento é corrigido ou justificado. Detalhes em
  [`analise-estatica.md`](analise-estatica.md).

### 3.2 Teste unitário

- **Objetivo:** verificar cada classe de domínio e de serviço isoladamente.
- **Ferramentas:** JUnit 5 (Jupiter) e Mockito.
- **Isolamento:**
  - os repositórios são substituídos por *mocks* (`@Mock`) e a tarefa agendada recebe o
    serviço por `@InjectMocks`;
  - o relógio do sistema (`java.time.Clock`) é injetado nos serviços e substituído nos
    testes por um relógio fixo (`Clock.fixed`), o que torna determinísticas as regras que
    dependem da data e hora atuais (RN02 e RF07);
  - `ArgumentCaptor` e `verify` conferem o que é gravado, e `verify(..., never())` garante
    que nada é gravado quando uma regra é violada.
- **Cobertura:** medida com JaCoCo. A meta é de pelo menos 80 % de linhas e de ramos nos
  pacotes `dominio` e `servico`, verificada no build.
- **Convenção:** classes `*Test.java`, executadas pelo Maven Surefire na fase `test`.

### 3.3 Teste de componente

- **Objetivo:** verificar componentes maiores que uma classe, de forma isolada do restante
  do sistema.
- **Convenção:** classes `*IT.java`, executadas pelo Maven Failsafe nas fases
  `integration-test` e `verify`.

| Componente | Abordagem | Isolamento |
|---|---|---|
| Persistência | `@DataJpaTest` | Apenas JPA e H2. Sem serviços e sem camada web. Transação desfeita após cada teste |
| API REST | `@SpringBootTest` em porta aleatória + RestAssured | Aplicação completa acessada apenas por HTTP, como um cliente externo. Banco limpo antes de cada teste |
| Camada web | `@WebMvcTest` + MockMvc | Apenas o Spring MVC, o controller e os templates. Serviços substituídos por `@MockitoBean` |

### 3.4 Teste de sistema

- **Objetivo:** validar o sistema do ponto de vista do usuário final, operando as telas em
  um navegador real.
- **Ferramenta:** Selenium WebDriver 4, com o Microsoft Edge em modo *headless*. O driver
  do navegador é obtido automaticamente pelo Selenium Manager.
- **Organização:** padrão **Page Object**, com uma classe por tela. A localização dos
  elementos HTML fica concentrada nessas classes e os testes descrevem ações do usuário.
- **Ambiente:** a aplicação completa é iniciada em porta aleatória.
- **Cenários:** cada teste cobre um fluxo principal ou um fluxo alternativo, como
  conflito, dados inválidos ou sala desativada.
- **Evidências:** captura de tela nos passos relevantes e, automaticamente, em caso de
  falha.
- **Execução:** perfil Maven `sistema` (`mvnw verify -Psistema`), para que o build padrão
  não dependa de navegador.

### 3.5 Teste de segurança (extra)

- **Objetivo:** avaliar vulnerabilidades básicas (RNF01): injeção de SQL, XSS, validação
  de entrada, atribuição em massa, exposição de informações, cabeçalhos HTTP e sessão.
- **Abordagem:** testes automatizados que descrevem o comportamento seguro esperado,
  executados antes e depois das correções.
- **Detalhes:** [`teste-seguranca.md`](teste-seguranca.md).

### 3.6 Técnicas de projeto de casos de teste

| Técnica | Aplicação no projeto |
|---|---|
| **Partição de equivalência** | Entradas divididas em classes válidas e inválidas, como capacidade válida (1–1000) e inválida (≤ 0 e > 1000), ou solicitante preenchido e nulo, vazio ou em branco |
| **Análise de valor-limite** | Sobreposição de períodos em 9 situações (adjacente antes e depois, invasão de 1 minuto, idêntico, contido, contém); duração de 480 min (aceita) e 481 min (rejeitada); início exatamente "agora" e 1 minuto antes; capacidade 0, 1, 1000 e 1001; nome com 100 e 101 caracteres |
| **Teste baseado em requisitos** | Cada requisito da seção 1.3 possui casos em mais de um nível (matriz da seção 8) |
| **Teste de transição de estados** | Status da reserva: ATIVA → CANCELADA, ATIVA → ENCERRADA e transições inválidas (cancelar duas vezes, encerrar reserva cancelada) |
| **Dublês de teste** | *Mocks* (Mockito) para repositórios e serviços; *fake* (relógio fixo) para o tempo |
| **Testes parametrizados** | `@ParameterizedTest` com `@CsvSource`, `@ValueSource` e `@NullAndEmptySource`, aplicando a mesma verificação a vários dados |
| **Teste de regressão** | Todos os testes integram o build e são executados a cada `mvnw verify` |

## 4. Critérios de entrada e saída

### 4.1 Critérios de entrada (quando os testes começam)

1. Requisitos e escopo definidos e validados (Etapa 1 entregue ao professor).
2. Ambiente disponível: JDK 17 instalado e acesso à internet para o *download* das
   dependências pelo Maven Wrapper. Para os testes de sistema, também o Microsoft Edge.
3. Código da etapa compilando sem erros (`mvnw compile`).
4. Build da etapa anterior verde (`mvnw verify` com sucesso), para que cada nova etapa
   parta de uma base estável.

### 4.2 Critérios de saída (quando os testes são considerados concluídos)

| # | Critério | Como é verificado |
|---|---|---|
| CS1 | 100 % dos testes unitários, de componente, de sistema e de segurança aprovados | Relatórios do Surefire e do Failsafe |
| CS2 | Cobertura dos testes unitários ≥ 80 % de linhas e de ramos nos pacotes `dominio` e `servico` | JaCoCo `check`, que interrompe o build |
| CS3 | Cobertura consolidada (unitários + componente) ≥ 80 % de linhas e de ramos do sistema | JaCoCo `check` sobre o relatório consolidado |
| CS4 | Zero violações de PMD, CPD e Checkstyle, com cada supressão justificada por escrito | `mvnw verify` e `analise-estatica.md` |
| CS5 | Todos os requisitos da seção 1.3 com pelo menos um caso de teste em cada nível aplicável | Matriz de rastreabilidade (seção 8) |
| CS6 | Achados de segurança corrigidos ou com risco aceito e documentado | `teste-seguranca.md` |
| CS7 | Evidências de execução registradas no repositório | `docs/relatorios/` e `docs/evidencias/` |

### 4.3 Critérios de suspensão e retomada

Os testes de uma etapa são suspensos quando o build não compila ou quando uma falha de
ambiente impede a execução, por exemplo navegador ou driver indisponível. São retomados
após a correção, com a reexecução completa do build (`mvnw clean verify`).

## 5. Riscos e mitigação

| ID | Risco | Prob. | Impacto | Mitigação |
|---|---|---|---|---|
| R01 | Testes dependentes da data e hora atuais tornam-se não determinísticos ("passam hoje, falham amanhã") | Alta | Alto | `Clock` injetado nos serviços e fixado nos testes unitários. Nos testes de componente e de sistema, datas relativas ("amanhã às 10h") |
| R02 | Testes de sistema instáveis (*flaky*) por problemas de sincronização com o navegador | Média | Médio | Esperas explícitas (`WebDriverWait`) em vez de pausas fixas; espera pela troca de página após cada envio; Page Objects |
| R03 | Build dependente de navegador ou driver, impedindo a execução em outra máquina | Média | Médio | Testes de sistema isolados no perfil `sistema`; driver obtido automaticamente pelo Selenium Manager; navegador configurável (`-Dnavegador`) |
| R04 | Interferência entre testes por estado compartilhado no banco | Média | Alto | Banco limpo antes de cada teste; `@DataJpaTest` desfaz a transação ao final |
| R05 | Tarefa agendada de encerramento alterar dados durante os testes | Baixa | Médio | Reservas de teste sempre futuras; nos cenários de encerramento, a tarefa é acionada explicitamente |
| R06 | Divergência entre a regra de sobreposição do domínio (Java) e a da consulta ao banco (JPQL) | Média | Alto | Os mesmos 9 casos de valor-limite são executados no teste unitário da entidade e no teste de componente do repositório |
| R07 | Incompatibilidade de versões entre ferramentas (Spring Boot 4, Java 17) | Média | Médio | Versões fixadas no `pom.xml` e documentadas (seção 7). Por exemplo, o Checkstyle 12.3.1, porque a série 13 exige Java 21 |
| R08 | Prazo curto para a execução de todas as atividades | Média | Alto | Execução incremental por etapas, cada uma terminando com o build verde; entrega planejada antes do prazo final |
| R09 | Falsos positivos da análise estática gerando correções desnecessárias | Média | Baixo | Ajuste justificado do conjunto de regras; supressões pontuais com `@SuppressWarnings` comentado |
| R10 | Ausência de controle de acesso no produto | Certa | Alto (em produção) | Registrado como achado SEG-05 e risco aceito, com recomendação de Spring Security |
| R11 | Condição de corrida entre reservas simultâneas do mesmo horário | Baixa | Médio | Fora do escopo dos testes. Registrado em `teste-seguranca.md` com sugestão de bloqueio pessimista |

## 6. Cronograma

As atividades das etapas 2 a 7 foram executadas de forma incremental. Cada etapa só foi
iniciada após o build da anterior estar verde (critério de entrada 4).

| # | Atividade | Planejado | Realizado | Situação |
|---|---|---|---|---|
| 1 | Definição do projeto (Etapa 1, PDF) | 25/09/2026 | 25/09/2026 | Concluída |
| 2 | Configuração do ambiente e esqueleto do projeto | 22/11/2026 | 22/11/2026 | Concluída |
| 3 | Implementação do sistema (domínio, serviços, API e telas) | 22/11/2026 | 22/11/2026 | Concluída |
| 4 | Análise estática (PMD, CPD, Checkstyle) e correções | 22/11/2026 | 22/11/2026 | Concluída |
| 5 | Testes unitários e cobertura (JaCoCo) | 22/11/2026 | 22/11/2026 | Concluída |
| 6 | Testes de componente (API, persistência, camada web) | 22/11/2026 | 22/11/2026 | Concluída |
| 7 | Testes de sistema (Selenium) | 22/11/2026 | 22/11/2026 | Concluída |
| 8 | Testes de segurança (extra) e correções | 22/11/2026 | 22/11/2026 | Concluída |
| 9 | Plano de teste e documentação final | 22/11/2026 | 22/11/2026 | Concluída |
| 10 | Revisão do plano e dos documentos pelo autor | 23/11/2026 | | Pendente |
| 11 | Criação do repositório Git remoto e publicação | 24/11/2026 | | Pendente |
| 12 | Verificação final em cópia limpa do repositório (`mvnw clean verify -Psistema`) | 25/11/2026 | | Pendente |
| 13 | Entrega antecipada | 26/11/2026 | | Pendente |
| — | **Prazo final de entrega** | **27/11/2026** | | — |

## 7. Ambiente e ferramentas

### 7.1 Ambiente de execução

| Item | Versão / configuração |
|---|---|
| Sistema operacional | Windows 11 Home (10.0.26300) |
| JDK | Oracle JDK 17.0.21.1 (LTS) |
| Maven | 3.10.1, via Maven Wrapper (`mvnw`). Não precisa ser instalado |
| Navegador (testes de sistema) | Microsoft Edge 158.0.4370.33, em modo *headless* |
| Banco de dados | H2 2.4.240, em memória |

Requisitos de hardware: qualquer computador capaz de executar o JDK 17 e o Microsoft
Edge. O build completo foi executado em poucos minutos em um computador pessoal comum.

### 7.2 Sistema sob teste

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot (Spring Web MVC, Spring Data JPA, Bean Validation) | 4.1.3 (Spring Framework 7.0.11) |
| Hibernate ORM | 7.4.15.Final |
| Thymeleaf | 3.1.7 |
| Tomcat (embarcado) | 11.0.27 |

### 7.3 Ferramentas de teste e qualidade

| Finalidade | Ferramenta | Versão |
|---|---|---|
| Framework de teste | JUnit Jupiter (API do JUnit 5) | 6.0.3 |
| Dublês de teste | Mockito | 5.23.0 |
| Asserções | AssertJ / Hamcrest | 3.27.7 / 3.0 |
| Execução de testes unitários | Maven Surefire | 3.5.6 |
| Execução de testes de componente e sistema | Maven Failsafe | 3.5.6 |
| Testes de API | RestAssured | 6.3.0 |
| Testes da camada web | Spring MockMvc (`@WebMvcTest`) | Spring Framework 7.0.11 |
| Testes de persistência | Spring Boot `@DataJpaTest` | 4.1.3 |
| Testes de sistema | Selenium WebDriver (com Selenium Manager) | 4.43.0 |
| Cobertura de código | JaCoCo | 0.8.16 |
| Análise estática | PMD / CPD | 7.17.0 (plugin 3.28.0) |
| Análise estática | Checkstyle | 12.3.1 (plugin 3.6.0) |
| Inspeção manual de segurança | curl (distribuído com o Git for Windows) | 8.23.0 |

## 8. Matriz de rastreabilidade

Relaciona cada requisito aos testes que o verificam em cada nível. Os nomes referem-se às
classes de teste e, entre parênteses, aos grupos (`@Nested`) ou métodos.

| Req. | Unitário | Componente | Sistema |
|---|---|---|---|
| **RF01** | `SalaTest` (criação); `SalaServiceTest` (Cadastrar) | `SalaApiIT` (POST); `SalaRepositoryIT` (nome único); `SalaWebControllerIT` (Cadastro) | `GerenciarSalasSistemaIT` (cadastraSala) |
| **RF02** | `SalaServiceTest` (Consultas: listagens, salas ocupadas); `ReservaTest` (estaEmAndamento) | `SalaApiIT` (GET, indicaSalaOcupada); `ReservaRepositoryIT` (idsSalasOcupadas); `SalaWebControllerIT` (Listar) | `GerenciarSalasSistemaIT` (cadastraSala); `ReservarSalaSistemaIT` (salaOcupadaELiberadaAutomaticamente) |
| **RF03** | `SalaTest` (atualizar, desativar/ativar); `SalaServiceTest` (Atualizar, DesativarAtivar); `ReservaServiceTest` (rejeitaSalaInativa) | `SalaApiIT` (PUT, PATCH); `ReservaApiIT` (salaInativa); `SalaRepositoryIT` (listarAtivas); `SalaWebControllerIT` (Edicao, DesativarAtivar) | `GerenciarSalasSistemaIT` (editaSala, desativaEReativaSala) |
| **RF04** | `ReservaTest` (criação); `ReservaServiceTest` (Criar) | `ReservaApiIT` (sucesso, validações); `ReservaWebControllerIT` (Criar) | `ReservarSalaSistemaIT` (reservaSala) |
| **RF05** | `ReservaTest` (Sobreposicao: 9 casos); `ReservaServiceTest` (rejeitaConflito) | `ReservaRepositoryIT` (existeReservaSobreposta: 9 casos, outra sala, inativas); `ReservaApiIT` (Conflito: 5 casos, adjacentes, outra sala); `ReservaWebControllerIT` (conflito) | `ReservarSalaSistemaIT` (conflitoDeHorario, horarioAdjacente) |
| **RF06** | `ReservaTest` (Cancelar); `ReservaServiceTest` (Cancelar) | `ReservaApiIT` (Cancelar); `ReservaRepositoryIT` (reservas canceladas não conflitam); `ReservaWebControllerIT` (Cancelar) | `ReservarSalaSistemaIT` (cancelaReservaELiberaHorario) |
| **RF07** | `ReservaTest` (DeveSerEncerrada, Encerrar); `ReservaServiceTest` (EncerrarVencidas); `EncerramentoReservasJobTest` | `ReservaRepositoryIT` (reservasVencidas); `ReservaApiIT` (EncerramentoAutomatico) | `ReservarSalaSistemaIT` (salaOcupadaELiberadaAutomaticamente) |
| **RN01** | `ReservaServiceTest` (rejeitaFimNaoPosterior) | `ReservaApiIT` (fimAntesDoInicio) | `ReservarSalaSistemaIT` (periodosInvalidos) |
| **RN02** | `ReservaServiceTest` (rejeitaInicioNoPassado, aceitaInicioAgora) | `ReservaApiIT` (inicioNoPassado) | `ReservarSalaSistemaIT` (periodosInvalidos) |
| **RN03** | `ReservaServiceTest` (aceitaDuracaoValida, rejeitaDuracaoExcessiva) | `ReservaApiIT` (duracaoMaximaExata, duracaoExcessiva) | `ReservarSalaSistemaIT` (periodosInvalidos) |
| **RN04** | `ReservaServiceTest` (rejeitaSolicitanteEmBranco) | `ReservaApiIT` (camposObrigatorios); `ReservaWebControllerIT` (camposVazios); `SegurancaApiIT` (solicitanteGigante) | `ReservarSalaSistemaIT` (camposObrigatorios) |
| **RN05** | `SalaServiceTest` (rejeitaNomeEmBranco, rejeitaNomeDuplicado, rejeitaNomeDeOutraSala) | `SalaApiIT` (validaCampos, nomeDuplicado, mantemProprioNome); `SalaRepositoryIT` (restricaoDeNomeUnico); `SegurancaApiIT` (limiteDeTamanhoDoNome) | `GerenciarSalasSistemaIT` (camposObrigatorios, nomeDuplicado) |
| **RN06** | `SalaServiceTest` (rejeitaCapacidadeInvalida, aceitaCapacidadeNosLimites) | `SalaApiIT` (capacidadeAcimaDoMaximo); `SalaWebControllerIT` (dadosInvalidos) | `GerenciarSalasSistemaIT` (capacidadeInvalida) |
| **RNF01** | — | `SegurancaApiIT`; `SegurancaWebIT` | `XssNavegadorSistemaIT` |

Todos os requisitos possuem casos de teste em todos os níveis aplicáveis (critério CS5).

## 9. Procedimento de registro de testes e evidências

### 9.1 Organização e nomenclatura

| Tipo | Local | Convenção de nome | Executado por |
|---|---|---|---|
| Unitário | `src/test/java/.../dominio`, `.../servico` | `*Test.java` | Surefire (fase `test`) |
| Componente | `.../api`, `.../repositorio`, `.../web` | `*IT.java` | Failsafe (`integration-test`/`verify`) |
| Sistema | `.../sistema` (Page Objects em `sistema/paginas`) | `*SistemaIT.java` | Failsafe, perfil `sistema` |
| Segurança | `.../seguranca` | `Seguranca*IT.java`, `XssNavegadorSistemaIT.java` | Failsafe |

Cada teste usa `@DisplayName` em português, descrevendo o cenário e o resultado esperado,
o que torna os relatórios legíveis por quem não lê o código.

### 9.2 Registro dos resultados

| Evidência | Local no repositório |
|---|---|
| Resultados dos testes unitários (Surefire, XML e TXT) | `docs/relatorios/testes-unitarios/` |
| Resultados dos testes de componente (Failsafe) | `docs/relatorios/testes-componente/` |
| Resultados dos testes de sistema (Failsafe) | `docs/relatorios/testes-sistema/` |
| Resultados dos testes de segurança, antes e depois das correções | `docs/relatorios/teste-seguranca-antes/` e `docs/relatorios/teste-seguranca-depois/` |
| Cobertura (HTML: abrir o `index.html`) | `docs/relatorios/jacoco-unitarios/`, `jacoco-componente/` e `jacoco-consolidado/` |
| Análise estática, antes e depois das correções | `docs/relatorios/analise-estatica-inicial/` e `docs/relatorios/analise-estatica-final/` |
| Log completo do build com todos os testes | `docs/relatorios/build-verify-sistema.log` |
| Capturas de tela dos testes de sistema | `docs/evidencias/sistema/` |
| Evidências de segurança (capturas e `curl`) | `docs/evidencias/seguranca/` |

Os relatórios em `docs/` são cópias dos gerados em `target/`, que não é versionado.
Qualquer avaliador pode regenerá-los com os comandos da seção 10.

### 9.3 Registro de defeitos

Os defeitos encontrados são registrados no documento da atividade que os revelou, com
descrição, evidência, correção e verificação:

- apontamentos da análise estática: `analise-estatica.md`, seção 4.2;
- vulnerabilidades: `teste-seguranca.md`, seção 4.

Toda correção é verificada pela reexecução do teste que revelou o defeito e de todo o
build (regressão).

## 10. Como executar

```bash
# Build padrão: análise estática + testes unitários + componente + segurança + cobertura
./mvnw clean verify

# Build completo, incluindo os testes de sistema no navegador (Microsoft Edge, headless)
./mvnw clean verify -Psistema

# Testes de sistema com a janela do navegador visível, ou em outro navegador
./mvnw verify -Psistema -Dheadless=false
./mvnw verify -Psistema -Dnavegador=chrome

# Apenas os relatórios de análise estática (target/reports)
./mvnw compile pmd:pmd pmd:cpd checkstyle:checkstyle
```

No Windows, utilize `.\mvnw.cmd` (PowerShell) ou `mvnw.cmd` (Prompt de Comando) no lugar de `./mvnw`. Os relatórios gerados ficam em
`target/site/jacoco*`, `target/surefire-reports`, `target/failsafe-reports` e
`target/reports`.

## 11. Resultados da execução

Execução final em 22/11/2026, com o comando `mvnw clean verify -Psistema`:

| Nível | Testes | Aprovados | Reprovados |
|---|---|---|---|
| Unitário | 91 | 91 | 0 |
| Componente | 89 | 89 | 0 |
| Sistema | 15 | 15 | 0 |
| Segurança | 36 | 36 | 0 |
| **Total** | **231** | **231** | **0** |

| Cobertura (JaCoCo) | Linhas | Ramos |
|---|---|---|
| Unitários: pacotes `dominio` e `servico` | 98,5 % / 100 % | 100 % / 100 % |
| Consolidada: todo o sistema | 98,8 % | 98,4 % |

| Análise estática | Inicial | Final |
|---|---|---|
| PMD | 9 | 0 |
| CPD | 0 | 0 |
| Checkstyle | 3 | 0 |

**Defeitos e achados:**
- **Análise estática:** 12 apontamentos, sendo 6 corrigidos e 6 justificados.
- **Segurança:** 8 achados, sendo 4 vulnerabilidades corrigidas, 3 riscos aceitos ou
  recomendações e 1 registrado.
- **Funcionais:** os testes unitários, de componente e de sistema não revelaram defeitos
  funcionais nas regras de negócio. Os requisitos foram implementados junto com os testes,
  e as regras de borda (adjacência, limites de duração e de capacidade) foram tratadas
  desde a implementação.

**Situação dos critérios de saída:** CS1 a CS7 atendidos. As etapas pendentes do
cronograma (seção 6) referem-se à revisão, à publicação e à entrega.
