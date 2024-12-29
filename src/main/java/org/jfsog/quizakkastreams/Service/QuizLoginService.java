package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.stream.FlowShape;
import akka.stream.OverflowStrategy;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfsog.grpc_quiz.v1.quiz.AuthenticationResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserLoginRequest;
import org.jfsog.grpc_quiz.v1.quiz.UserRegisterRequest;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginService;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.jfsog.grpc_quiz.v1.quiz.AuthStatus.FAILURE_VALUE;
import static org.jfsog.grpc_quiz.v1.quiz.AuthStatus.SUCCESS_VALUE;

@Service
@Slf4j
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuizLoginService implements gRPCLoginService {
    private final CacheServiceValkey cacheService;
    private BiscuitTokenService tokenService;
    private Argon2PasswordEncoder passwordEncoder;
    @Override
    public Source<AuthenticationResponse, NotUsed> login(Source<UserLoginRequest, NotUsed> in) {
        return in.buffer(1000, OverflowStrategy.backpressure())
                 .throttle(5, Duration.ofSeconds(1))
                 .mapAsync(16, this::authenticateUser);
    }
    private CompletionStage<AuthenticationResponse> authenticateUser(UserLoginRequest credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var user = cacheService.findByLogin(credentials.getName());
                var builder = AuthenticationResponse.newBuilder();
                if (user != null && passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
                    builder.setMessage("Login bem-sucedido!")
                           .setToken(cacheService.getUserToken(user))
                           .setStatusValue(SUCCESS_VALUE);
                } else {
                    builder.setMessage("Credenciais inválidas!").setStatusValue(FAILURE_VALUE);
                }
                return builder.build();
            } catch (Exception e) {
                System.out.println(credentials);
                throw new RuntimeException(e);
            }
        });
    }
    @Override
    public Source<AuthenticationResponse, NotUsed> register(Source<UserRegisterRequest, NotUsed> in) {
        int paralelismo = 16;
        var flow = Flow.of(UserRegisterRequest.class)
                       //simulando um cenário de baixa capacidade de processamento, evitando sobrecarga
//                       .throttle(2, Duration.ofSeconds(5))
                       .mapAsyncUnordered(paralelismo, r -> CompletableFuture.completedFuture(processRequest(r)));
        //distribuição e controle de concorrência
        return in.via(Flow.fromGraph(GraphDSL.create(b -> {
            UniformFanOutShape<UserRegisterRequest, UserRegisterRequest> balance = b.add(Balance.create(paralelismo));
            UniformFanInShape<AuthenticationResponse, AuthenticationResponse> merge = b.add(Merge.create(paralelismo));
            for (int i = 0; i < paralelismo; i++)
                b.from(balance.out(i)).via(b.add(flow.async())).toInlet(merge.in(i));
            return new FlowShape<>(balance.in(), merge.out());
        })));
    }
    private AuthenticationResponse processRequest(UserRegisterRequest request) {
        var name = request.getName();
        // uso de lock para garantir que apenas uma thread consiga tentar registrar usuário
        RLock lock = cacheService.getUserLock(name);
        try {
            lock.lock();
            //validação da requisição
            validateRegistrationRequest(request);
            //verifica se usuário já existe no cache ou no banco
            if (cacheService.existsByLogin(name)) return createDuplicateUserResponse(request);
            var newuser = createNewUser(request);
            cacheService.SaveUser(newuser);
            return createSuccessRegistrationResponse(newuser);
        } catch (IllegalArgumentException e) {
            return createFailureRegistrationResponse(request, e.getMessage());
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    private void validateRegistrationRequest(@NotNull UserRegisterRequest request) throws IllegalArgumentException {
        if (request.getName().isBlank()) {
            throw new IllegalArgumentException("Nome do usuário não pode ser nulo");
        }
        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres");
        }
    }
    private AuthenticationResponse createDuplicateUserResponse(UserRegisterRequest request) {
        return AuthenticationResponse.newBuilder()
                                     .setMessage("Usuário %s já existe! em %s".formatted(request.getName(),
                                             Thread.currentThread().getName()))
                                     .setStatusValue(FAILURE_VALUE)
                                     .build();
    }
    private Users createNewUser(UserRegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        return new Users(request.getName(), encodedPassword, request.getRole());
    }
    private AuthenticationResponse createSuccessRegistrationResponse(Users user) {
        return AuthenticationResponse.newBuilder()
                                     .setMessage("Usuário %s salvo com sucesso em %s!".formatted(user.getLogin(),
                                             Thread.currentThread().getName()))
                                     .setToken(tokenService.createUserToken(user))
                                     .setStatusValue(SUCCESS_VALUE)
                                     .build();
    }
    private AuthenticationResponse createFailureRegistrationResponse(UserRegisterRequest request, String message) {
        return AuthenticationResponse.newBuilder()
                                     .setMessage("User failed: %s:%s".formatted(request, message))
                                     .setStatusValue(FAILURE_VALUE)
                                     .build();
    }
}