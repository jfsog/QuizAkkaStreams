# QuizAkkaStreams

QuizAkkaStreams é um projeto baseado em **Akka-gRPC**, **Biscuit tokens** e **Redisson (Valkey)** para autenticação e execução de um jogo de quiz via gRPC.

## 📌 Tecnologias Utilizadas

- **Java + Spring Boot** → Backend principal.
- **Akka-gRPC** → Comunicação entre serviços via gRPC.
- **Biscuit tokens** → Autenticação baseada em tokens com validade de 30 segundos.
- **Redisson + Valkey** → Cache para tokens de autenticação e usuários.
- **Docker** → Infraestrutura para rodar o Valkey e PostgreSQL.
- **Maven** → Gerenciamento de dependências e compilação.
## 🛠 Estrutura do Projeto
```bash
QuizAkkaStreams/
│── README.md               # Documentação do projeto
│── docker-compose.yml      # Configuração do Valkey
│── pom.xml                 # Configuração do Maven
│── src/
│   ├── main/
│   │   ├── java/org/jfsog/quizakkastreams/
│   │   │   ├── Biscuit/                  # Serviço de tokens
│   │   │   │   ├── BiscuitTokenService.java
│   │   │   ├── Config/                    # Configurações
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── RedissonSpringDataConfig.java
│   │   │   ├── Models/                     # Modelos do banco
│   │   │   │   ├── User/Users.java
│   │   │   │   ├── Pergunta/Pergunta.java
│   │   │   ├── Repository/                 # Repositórios
│   │   │   │   ├── UsersRepository.java
│   │   │   │   ├── PerguntaRepository.java
│   │   │   ├── Service/                    # Lógica de negócios
│   │   │   │   ├── CacheServiceValkey.java
│   │   │   │   ├── QuizService.java
│   │   │   │   ├── QuizLoginService.java
│   │   │   ├── QuizAkkaStreamsApplication.java
│   │   ├── resources/
│   │   │   ├── application.yaml            # Configuração do Spring Boot
│   │   ├── proto/                          # Definição dos serviços gRPC
│   │   │   ├── QuizService.proto
│   │   │   ├── LoginService.proto
│   │   │   ├── Enums.proto
│   ├── test/
│   │   ├── java/org/jfsog/quizakkastreams/
│   │   │   ├── QuizAkkaStreamsApplicationTests.java
```
## 🚀 Como Executar o Projeto

### Pré-requisitos
- **JDK 17**
- **Maven**
- **Docker e Docker Compose** (para rodar o Valkey e PostgreSQL)

### 📦 Configurar e Rodar

1. **Clonar o repositório**
```bash 
  git clone https://github.com/jfsog/QuizAkkaStreams.git
  cd QuizAkkaStreams
```
3. **Subir o container do Valkey e PostgreSQL**

```bash
  docker-compose up -d
```
-   **Gerar os arquivos gRPC a partir dos `.proto`**
```bash 
  mvn akka-grpc:generate
````

-   **Rodar a aplicação**
```bash
  mvn spring-boot:run
```
## 🔐 Autenticação com Biscuit + Cache com Redisson (Valkey)

-   **O sistema gera um token Biscuit na autenticação.**
-   **O token é armazenado no Valkey (cache) por 30 segundos.**
-   **Se o token expirar, o usuário precisa fazer login novamente.**
## 🧪 Testes
Para testar os endpoints gRPC, usei o **Postman**, usando reflexão e configurando as chamadas para os serviços, mas o mesmo efeito pode ser atingido importando os arquivos `.proto`.
Testes também podem ser feitos usando `grpcurl` via terminal.

Teste de registro:
```bash
grpcurl -plaintext -d '{
  "password":  "987654321",
  "role":  "USER",
  "user_name":  "Tester"
  }' localhost:9090 org.jfsog.grpc_quiz.v1.quiz.gRPCLoginService.Register
```
Saída esperada:
```bash
{
"status":  "SUCCESS",
"token":  "ErwBClIKBlRlc3......",
"message":  "Usuário Tester salvo com sucesso!"
}
```
Teste de obter score do jogador:
```bash
grpcurl -plaintext -d '{
  "token": "ErwBClIKBlRlc3...",
  "user_name": "Tester"
  }' localhost:9090 org.jfsog.grpc_quiz.v1.quiz.gRPCQuizService.QuizScore
```
Exemplo de saída:
```bash
{
    "total_questions": 30,
    "correct_answers": 25,
    "total_points": "25",
    "status": "SUCCESS"
}
```
## 📌 Considerações Finais

Este projeto explora autenticação segura com **Biscuit tokens**, caching eficiente com **Redisson + Valkey**, e **gRPC** para comunicação de alto desempenho. 🚀