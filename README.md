# SalaPrecisa

[![Build e testes](https://github.com/lucas-palmeida/sala-precisa/actions/workflows/build.yml/badge.svg)](https://github.com/lucas-palmeida/sala-precisa/actions/workflows/build.yml)

Sistema web para gerenciamento de reservas de salas (salas de reunião, auditórios,
laboratórios e outros ambientes), desenvolvido como projeto da disciplina de
**Verificação e Validação de Sistemas** (Tecnologia em Sistemas para Internet — IFRS).

O foco do trabalho é a aplicação de técnicas de V&V: planejamento de testes, análise
estática, teste unitário, teste de componente, teste de sistema e teste de segurança.

## Funcionalidades

- Cadastro, edição, desativação e reativação de salas (nome, tipo e capacidade);
- Listagem de salas com a situação atual: **Livre**, **Ocupada** ou **Inativa**;
- Reserva de sala para um solicitante em um período, com validações:
  - não permite sobreposição com outra reserva ativa da mesma sala (horários adjacentes são permitidos);
  - o fim deve ser posterior ao início, o início não pode estar no passado e a duração máxima é de 8 horas;
- Cancelamento de reservas;
- Encerramento automático das reservas cujo período terminou, liberando a sala.

O sistema oferece **telas web** (Thymeleaf) e uma **API REST** (JSON) sobre as mesmas regras.

## Tecnologias

Java 17 · Spring Boot 4.1.3 (Web MVC, Data JPA, Validation) · Thymeleaf · H2 (em memória) · Maven

**Testes e qualidade:** JUnit 5 · Mockito · AssertJ · RestAssured · Selenium WebDriver ·
JaCoCo · PMD · Checkstyle

## Pré-requisitos

O projeto funciona em **Linux, macOS e Windows**. O build completo é executado nos três
sistemas a cada *push* pelo GitHub Actions (ver "Integração contínua").

- **JDK 17**, a versão usada no desenvolvimento e nos testes;
- **um navegador**, apenas para os testes de sistema (ver tabela abaixo);
- acesso à internet na primeira execução, para o *download* do Maven, das dependências e
  do driver do navegador. O driver é obtido automaticamente pelo Selenium Manager.

Não é necessário instalar o Maven: o projeto inclui o **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

### Instalação do JDK 17

Confira primeiro com `java -version`. Se a versão exibida já for a 17, não é preciso
fazer nada.

| Sistema | Instalação | `JAVA_HOME` (se necessário) |
|---|---|---|
| **macOS** | Instalador `.pkg` do Eclipse Temurin 17 (https://adoptium.net) ou, com Homebrew, `brew install --cask temurin@17` | `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` (para manter, acrescente essa linha ao `~/.zshrc`) |
| **Linux** (Ubuntu/Debian) | `sudo apt install openjdk-17-jdk` | `export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))` (para manter, acrescente ao `~/.bashrc`) |
| **Windows** | Instalador `.msi` do Eclipse Temurin 17 (https://adoptium.net), marcando a opção *Set JAVA_HOME variable* | Configurado pelo instalador. Abra um novo terminal depois |

### Navegador dos testes de sistema

O navegador é escolhido automaticamente conforme o sistema operacional. Também é possível
escolher outro com `-Dnavegador=edge`, `chrome` ou `firefox`.

| Sistema | Navegador padrão | Observação |
|---|---|---|
| **macOS** | Google Chrome | Se o Chrome não estiver instalado, o Selenium Manager baixa automaticamente uma versão própria para testes (*Chrome for Testing*) |
| **Linux** | Google Chrome | Idem |
| **Windows** | Microsoft Edge | Já vem instalado no Windows |

### Comandos em cada sistema

Os exemplos deste documento usam `./mvnw`, a forma usada no **Linux**, no **macOS** e no
Git Bash. No Windows:

- no **PowerShell**, use `.\mvnw.cmd`;
- no **Prompt de Comando**, use `mvnw.cmd`.

Argumentos `-D` que contêm ponto devem ficar entre aspas duplas, como já aparecem nos
exemplos (por exemplo `"-Dit.test=..."`). As aspas funcionam em todos os sistemas e são
obrigatórias no PowerShell.

## Passo a passo para avaliação

1. **Obter o projeto:**
   ```bash
   git clone https://github.com/lucas-palmeida/sala-precisa.git
   cd sala-precisa
   ```
2. **Linux/macOS:** se `./mvnw` responder "Permission denied", execute `chmod +x mvnw`.
3. **Build padrão.** Executa a análise estática, os testes unitários, de componente e de
   segurança, além da verificação de cobertura:
   ```bash
   ./mvnw clean verify
   ```
   Resultado esperado: `BUILD SUCCESS`, com `Tests run: 91` (unitários, Surefire) e
   `Tests run: 124` (componente e segurança, Failsafe).
4. **Build completo, com os testes de sistema no navegador** (Chrome no Linux e no macOS,
   Edge no Windows, sem abrir janela):
   ```bash
   ./mvnw clean verify -Psistema
   ```
   Resultado esperado: `BUILD SUCCESS`, com `Tests run: 91` e `Tests run: 140`. Os 16
   testes a mais são os 15 testes de sistema e o teste de XSS no navegador. As capturas
   de tela em `docs/evidencias/sistema/` são regravadas nessa execução.
5. **Consultar os resultados gerados:**

   | Resultado | Local |
   |---|---|
   | Cobertura consolidada (abrir no navegador) | `target/site/jacoco-consolidado/index.html` |
   | Cobertura só dos testes unitários / de componente | `target/site/jacoco/index.html` / `target/site/jacoco-it/index.html` |
   | Resultados dos testes unitários | `target/surefire-reports/` |
   | Resultados dos testes de componente, sistema e segurança | `target/failsafe-reports/` |
   | Relatórios HTML do PMD, CPD e Checkstyle | `target/reports/` (gerados pelo comando `./mvnw compile pmd:pmd pmd:cpd checkstyle:checkstyle`) |
   | Capturas de tela dos testes de sistema | `docs/evidencias/sistema/` |

   Para abrir um relatório HTML pelo terminal:

   | Sistema | Comando |
   |---|---|
   | macOS | `open target/site/jacoco-consolidado/index.html` |
   | Linux | `xdg-open target/site/jacoco-consolidado/index.html` |
   | Windows | `start target\site\jacoco-consolidado\index.html` |

6. **Usar o sistema manualmente:** veja a seção a seguir.

Os resultados da execução feita pelo autor também estão versionados em `docs/relatorios/`
e `docs/evidencias/`.

## Executando a aplicação

```bash
./mvnw spring-boot:run
```

Acesse **http://localhost:8080**. Para encerrar, pressione `Ctrl+C` no terminal. O banco
é em memória, então os dados são perdidos ao encerrar a aplicação.

### API REST

| Método | Endereço | Descrição |
|---|---|---|
| `GET` | `/api/salas` | Lista as salas (com o indicador `ocupada`) |
| `GET` | `/api/salas/{id}` | Consulta uma sala |
| `POST` | `/api/salas` | Cadastra uma sala: `{"nome": "Lab 101", "tipo": "LABORATORIO", "capacidade": 30}` |
| `PUT` | `/api/salas/{id}` | Altera uma sala |
| `PATCH` | `/api/salas/{id}/desativar` · `/ativar` | Desativa ou reativa uma sala |
| `GET` | `/api/salas/{id}/reservas` | Lista as reservas de uma sala |
| `GET` | `/api/reservas` · `/api/reservas/{id}` | Lista ou consulta reservas |
| `POST` | `/api/reservas` | Cria uma reserva: `{"salaId": 1, "solicitante": "Ana", "inicio": "2030-01-10T10:00:00", "fim": "2030-01-10T12:00:00"}` |
| `PATCH` | `/api/reservas/{id}/cancelar` | Cancela uma reserva |

Tipos de sala: `SALA_REUNIAO`, `AUDITORIO`, `LABORATORIO` e `OUTRO`.

Respostas de erro: `400` (dados inválidos ou regra de negócio), `404` (não encontrado) e
`409` (conflito de horário), sempre com o corpo `{"status", "mensagem", "detalhes"}`.

## Executando os testes

| Comando | O que executa |
|---|---|
| `./mvnw clean verify` | Análise estática (PMD, CPD, Checkstyle) + testes unitários + componente + segurança + cobertura (JaCoCo) |
| `./mvnw clean verify -Psistema` | Tudo acima + **testes de sistema** com Selenium (Chrome no Linux/macOS, Edge no Windows; *headless*) |
| `./mvnw verify -Psistema -Dheadless=false` | Exibe a janela do navegador durante os testes de sistema |
| `./mvnw verify -Psistema -Dnavegador=firefox` | Usa um navegador específico (`edge`, `chrome` ou `firefox`) |
| `./mvnw verify -Psistema "-Dit.test=Seguranca*IT,XssNavegador*IT"` | Executa, além dos unitários, apenas os testes de segurança |
| `./mvnw compile pmd:pmd pmd:cpd checkstyle:checkstyle` | Gera apenas os relatórios de análise estática |

O build **falha** se algum teste falhar, se houver violação de PMD, CPD ou Checkstyle, ou
se a cobertura ficar abaixo de 80 % (linhas e ramos).

| Nível | Ferramentas | Testes |
|---|---|---|
| Unitário | JUnit 5 + Mockito | 91 |
| Componente | RestAssured (API), MockMvc (telas), `@DataJpaTest` (persistência) | 89 |
| Sistema | Selenium WebDriver + Page Objects | 15 |
| Segurança | RestAssured + Selenium | 36 |

Cobertura consolidada: **98,8 % das linhas** e **98,4 % dos ramos**.

## Integração contínua

O workflow [`.github/workflows/build.yml`](.github/workflows/build.yml) executa o build
completo (`./mvnw -B clean verify -Psistema`) em **Ubuntu, macOS e Windows** a cada
*push*, usando as máquinas do GitHub Actions. O selo no topo deste documento mostra o
resultado da última execução.

Os relatórios de cada sistema (testes, cobertura e capturas de tela) ficam disponíveis
para *download* na página da execução, na aba **Actions** do repositório, como artefatos
`relatorios-<sistema>`.

## Problemas comuns

| Sintoma | Sistema | Solução |
|---|---|---|
| `java: command not found`, `java` não reconhecido ou `JAVA_HOME` inválido | Todos | Instale o JDK 17 e configure o `JAVA_HOME` (ver "Instalação do JDK 17"); depois abra um novo terminal |
| `java -version` mostra outra versão (por exemplo, 11 ou 21) | macOS | `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` antes de executar o `./mvnw` |
| `java -version` mostra outra versão | Linux | `sudo update-alternatives --config java` e escolha a 17 |
| `Permission denied` ao executar `./mvnw` | Linux/macOS | `chmod +x mvnw` |
| `Unknown lifecycle phase ".test=..."` | Windows (PowerShell) | Coloque o argumento `-D` entre aspas duplas: `"-Dit.test=..."` |
| Navegador padrão indisponível nos testes de sistema | Todos | Escolha outro instalado: `-Dnavegador=chrome`, `firefox` ou `edge` |
| Porta 8080 já em uso ao executar a aplicação | Todos | `./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"` e acesse http://localhost:8081. No Linux/macOS, `lsof -i :8080` mostra qual processo ocupa a porta |

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/plano-de-teste.md`](docs/plano-de-teste.md) | Plano de teste: escopo, estratégia, critérios, riscos, cronograma, ambiente, rastreabilidade e resultados |
| [`docs/analise-estatica.md`](docs/analise-estatica.md) | Configuração da análise estática e tratamento de cada apontamento |
| [`docs/teste-seguranca.md`](docs/teste-seguranca.md) | Teste de segurança: casos, achados, correções e evidências |
| `docs/relatorios/` | Relatórios de execução: testes, cobertura, análise estática e logs |
| `docs/evidencias/` | Capturas de tela dos testes de sistema e de segurança |

## Estrutura do projeto

```
.github/workflows/build.yml   Integração contínua (Ubuntu, macOS e Windows)
src/main/java/br/edu/ifrs/salaprecisa/
├── dominio/       Entidades Sala e Reserva e enumerações
├── repositorio/   Repositórios Spring Data JPA
├── servico/       Regras de negócio e tarefa agendada de encerramento
├── api/           API REST (controllers, DTOs, tratamento de erros)
├── web/           Telas (controllers e formulários)
├── config/        Relógio, agendamento e cabeçalhos de segurança
└── excecao/       Exceções de negócio
src/test/java/br/edu/ifrs/salaprecisa/
├── dominio/, servico/        Testes unitários (*Test)
├── api/, repositorio/, web/  Testes de componente (*IT)
├── sistema/                  Testes de sistema (*SistemaIT) e Page Objects
└── seguranca/                Testes de segurança
```

## Autor

Lucas Pacheco — Verificação e Validação de Sistemas, IFRS.
