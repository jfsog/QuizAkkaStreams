package org.jfsog.quizakkastreams;

import akka.actor.ActorSystem;
import akka.grpc.javadsl.ServerReflection;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.Http;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginService;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServiceHandlerFactory;
import org.jfsog.grpc_quiz.v1.quiz.gRPCQuizService;
import org.jfsog.grpc_quiz.v1.quiz.gRPCQuizServiceHandlerFactory;
import org.jfsog.quizakkastreams.Service.QuizLoginService;
import org.jfsog.quizakkastreams.Service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetSocketAddress;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@AllArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j  // Adiciona logging estruturado
public class QuizAkkaStreamsApplication {
    public static void main(String[] args) {
        // Configuração do Akka HTTP com suporte a HTTP/2
        Config conf = ConfigFactory.parseString("akka.http.server.enable-http2 = on")
                                   .withFallback(ConfigFactory.defaultApplication());
        var actorSystem = ActorSystem.create("QuizAkkaStreams", conf);
        // Inicializa o contexto do Spring
        var context = SpringApplication.run(QuizAkkaStreamsApplication.class, args);
        // Obtém os serviços do Spring
        var quizService = context.getBean(QuizService.class);
        var quizLoginService = context.getBean(QuizLoginService.class);
        // Inicializa o servidor gRPC
        startGrpcServer(actorSystem, quizService, quizLoginService);
        // Adiciona um hook para desligar corretamente o ActorSystem ao encerrar a aplicação
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando o sistema de atores...");
            actorSystem.terminate();
        }));
    }
    private static void startGrpcServer(ActorSystem actorSystem, QuizService quizService,
                                        QuizLoginService quizLoginService) {
        try {
            // Cria os handlers para os serviços gRPC
            var loginHandler = gRPCLoginServiceHandlerFactory.create(quizLoginService, actorSystem);
            var quizHandler = gRPCQuizServiceHandlerFactory.create(quizService, actorSystem);
            var reflectionHandler = ServerReflection.create(
                    List.of(gRPCQuizService.description, gRPCLoginService.description), actorSystem);
            // Junta os handlers em um único ServiceHandler
            @SuppressWarnings("unchecked")
            var serviceHandlers = ServiceHandler.concatOrNotFound(quizHandler, loginHandler, reflectionHandler);
            // Inicia o servidor gRPC na porta 9090
            var serverBinding = Http.get(actorSystem).newServerAt("localhost", 9090).bind(serviceHandlers);
            serverBinding.thenAccept(binding -> {
                InetSocketAddress address = binding.localAddress();
                log.info("🚀 Servidor gRPC iniciado em {}:{}", address.getHostString(), address.getPort());
            }).exceptionally(ex -> {
                log.error("❌ Erro ao iniciar o servidor gRPC: {}", ex.getMessage());
                actorSystem.terminate();  // Encerra o sistema em caso de erro crítico
                return null;
            });
        } catch (Exception e) {
            log.error("Erro inesperado ao iniciar o servidor gRPC", e);
            actorSystem.terminate();  // Garante que o sistema será encerrado em caso de falha
        }
    }
}
