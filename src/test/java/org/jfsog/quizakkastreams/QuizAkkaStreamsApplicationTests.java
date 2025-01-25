package org.jfsog.quizakkastreams;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.javadsl.Source;
import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import org.jfsog.grpc_quiz.v1.quiz.AuthenticationResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserLoginRequest;
import org.jfsog.grpc_quiz.v1.quiz.UserRegisterRequest;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServiceClient;
import org.jfsog.quizakkastreams.Models.Pergunta.Pergunta;
import org.jfsog.quizakkastreams.Repository.PerguntaRepository;
import org.jfsog.quizakkastreams.Repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.time.Instant;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
class QuizAkkaStreamsApplicationTests {
    public static final int COUNT = 100;
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
    @Autowired
    private PerguntaRepository perguntaRepository;
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
                UserLoginRequest.Builder::build, UserLoginRequest.Builder::setUserName,
                UserLoginRequest.Builder::setPassword);
        Source<UserLoginRequest, NotUsed> requestl2 = generate(COUNT, "t_1", UserLoginRequest.newBuilder(),
                UserLoginRequest.Builder::build, UserLoginRequest.Builder::setUserName,
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
    void testUserRegistrationLoad() throws ExecutionException, InterruptedException {
        Source<UserRegisterRequest, NotUsed> requestList1 = generate(COUNT, "t_1", UserRegisterRequest.newBuilder(),
                UserRegisterRequest.Builder::build, UserRegisterRequest.Builder::setUserName,
                UserRegisterRequest.Builder::setPassword);
        Source<UserRegisterRequest, NotUsed> requestList2 = generate(COUNT, "t_2", UserRegisterRequest.newBuilder(),
                UserRegisterRequest.Builder::build, UserRegisterRequest.Builder::setUserName,
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
        return Source.range(1, count).map(i -> {
            setName.accept(builder, "%suser__%d".formatted(prefix, i));
            setPassword.accept(builder, "%spass__%d".formatted(prefix, i));
            return build.apply(builder);
        });
    }
    private <E, T> CompletableFuture<Done> runReactiveStream(Function<T, CompletionStage<E>> function,
                                                             Source<T, NotUsed> source, Function<E, String> getMessage,
                                                             ActorSystem system) {
        return source.map(function::apply)
                     .runForeach(e -> {}, system)
                     .whenComplete((done, e) -> system.terminate())
                     .toCompletableFuture();
//        return function.apply(source)
////                       .runForeach(e -> System.out.printf("Response: %s%n", getMessage.apply(e)), system)
//                       .runForeach(e -> {}, system).whenComplete((done, e) -> system.terminate())
//                       .toCompletableFuture();
    }
    @Test
    public void LoadFakeQuestions() {
        int alternativas = 5;
        var m2 = Stream.rangeClosed(1, 100)
                       .shuffle()
                       .zipWith(Stream.iterate(0, i -> i + 1), Tuple2::new)
                       .collect(Collectors.groupingBy(t -> t._2() / alternativas,
                               Collectors.mapping(Tuple2::_1, Collectors.toCollection(TreeSet::new))));
        var perguntas = m2.values().stream().map(v -> {
            var AlternativasSet = v.stream().map(Objects::toString).collect(Collectors.toSet());
            return Pergunta.builder()
                           .disciplina("Matemática")
                           .enunciado("Qual o menor número?: " + String.join(",", AlternativasSet))
                           .alternativas(AlternativasSet)
                           .resposta(v.pollFirst() + "")
                           .build();
        }).toList();
        perguntaRepository.saveAll(perguntas);
        m2.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}

