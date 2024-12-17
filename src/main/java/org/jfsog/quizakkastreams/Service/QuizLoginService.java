package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.grpc.javadsl.Metadata;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Source;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfsog.grpc_quiz.v1.quiz.AuthenticationResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserLoginRequest;
import org.jfsog.grpc_quiz.v1.quiz.UserRegisterRequest;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.jfsog.quizakkastreams.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.jfsog.grpc_quiz.v1.quiz.AuthStatus.FAILURE_VALUE;
import static org.jfsog.grpc_quiz.v1.quiz.AuthStatus.SUCCESS_VALUE;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class QuizLoginService implements org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServicePowerApi {
    private final UsersRepository usersRepository;
    private final CacheServiceValkey cacheService;
    private BiscuitTokenService tokenService;
    private Argon2PasswordEncoder passwordEncoder;
    @Override
    public Source<AuthenticationResponse, NotUsed> login(Source<UserLoginRequest, NotUsed> in, Metadata metadata) {
        return in.buffer(10, OverflowStrategy.backpressure()).mapAsyncUnordered(12, this::authenticateUser);
    }
    private CompletionStage<AuthenticationResponse> authenticateUser(UserLoginRequest credentials) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }
    @Override
    public Source<AuthenticationResponse, NotUsed> register(Source<UserRegisterRequest, NotUsed> in,
                                                            Metadata metadata) {
        return in.buffer(4, OverflowStrategy.backpressure()).mapAsyncUnordered(12, this::registerUser);
    }
    protected CompletionStage<AuthenticationResponse> registerUser(UserRegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                //está tentando registrar usuário novamete
                validateRegistrationRequest(request);
                synchronized (cacheService) {
                    var user = cacheService.findByLogin(request.getName());
                    if (user != null) {
                        return createDuplicateUserResponse(request);
                    }
                    return createSuccessRegistrationResponse(cacheService.SaveUser(createNewUser(request)));
                }
            } catch (IllegalArgumentException e) {
                log.info("Erro de validação durante o registro do usuário", e);
                return createFailureRegistrationResponse(request, e.getMessage());
            } catch (Exception e) {
                log.info("Erro no servidor não experado!");
                e.printStackTrace(System.out);
                return createFailureRegistrationResponse(request, e.getMessage());
            }
        });
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
                                     .setMessage("Usuário %s já existe!".formatted(request.getName()))
                                     .setStatusValue(FAILURE_VALUE)
                                     .build();
    }
    private Users createNewUser(UserRegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        return new Users(request.getName(), encodedPassword, request.getRole());
    }
    private AuthenticationResponse createSuccessRegistrationResponse(Users user) {
        return AuthenticationResponse.newBuilder()
                                     .setMessage("Usuário %s salvo com sucesso!".formatted(user.getLogin()))
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
