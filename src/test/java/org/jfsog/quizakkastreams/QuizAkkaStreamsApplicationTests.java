package org.jfsog.quizakkastreams;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.javadsl.Source;
import org.jfsog.grpc_quiz.v1.quiz.AuthenticationResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserLoginRequest;
import org.jfsog.grpc_quiz.v1.quiz.UserRegisterRequest;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringBootTest
class QuizAkkaStreamsApplicationTests {
    public static final int COUNT = 50;
    @Autowired
    Argon2PasswordEncoder encoder;
    ActorSystem system1 = ActorSystem.create("GrpcClientSystem");
    ActorSystem system2 = ActorSystem.create("GrpcClientSystem2");
    // Configurações do cliente gRPC
    GrpcClientSettings settings1 = GrpcClientSettings.connectToServiceAt("localhost", 9090, system1).withTls(false);
    // Criação do stub do cliente
    gRPCLoginServiceClient client1 = gRPCLoginServiceClient.create(settings1, system1);
    GrpcClientSettings settings2 = GrpcClientSettings.connectToServiceAt("localhost", 9090, system2).withTls(false);
    gRPCLoginServiceClient client2 = gRPCLoginServiceClient.create(settings2, system2);
    @Autowired
    private org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServicePowerApi gRPCLoginServicePowerApi;
    private <T, E> Source<E, NotUsed> generateSource(Supplier<E> build,
                                                     Function<String, T> setname,
                                                     Function<String, T> setPassword,
                                                     String prefix,
                                                     int count) {
        return Source.range(1, count).async().map(i -> {
            setname.apply(prefix + "user__" + i);
            setPassword.apply(prefix + "pass__" + i);
            return build.get();
        });
    }
    @Test
    void contextLoads() throws ExecutionException, InterruptedException, TimeoutException {
        gRPCLoginServiceClient client = gRPCLoginServiceClient.create(settings1, system1);
        // Mensagens para enviar
//        var l=generateSource(UserRegisterRequest::newBuilder, UserRegisterRequest.Builder::setName,)
        var bReq = UserRegisterRequest.newBuilder();
        var reqList1 = generateSource(bReq::build, bReq::setName, bReq::setPassword, "t_1", COUNT);
        var reqList2 = generateSource(bReq::build, bReq::setName, bReq::setPassword, "t_2", COUNT);
        var bLogin = UserLoginRequest.newBuilder();
        var loginList1 = generateSource(bLogin::build, bLogin::setName, bLogin::setPassword, "t_2", COUNT);
        var loginList2 = generateSource(bLogin::build, bLogin::setName, bLogin::setPassword, "t_2", COUNT);
        // Chamando o método gRPC
        Function<Source<UserRegisterRequest, NotUsed>, Source<AuthenticationResponse, NotUsed>> val
                = v -> client1.register(v);
//        CompletableFuture<Done> cf1 = createCompletable(client1::register, list1, system1);
//        CompletableFuture<Done> cf2 = createCompletable(client2::register, list2, system2);
//        CompletableFuture<Done> cf1 = createCompletable(client1::login, loginList1,AuthenticationResponse::getMessage, system1);
//        CompletableFuture<Done> cf2 = createCompletable(client2::login, loginList2,AuthenticationResponse::getMessage, system2);
        CompletableFuture<Done> regCf_1 = createCompletable(client1::register, reqList1,AuthenticationResponse::getMessage, system1);
        CompletableFuture<Done> regCf_2 = createCompletable(client2::register, reqList2,AuthenticationResponse::getMessage, system2);
        var antes = Instant.now();
        CompletableFuture.allOf(regCf_1, regCf_2).get();
//        CompletableFuture.allOf(cf1, cf2).get();
        var depois = Instant.now();
        System.out.println("Tempo total =" + depois.minusMillis(antes.toEpochMilli()).toEpochMilli());
        // Finalizando o sistema
        system1.terminate();
        system2.terminate();
    }
    private <E, T> CompletableFuture<Done> createCompletable(Function<Source<T, NotUsed>, Source<E, NotUsed>> function,
                                                             Source<T, NotUsed> source,
                                                             Function<E, String> getmessage,
                                                             ActorSystem system) {
        var t = function.apply(source)
                        .async()
                        .runForeach(e -> System.out.println("Response: " + getmessage.apply(e)), system)
                        .toCompletableFuture();
//        return function.apply(source).async().runForeach(()->{},system).toCompletableFuture();
        return t;
    }
}
