package org.jfsog.quizakkastreams;

import akka.actor.ActorSystem;
import akka.grpc.javadsl.ServiceHandler;
import akka.http.javadsl.Http;
import akka.stream.Materializer;
import com.google.common.reflect.Reflection;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.AllArgsConstructor;
import org.jfsog.grpc_quiz.v1.quiz.gRPCLoginServiceHandlerFactory;
import org.jfsog.quizakkastreams.Service.QuizLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuizAkkaStreamsApplication {
    public static void main(String[] args) {
        Config conf = ConfigFactory.parseString("akka.http.server.enable-http2 = on")
                                   .withFallback(ConfigFactory.defaultApplication());
        var sys = ActorSystem.create("QuizAkkaStreams", conf);
//        var mat = Materializer.createMaterializer(sys);
        var context = SpringApplication.run(QuizAkkaStreamsApplication.class, args);
        var quizloginService = context.getBean(QuizLoginService.class);
        var t = gRPCLoginServiceHandlerFactory.create(quizloginService, sys);
        var reflection = gRPCLoginServiceHandlerFactory.createWithServerReflection(quizloginService, sys);
//        @SuppressWarnings("unchecked")
//        var serviceHandlers = ServiceHandler.handler(t);
//        @SuppressWarnings("unchecked")
        var reflectionHandlers = ServiceHandler.handler(reflection);
        Http.get(sys)
            .newServerAt("localhost", 9090)
//                .bind(reflection)
            .bind(reflectionHandlers)
            .thenAccept(binding -> System.out.printf("gRPC server bound to: %s%n", binding.localAddress()));
    }
}
