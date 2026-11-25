# Análise Estática — SalaPrecisa

Este documento descreve a configuração, a execução e os resultados da análise estática
aplicada ao código-fonte do SalaPrecisa, bem como o tratamento dado a cada apontamento
(correção ou justificativa).

## 1. Ferramentas e versões

| Ferramenta | Versão | Finalidade |
|---|---|---|
| PMD (via `maven-pmd-plugin` 3.28.0) | 7.17.0 | Detecção de más práticas, código propenso a erros, problemas de design e de segurança |
| CPD (Copy/Paste Detector, parte do PMD) | 7.17.0 | Detecção de código duplicado (mínimo de 50 *tokens*) |
| Checkstyle (via `maven-checkstyle-plugin` 3.6.0) | 12.3.1 | Verificação do padrão de codificação (nomes, formatação, imports, Javadoc) |

O Checkstyle 12.3.1 foi escolhido por ser a versão mais recente compatível com Java 17.
A partir da série 13.x, a ferramenta passou a exigir Java 21. A versão padrão que acompanha
o plugin (9.3) é antiga e não reconhece adequadamente construções recentes da linguagem,
como *records*.

## 2. Integração ao processo de build

As duas ferramentas estão configuradas no `pom.xml` e são executadas automaticamente na
fase `verify` do ciclo de vida do Maven, conforme apresentado na disciplina:

- **PMD**: *goals* `check` (análise de regras) e `cpd-check` (código duplicado);
- **Checkstyle**: *goal* `check`.

Em ambas, `failOnViolation` está habilitado, ou seja, **o build falha se houver qualquer
violação**. No PMD, `failurePriority = 5` faz com que até as violações de menor prioridade
interrompam o build. Assim, nenhum código com apontamentos pendentes passa pelo
`mvnw verify`.

Arquivos de configuração:

- [`pmd-ruleset.xml`](../pmd-ruleset.xml) — conjunto de regras do PMD;
- [`checkstyle.xml`](../checkstyle.xml) — regras do Checkstyle.

### Como reproduzir

```bash
# Build completo: compila, executa os testes e a análise estática (falha se houver violação)
./mvnw clean verify

# Apenas gerar os relatórios HTML (sem interromper o build), em target/reports/
./mvnw compile pmd:pmd pmd:cpd checkstyle:checkstyle
```

No Windows, utilize `.\mvnw.cmd` (PowerShell) ou `mvnw.cmd` (Prompt de Comando) no lugar de `./mvnw`.

## 3. Conjunto de regras

### 3.1 PMD

Foram habilitadas **integralmente** quatro categorias de regras do PMD para Java:

- `bestpractices` — boas práticas;
- `errorprone` — construções propensas a erro;
- `design` — problemas de design (complexidade, acoplamento, classes de dados etc.);
- `security` — problemas de segurança.

Foram excluídas apenas duas regras da categoria `design`:

| Regra excluída | Justificativa |
|---|---|
| `LawOfDemeter` | Gera grande quantidade de falsos positivos em código de mapeamento simples, como `reserva.getSala().getNome()` ao montar objetos de resposta (DTOs). A navegação entre entidades associadas é intrínseca ao modelo JPA e não representa acoplamento indevido neste contexto. |
| `LoosePackageCoupling` | Só funciona se forem configurados os pacotes que se deseja proteger. Sem essa configuração, o próprio PMD descarta a regra e emite um aviso. Não se aplica ao projeto. |

### 3.2 Checkstyle

A configuração parte das convenções da Sun (`sun_checks.xml`, padrão do plugin) com os
seguintes ajustes:

| Ajuste | Justificativa |
|---|---|
| Limite de linha de 120 colunas (padrão: 80) | O limite de 80 colunas é herdado de terminais antigos. Com nomes de classes e métodos em português e as anotações do Spring, 80 colunas forçariam quebras de linha que prejudicam a legibilidade. |
| Javadoc obrigatório apenas em tipos públicos (`MissingJavadocType`) | Exigir Javadoc em todo método e atributo (`JavadocMethod`, `JavadocVariable`) geraria comentários redundantes em getters, setters e construtores triviais. Métodos com regra de negócio relevante foram documentados mesmo assim. |
| Sem `FinalParameters` | Exigir `final` em todo parâmetro aumenta o ruído visual sem benefício prático. O projeto não reatribui parâmetros. |
| Sem `DesignForExtension` | É incompatível com o Spring e o Hibernate, que criam *proxies* por herança das classes de serviço e das entidades. |
| `MagicNumber` ignora anotações | Valores em anotações de validação e mapeamento (`@Size(max = 100)`, `@Column(length = 100)`) são a própria declaração da restrição, e não números "mágicos". |
| `HiddenField` ignora construtores e *setters* | É o padrão idiomático em Java (`this.nome = nome`). |

### 3.3 Escopo da análise

A análise estática é aplicada ao **código de produção** (`src/main/java`), que é o padrão
dos dois plugins. O código de teste não é analisado porque usa intencionalmente valores
literais (datas, capacidades, nomes) e métodos com muitas asserções, o que geraria
apontamentos sem relação com a qualidade do sistema. A qualidade dos testes é assegurada
pela revisão e pela própria execução.

## 4. Resultados

### 4.1 Execução inicial (antes das correções)

Relatórios: [`docs/relatorios/analise-estatica-inicial/`](relatorios/analise-estatica-inicial/)
(`pmd.html`, `cpd.html`, `checkstyle.html`).

| Ferramenta | Apontamentos |
|---|---|
| PMD | 9 |
| CPD | 0 |
| Checkstyle | 3 |
| **Total** | **12** |

### 4.2 Tratamento dos apontamentos

| # | Ferramenta / regra | Local | Descrição | Tratamento |
|---|---|---|---|---|
| 1 | PMD `UseUtilityClass` | `SalaprecisaApplication` | Classe só com métodos estáticos deveria ter construtor privado. | **Justificado** e suprimido com `@SuppressWarnings`. O Spring instancia a classe anotada com `@SpringBootApplication` como bean de configuração, o que é impossível com construtor privado. |
| 2 | Checkstyle `HideUtilityClassConstructor` | `SalaprecisaApplication` | Mesmo apontamento do item 1. | **Justificado** (mesmo motivo do item 1). |
| 3 | Checkstyle `MissingJavadocType` | `SalaprecisaApplication` | Classe pública sem Javadoc. | **Corrigido**: Javadoc adicionado. |
| 4 | Checkstyle `FileTabCharacter` | `SalaprecisaApplication` | Arquivo gerado pelo Spring Initializr com indentação por tabulação. | **Corrigido**: indentação convertida para espaços. |
| 5–6 | PMD `LawOfDemeter` (2×) | `ReservaResponse.de` | Chamada `reserva.getSala().getId()`. | **Justificado**: regra excluída do conjunto (ver seção 3.1). |
| 7 | PMD `CyclomaticComplexity` | `ReservaService.validarPeriodo` | Complexidade ciclomática 10 (limite: 10). | **Corrigido**: método dividido em `validarPeriodo` (datas obrigatórias e ordem) e `validarAntecedenciaEDuracao` (início no passado e duração máxima). |
| 8 | PMD `DataClass` | `ReservaForm` | Classe "somente dados". | **Justificado** e suprimido com `@SuppressWarnings("PMD.DataClass")`. É um *form backing bean*: o Spring MVC preenche os campos pelos *setters* e o Thymeleaf os lê pelos *getters*. A regra de negócio fica nos serviços e entidades. |
| 9 | PMD `DataClass` | `SalaForm` | Mesmo apontamento do item 8. | **Justificado** (mesmo motivo do item 8). |
| 10 | PMD `AvoidDuplicateLiterals` | `SalaWebController` | O literal `"/salas"` aparece 5 vezes. | **Corrigido**: extraído para a constante `URL_SALAS`. |
| 11 | PMD `AvoidDuplicateLiterals` | `SalaWebController` | O literal `"salaForm"` aparece 4 vezes. | **Corrigido**: constante `ATRIBUTO_FORM`. |
| 12 | PMD `AvoidDuplicateLiterals` | `SalaWebController` | O literal `"sucesso"` aparece 4 vezes. | **Corrigido**: constante `ATRIBUTO_SUCESSO`. Outras repetições, como `"erro"` e os títulos, foram extraídas preventivamente. |

**Resumo:** dos 12 apontamentos, 6 foram corrigidos (itens 3, 4, 7, 10, 11 e 12) e 6 foram
justificados (itens 1, 2, 5, 6, 8 e 9). Os itens 5 e 6 decorrem de uma única regra, que foi
excluída do conjunto.

### 4.3 Execução final (após as correções)

Relatórios: [`docs/relatorios/analise-estatica-final/`](relatorios/analise-estatica-final/).

| Ferramenta | Apontamentos |
|---|---|
| PMD | 0 |
| CPD | 0 |
| Checkstyle | 0 |

O comando `./mvnw clean verify` termina com `BUILD SUCCESS` e com a mensagem
`You have 0 Checkstyle violations.`. Como o build está configurado para falhar diante de
qualquer violação, a análise estática passa a funcionar como **critério de qualidade
contínuo**: qualquer novo apontamento introduzido no código interrompe o build.

## 5. Observações

- O relatório do Checkstyle é gerado em português porque a ferramenta usa a localidade do
  sistema operacional.
- Durante a execução, o Maven pode exibir avisos sobre a URL do projeto pai ("Unable to find
  a URL to the parent project") e sobre um repositório externo com certificado expirado
  (`maven.java.net`). Esses avisos se referem à geração de páginas do relatório e à
  resolução de metadados de terceiros, e não afetam o resultado da análise.
