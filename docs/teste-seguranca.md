# Teste de Segurança — SalaPrecisa

Este documento registra a avaliação de vulnerabilidades básicas do SalaPrecisa, realizada
como atividade complementar (pontuação extra) do trabalho de Verificação e Validação.
São descritos a metodologia, os casos de teste, os achados com sua severidade e o
tratamento dado a cada um, além das evidências de execução.

## 1. Objetivo e escopo

Avaliar se o sistema resiste a ataques comuns contra aplicações web, nas seguintes
categorias:

| Categoria | O que foi avaliado |
|---|---|
| Injeção (SQL) | Campos de texto e parâmetros de URL com comandos SQL |
| Cross-Site Scripting (XSS) | Conteúdo malicioso armazenado e refletido nas telas |
| Validação de entrada | Tamanhos-limite, *overflow* numérico, corpo vazio, tipos de conteúdo e métodos HTTP |
| Atribuição em massa | Campos internos (id, status, ativa) enviados indevidamente pelo cliente |
| Exposição de informações | Stack traces, mensagens internas, consoles administrativos e identificação do servidor |
| Configuração de segurança | Cabeçalhos HTTP de proteção do navegador |
| Gerenciamento de sessão | Identificador de sessão na URL e atributos do cookie |
| Autenticação e autorização | Análise documental (o sistema não possui controle de acesso, ver achado SEG-05) |

As categorias foram escolhidas com base no **OWASP Top 10:2025**, edição vigente da lista
de riscos mais críticos para aplicações web mantida pela OWASP
(https://top10.owasp.org/2025/). A avaliação abrange as seguintes categorias da lista:

| Categoria OWASP Top 10:2025 | Relação com este teste |
|---|---|
| A01:2025 – *Broken Access Control* (quebra de controle de acesso) | Ausência de autenticação e autorização (SEG-05); ausência de proteção contra CSRF (SEG-06) |
| A02:2025 – *Security Misconfiguration* (configuração incorreta de segurança) | Cabeçalhos HTTP de segurança (SEG-03); respostas de erro sem *stack trace*; consoles administrativos expostos |
| A05:2025 – *Injection* (injeção) | Injeção de SQL e *Cross-Site Scripting* (XSS), que a OWASP inclui nesta categoria |
| A07:2025 – *Authentication Failures* (falhas de autenticação) | Gerenciamento de sessão: identificador na URL (SEG-01) e atributos do *cookie* (SEG-02, SEG-07) |
| A10:2025 – *Mishandling of Exceptional Conditions* (tratamento inadequado de condições excepcionais) | Tratamento de erros e exceções (SEG-04, SEG-08) |

Os nomes oficiais das categorias são em inglês; as traduções entre parênteses são livres.

**Fora do escopo:** varredura automatizada com ferramentas especializadas (como o OWASP ZAP),
análise de vulnerabilidades em dependências (como o OWASP Dependency-Check), testes de
negação de serviço e testes de infraestrutura (TLS, sistema operacional, rede).

## 2. Metodologia

Os testes de segurança foram escritos como **testes automatizados que descrevem o
comportamento seguro esperado**. Um teste que falha indica uma vulnerabilidade. O
processo seguiu três passos:

1. **Antes:** os testes foram escritos e executados sobre o sistema sem nenhuma correção,
   e os resultados foram registrados;
2. **Correção:** cada falha foi analisada e corrigida na aplicação;
3. **Depois:** os testes foram executados novamente, todos com sucesso, e passaram a
   integrar o build como **testes de regressão de segurança**.

| Classe de teste | Ferramenta | Execução | Testes |
|---|---|---|---|
| `seguranca/SegurancaApiIT` | JUnit 5 + RestAssured (requisições HTTP à aplicação real) | `mvnw verify` | 26 |
| `seguranca/SegurancaWebIT` | JUnit 5 + RestAssured (inspeção do HTML e dos cabeçalhos) | `mvnw verify` | 9 |
| `seguranca/XssNavegadorSistemaIT` | JUnit 5 + Selenium (Microsoft Edge real) | `mvnw verify -Psistema` | 1 |
| Inspeção manual | `curl` | linha de comando | — |

Os testes com navegador verificam algo que a simples inspeção do HTML não garante:
**se o código malicioso chega a ser executado**. Cada *payload* tenta definir a variável
JavaScript `window.xss`, e o teste confirma que ela continua indefinida e que nenhum
alerta foi aberto.

## 3. Casos de teste e resultados

Legenda: ✅ comportamento seguro; ❌ vulnerabilidade encontrada.

| ID | Categoria | Caso de teste | Antes | Depois |
|---|---|---|---|---|
| CT-S01 | Injeção SQL | 5 *payloads* (`'; DROP TABLE sala; --`, `' OR '1'='1`, `UNION SELECT` etc.) no nome da sala são gravados como texto literal e as tabelas permanecem íntegras | ✅ | ✅ |
| CT-S02 | Injeção SQL | A verificação de nome duplicado não é manipulável por `x' OR '1'='1` | ✅ | ✅ |
| CT-S03 | Injeção SQL | 3 *payloads* no id da URL (`1 OR 1=1` etc.) são rejeitados com 400 | ✅ | ✅ |
| CT-S04 | Injeção SQL | `UPDATE ... SET status` no solicitante é gravado literalmente e não altera dados | ✅ | ✅ |
| CT-S05 | Validação | Nome com 100 caracteres é aceito e com 101 é rejeitado (valor-limite) | ✅ | ✅ |
| CT-S06 | Validação | Solicitante com 10.000 caracteres é rejeitado e nada é gravado | ✅ | ✅ |
| CT-S07 | Validação | Capacidade `99999999999` (*overflow* de `int`) é rejeitada sem detalhes internos | ✅ | ✅ |
| CT-S08 | Validação | Ids `-1` e `99999999999999999999` não provocam erro interno | ✅ | ✅ |
| CT-S09 | Validação | Corpo vazio é rejeitado com 400 | ✅ | ✅ |
| CT-S10 | Validação | `Content-Type: text/plain` é rejeitado com **415**, sem detalhes internos | ❌ | ✅ |
| CT-S11 | Validação | Método não suportado (`DELETE`) é rejeitado com 405 | ✅ | ✅ |
| CT-S12 | Atribuição em massa | `id` e `ativa` enviados no cadastro de sala são ignorados | ✅ | ✅ |
| CT-S13 | Atribuição em massa | `id` e `status` enviados na criação de reserva são ignorados | ✅ | ✅ |
| CT-S14 | Exposição | JSON malformado não devolve detalhes do *parser* (linha e coluna) | ✅ | ✅ |
| CT-S15 | Exposição | Rota inexistente devolve 404 sem *stack trace* nem exceção | ✅ | ✅ |
| CT-S16 | Exposição | `/h2-console`, `/h2-console/login.do`, `/actuator` e `/actuator/env` estão inacessíveis (404) | ✅ | ✅ |
| CT-S17 | Exposição | As respostas não trazem os cabeçalhos `Server` nem `X-Powered-By` | ✅ | ✅ |
| CT-S18 | XSS | XSS armazenado: `<script>` no nome da sala aparece escapado no HTML | ✅ | ✅ |
| CT-S19 | XSS | XSS armazenado: `<img onerror>` no solicitante aparece escapado no HTML | ✅ | ✅ |
| CT-S20 | XSS | XSS refletido: valor malicioso reexibido no formulário com erro aparece escapado | ✅ | ✅ |
| CT-S21 | XSS | No navegador, os *payloads* `<script>`, `<img onerror>` e `<svg onload>` não executam | ✅ | ✅ |
| CT-S22 | Configuração | As respostas de 4 tipos de recurso (telas, formulário, API e CSS) incluem `X-Content-Type-Options`, `X-Frame-Options`, `Content-Security-Policy` e `Referrer-Policy` | ❌ | ✅ |
| CT-S23 | Sessão | O redirecionamento após o cadastro não expõe o `jsessionid` na URL | ❌ | ✅ |
| CT-S24 | Sessão | O cookie de sessão tem os atributos `HttpOnly` e `SameSite=Lax` | ❌ | ✅ |

**Totais:** 36 execuções de teste, contando cada parâmetro dos testes parametrizados.
- **Antes das correções:** 29 aprovadas e 7 reprovadas (CT-S10, os 4 recursos de CT-S22, CT-S23 e CT-S24).
- **Depois das correções:** 36 aprovadas.

## 4. Achados

| ID | Achado | Severidade | Situação |
|---|---|---|---|
| SEG-01 | Identificador de sessão exposto na URL | Média | **Corrigido** |
| SEG-02 | Cookie de sessão sem o atributo `SameSite` | Baixa | **Corrigido** |
| SEG-03 | Ausência de cabeçalhos HTTP de segurança | Média | **Corrigido** |
| SEG-04 | `Content-Type` não suportado tratado como erro interno (500) | Baixa | **Corrigido** |
| SEG-05 | Ausência de autenticação e autorização | Alta (em produção) | **Risco aceito** (fora do escopo) |
| SEG-06 | Formulários sem *token* anti-CSRF | Baixa no contexto atual | **Mitigado parcialmente** / risco aceito |
| SEG-07 | Cookie sem o atributo `Secure` e tráfego sem HTTPS | Média (em produção) | **Recomendação** para implantação |
| SEG-08 | Parâmetro com codificação inválida gera *stack trace* no log | Baixa | **Registrado** (não corrigido) |

A severidade foi atribuída qualitativamente, considerando o impacto e a facilidade de
exploração no contexto do sistema. Não foi usada uma métrica formal como o CVSS.

### SEG-01 — Identificador de sessão exposto na URL

- **Descrição:** após o envio de um formulário, o sistema redirecionava para
  `http://localhost:8080/salas;jsessionid=EEB3004D...`. O servidor Tomcat inclui o
  identificador de sessão na URL quando ainda não sabe se o navegador aceita *cookies*.
  O sistema cria uma sessão para exibir as mensagens de sucesso após o redirecionamento
  (*flash attributes*).
- **Risco:** o identificador fica registrado no histórico do navegador e em *logs* de
  servidores e *proxies*, e pode ser enviado a outros sites pelo cabeçalho `Referer`,
  permitindo o sequestro de sessão (*session hijacking*). O impacto atual é limitado,
  porque o sistema não tem login, mas se tornaria grave com a introdução de autenticação.
- **Como foi detectado:** observado manualmente na Etapa 2 (teste de fumaça com `curl`) e
  confirmado pelo teste automatizado CT-S23.
- **Evidência (antes):** `Location: http://localhost:49996/salas;jsessionid=62D4ED40D4921D069370A79C353A2BCF`
  (`docs/relatorios/teste-seguranca-antes/`).
- **Correção:** `server.servlet.session.tracking-modes=cookie` no `application.properties`.
  A sessão passa a trafegar exclusivamente por *cookie*.
- **Evidência (depois):** `Location: http://localhost:8089/salas` (`docs/evidencias/seguranca/curl-depois.txt`).

### SEG-02 — Cookie de sessão sem o atributo `SameSite`

- **Descrição:** o *cookie* era emitido como `JSESSIONID=...; Path=/; HttpOnly`, ou seja,
  sem o atributo `SameSite`.
- **Risco:** sem `SameSite`, o comportamento fica a critério de cada navegador quanto ao
  envio do *cookie* em requisições originadas de outros sites, o que facilita ataques de
  CSRF.
- **Correção:** `server.servlet.session.cookie.same-site=lax`. Também foi declarado
  explicitamente `server.servlet.session.cookie.http-only=true`, que já era o padrão.
- **Evidência (depois):** `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax`.

### SEG-03 — Ausência de cabeçalhos HTTP de segurança

- **Descrição:** nenhuma resposta (telas, API e arquivos estáticos) incluía cabeçalhos de
  proteção do navegador.
- **Riscos:**
  - sem `X-Frame-Options` ou `frame-ancestors`, as telas podem ser carregadas dentro de um
    *frame* invisível em outro site (*clickjacking*);
  - sem `X-Content-Type-Options`, o navegador pode interpretar um conteúdo como de outro
    tipo (*MIME sniffing*);
  - sem `Content-Security-Policy`, não há uma segunda barreira caso uma falha de XSS seja
    introduzida no futuro.
- **Correção:** foi criado o filtro `config/CabecalhosSegurancaFilter`, que adiciona a
  todas as respostas:

  | Cabeçalho | Valor |
  |---|---|
  | `X-Content-Type-Options` | `nosniff` |
  | `X-Frame-Options` | `DENY` |
  | `Content-Security-Policy` | `default-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'` |
  | `Referrer-Policy` | `same-origin` |

  A política de conteúdo foi viável sem ajustes nas telas porque elas não usam *scripts*
  nem estilos embutidos. Toda a estilização está em `/css/estilo.css`. Os 15 testes de
  sistema (Selenium) continuaram passando após a inclusão da política.

### SEG-04 — `Content-Type` não suportado tratado como erro interno

- **Descrição:** uma requisição `POST /api/salas` com `Content-Type: text/plain` era
  capturada pelo tratador genérico de exceções da API. O resultado era **HTTP 500** ("Erro
  interno") em vez de **415 Unsupported Media Type**, e um *stack trace* completo era
  gravado no log com nível `ERROR`.
- **Risco:**
  - informa incorretamente ao cliente que houve falha no servidor;
  - permite que qualquer pessoa **inunde o log** com *stack traces* (49 linhas por requisição
    na execução registrada em `docs/relatorios/teste-seguranca-antes/execucao.log`),
    dificultando o monitoramento e consumindo disco.
- **Como foi detectado:** pelo teste CT-S10 (`Expected status code <415> but was <500>`).
  Este achado não havia sido previsto no planejamento.
- **Correção:** em `api/TratadorErrosApi`, as exceções do próprio Spring MVC que já trazem
  o status HTTP adequado (interface `ErrorResponse`, por exemplo 415) passaram a ser
  respondidas com esse status, sem registro de *stack trace*.
- **Evidência (depois):** `HTTP/1.1 415` com o corpo `{"status":415,"mensagem":"Requisição não suportada.","detalhes":[]}`.

### SEG-05 — Ausência de autenticação e autorização

- **Descrição:** o sistema não possui login. Qualquer pessoa com acesso à rede pode
  cadastrar, alterar e desativar salas, além de criar e **cancelar reservas de terceiros**.
  O teste de componente `ReservaApiIT.cancelaELiberaHorario`, por exemplo, cancela uma
  reserva sem apresentar nenhuma credencial.
- **Risco:** em um ambiente real, qualquer usuário poderia sabotar as reservas de outros.
- **Situação:** **risco aceito**. Autenticação e autorização não fazem parte do escopo
  definido na Etapa 1. O sistema foi concebido como protótipo para uso em rede interna.
- **Recomendação:** antes de uma implantação real, adotar o Spring Security com
  autenticação de usuários e regras de autorização. Por exemplo, apenas o próprio
  solicitante ou um administrador poderia cancelar uma reserva, e apenas administradores
  poderiam gerenciar salas.

### SEG-06 — Formulários sem *token* anti-CSRF

- **Descrição:** os formulários não incluem *token* de proteção contra *Cross-Site Request
  Forgery*.
- **Análise:** o CSRF explora a sessão autenticada da vítima. Como o sistema não tem
  autenticação (SEG-05), um atacante não ganha nada induzindo a vítima a enviar um
  formulário, pois poderia enviá-lo diretamente. Além disso, o `SameSite=Lax` (SEG-02)
  impede o envio do *cookie* em requisições `POST` originadas de outros sites.
- **Situação:** mitigado parcialmente e aceito. Deve ser tratado junto com o SEG-05, já que
  o Spring Security inclui proteção CSRF por padrão.

### SEG-07 — Cookie sem `Secure` e tráfego sem HTTPS

- **Descrição:** a aplicação é servida por HTTP, e o *cookie* de sessão não tem o atributo
  `Secure`.
- **Situação:** **recomendação** para implantação. Em produção, o sistema deve ser servido
  exclusivamente por HTTPS (diretamente ou por um *proxy* reverso), com
  `server.servlet.session.cookie.secure=true` e o cabeçalho `Strict-Transport-Security`.
  Esses ajustes não foram aplicados porque impediriam a execução local por HTTP, usada no
  desenvolvimento e nos testes.

### SEG-08 — Parâmetro com codificação inválida gera *stack trace* no log

- **Descrição:** observado na Etapa 2. Um formulário enviado com caracteres em uma
  codificação diferente de UTF-8 (`Audit<byte 0xF3>rio`) é rejeitado pelo Tomcat com HTTP
  400, o que está correto, mas o servidor registra um *stack trace* com nível `ERROR`.
- **Risco:** baixo. A resposta ao cliente não expõe detalhes, mas o log pode ser poluído de
  forma semelhante ao SEG-04.
- **Situação:** registrado e não corrigido. O tratamento ocorre no próprio Tomcat, antes de
  a requisição chegar à aplicação. A correção exigiria configuração do nível de log ou um
  tratamento específico no servidor, o que foi considerado desproporcional para o escopo.

## 5. Controles que se mostraram adequados

Os testes confirmaram que as seguintes proteções já estavam presentes desde a
implementação:

- **Injeção de SQL:** todas as consultas usam Spring Data JPA, com consultas derivadas ou
  JPQL com parâmetros nomeados (`:salaId`, `:inicio`), nunca concatenação de texto. Os
  *payloads* foram gravados literalmente e o banco permaneceu íntegro.
- **XSS:** o Thymeleaf, com `th:text`, `th:attr` e `th:field`, escapa automaticamente os
  caracteres especiais do HTML. O teste com navegador confirmou que nenhum *payload* foi
  executado.
- **Validação de entrada:** a Bean Validation (`@NotBlank`, `@Size`, `@Min`, `@Max`) nos
  DTOs e formulários, somada às validações de regra de negócio nos serviços, garante
  defesa em profundidade.
- **Atribuição em massa:** os DTOs (`SalaRequest`, `ReservaRequest`) declaram apenas os
  campos que o cliente pode informar, de modo que `id`, `status` e `ativa` são ignorados.
- **Exposição de informações:**
  - `server.error.include-stacktrace=never` e `server.error.include-message=never`;
  - tratador de erros da API com mensagens próprias;
  - console do H2 removido do projeto (Etapa 1);
  - Spring Boot Actuator não incluído.

## 6. Evidências

| Evidência | Local |
|---|---|
| Resultados **antes** das correções (relatórios e log da execução) | `docs/relatorios/teste-seguranca-antes/` |
| Resultados **depois** das correções | `docs/relatorios/teste-seguranca-depois/` |
| Capturas de tela do teste de XSS no navegador | `docs/evidencias/seguranca/XssNavegadorSistemaIT-*.png` |
| Inspeção manual com `curl` após as correções | `docs/evidencias/seguranca/curl-depois.txt` |
| Código dos testes | `src/test/java/br/edu/ifrs/salaprecisa/seguranca/` |
| Código das correções | `config/CabecalhosSegurancaFilter.java`, `api/TratadorErrosApi.java`, `application.properties` |

Observação: na primeira execução, o teste `XssNavegadorSistemaIT` falhou por um defeito do
**próprio teste**, e não do sistema. O seletor XPath usado para localizar a reserva era
quebrado pelas aspas contidas no *payload*. O teste foi ajustado para verificar o texto da
tabela, e a execução registrada como "antes" já contém essa versão corrigida. A captura de
tela gerada automaticamente nessa falha do teste está em
`docs/relatorios/teste-seguranca-antes/FALHA-XssNavegadorSistemaIT-xssArmazenadoNaoExecuta.png`.

## 7. Como reproduzir

```bash
# Testes de segurança via HTTP (incluídos no build padrão)
./mvnw verify

# Apenas os testes de segurança, incluindo o teste com navegador
./mvnw -Psistema verify "-Dit.test=Seguranca*IT,XssNavegador*IT"
```

No Windows, utilize `.\mvnw.cmd` (PowerShell) ou `mvnw.cmd` (Prompt de Comando) no lugar de `./mvnw`.

## 8. Limitações e trabalhos futuros

- Não foi utilizada uma ferramenta de varredura dinâmica (por exemplo, OWASP ZAP), que
  poderia identificar classes de vulnerabilidade não cobertas pelos casos de teste
  escritos manualmente.
- Não foi feita análise de vulnerabilidades conhecidas nas dependências (por exemplo,
  OWASP Dependency-Check ou `mvn dependency:tree` cruzado com bases de CVE).
- Os testes de concorrência ficaram fora do escopo. Duas requisições simultâneas para o
  mesmo horário podem, em tese, passar ambas pela verificação de conflito antes de
  qualquer uma ser gravada, pois a verificação e a inserção não são atômicas. Uma possível
  mitigação seria o bloqueio pessimista da sala durante a criação da reserva.
