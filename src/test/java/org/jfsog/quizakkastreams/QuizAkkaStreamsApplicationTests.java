package org.jfsog.quizakkastreams;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.javadsl.Source;
import io.vavr.collection.Stream;
import org.jfsog.grpc_quiz.v1.quiz.AuthenticationResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserLoginRequest;
import org.jfsog.grpc_quiz.v1.quiz.UserRegisterRequest;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServiceClient;
import org.jfsog.quizakkastreams.Repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SpringBootTest
class QuizAkkaStreamsApplicationTests {
    public static final int COUNT = 2000;
    //Criação de 2 sistemas de atores
    ActorSystem system1 = ActorSystem.create("GrpcClientSystem");
    ActorSystem system2 = ActorSystem.create("GrpcClientSystem2");
    // Configurações do cliente gRPC para cada sistema
    GrpcClientSettings settings1 = GrpcClientSettings.connectToServiceAt("localhost", 9090, system1).withTls(false);
    // Criação dos clientes
    gRPCLoginServiceClient client1 = gRPCLoginServiceClient.create(settings1, system1);
    GrpcClientSettings settings2 = GrpcClientSettings.connectToServiceAt("localhost", 9090, system2).withTls(false);
    gRPCLoginServiceClient client2 = gRPCLoginServiceClient.create(settings2, system2);
    @Autowired
    private Argon2PasswordEncoder encoder;
    @Autowired
    private UsersRepository usersRepository;
    @Test
    void testTimeToEncode() {
        var stream = Stream.rangeClosed(1, COUNT).map(l -> "t_1" + "pass_" + l).toJavaParallelStream();
        var antes = Instant.now();
        stream.forEach(s -> encoder.encode(s));
        var depois = Instant.now();
        System.out.println("Tempo total =" + depois.minusMillis(antes.toEpochMilli()).toEpochMilli());
        System.out.println("Total de codificações : " + COUNT);
    }
    @Test
    void testUserLoginLoad() throws ExecutionException, InterruptedException {
        // Login de usuários usando os dois clientes em paralelo.
        Source<UserLoginRequest, NotUsed> requestl1 = generate(COUNT, "t_1", UserLoginRequest.newBuilder(),
                UserLoginRequest.Builder::build, UserLoginRequest.Builder::setName,
                UserLoginRequest.Builder::setPassword);
        Source<UserLoginRequest, NotUsed> requestl2 = generate(COUNT, "t_1", UserLoginRequest.newBuilder(),
                UserLoginRequest.Builder::build, UserLoginRequest.Builder::setName,
                UserLoginRequest.Builder::setPassword);
        CompletableFuture<Done> loginCf_1 = runReactiveStream(client1::login, requestl1,
                AuthenticationResponse::toString, system1);
        CompletableFuture<Done> loginCf_2 = runReactiveStream(client2::login, requestl2,
                AuthenticationResponse::toString, system1);
        var antes = Instant.now();
        CompletableFuture.allOf(loginCf_1, loginCf_2).get();
        var depois = Instant.now();
        System.out.println("Tempo total = " + depois.minusMillis(antes.toEpochMilli()).toEpochMilli());
    }
    @Test
    void testeIO() {
        Stream.continually("Teste nosso de io").forEach(System.out::println);
    }
    @Test
    void testUserRegistrationLoad() throws ExecutionException, InterruptedException {
        // Registro de usuários usando os dois clientes em paralelo.
//        var requestList1 = generateRequests(COUNT, "t_1");
//        var requestList2 = generateRequests(COUNT, "t_2");
        Source<UserRegisterRequest, NotUsed> requestList1 = generate(COUNT, "t_1", UserRegisterRequest.newBuilder(),
                UserRegisterRequest.Builder::build, UserRegisterRequest.Builder::setName,
                UserRegisterRequest.Builder::setPassword);
        Source<UserRegisterRequest, NotUsed> requestList2 = generate(COUNT, "t_2", UserRegisterRequest.newBuilder(),
                UserRegisterRequest.Builder::build, UserRegisterRequest.Builder::setName,
                UserRegisterRequest.Builder::setPassword);
        // Registro de usuários usando os dois clientes em paralelo.
        CompletableFuture<Done> regCf_1 = runReactiveStream(client1::register, requestList1,
                AuthenticationResponse::getMessage, system1);
        CompletableFuture<Done> regCf_2 = runReactiveStream(client2::register, requestList2,
                AuthenticationResponse::getMessage, system2);
        var antes = Instant.now();
        CompletableFuture.allOf(regCf_1, regCf_2).get();
        var depois = Instant.now();
        System.out.println("Tempo total =" + depois.minusMillis(antes.toEpochMilli()).toEpochMilli());
        var count = usersRepository.count();
        System.out.println("Users total =" + count);
    }
    private <E, T> Source<T, NotUsed> generate(int count, String prefix, E builder, Function<E, T> build,
                                               BiConsumer<E, String> setName, BiConsumer<E, String> setPassword) {
        return Source.range(1, count ).map(i -> {
            setName.accept(builder, "%suser__%d".formatted(prefix, i));
            setPassword.accept(builder, "%spass__%d".formatted(prefix, i));
            return build.apply(builder);
        });
    }
    /**
     * Executa uma função assíncrona para processar uma fonte reativa e retorna um CompletableFuture.
     *
     * @param <E>        Tipo da saída do processamento.
     * @param <T>        Tipo dos elementos na fonte de entrada.
     * @param function   Função que processa a fonte de entrada.
     * @param source     Fonte de entrada.
     * @param getMessage Função para extrair mensagens de resposta.
     * @param system     Sistema de ator a ser usado.
     * @return CompletableFuture representando a conclusão do processamento.
     */
    private <E, T> CompletableFuture<Done> runReactiveStream(Function<Source<T, NotUsed>, Source<E, NotUsed>> function,
                                                             Source<T, NotUsed> source, Function<E, String> getMessage,
                                                             ActorSystem system) {
        return function.apply(source)
//                       .runForeach(e -> System.out.printf("Response: %s%n", getMessage.apply(e)), system)
                       .runForeach(e -> {}, system)
                       .whenComplete((done, e) -> system.terminate()).toCompletableFuture();
    }
}
