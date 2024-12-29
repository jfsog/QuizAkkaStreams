package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import org.jfsog.grpc_quiz.v1.quiz.QuizRequest;
import org.jfsog.grpc_quiz.v1.quiz.QuizResponse;
import org.jfsog.grpc_quiz.v1.quiz.QuizSummaryResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserCredentials;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Repository.PerguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuizService implements org.jfsog.grpc_quiz.v1.quiz.gRPCQuizService {
    private final CacheServiceValkey cacheService;
    private final BiscuitTokenService tokenService;
    private final PerguntaRepository perguntaRepository;
    //rever e testar
    @Override
    public Source<QuizResponse, NotUsed> playQuiz(Source<QuizRequest, NotUsed> in) {
        return in.async().map(quizRequest -> {
            var b = QuizResponse.newBuilder();
            var user = cacheService.findByLogin(quizRequest.getUserName());
            if (user != null) {
                perguntaRepository.findById(quizRequest.getQuestionId()).ifPresent(question -> {
                    var acertou = question.getResposta().equals(quizRequest.getResposta());
                    b.setQuestionId(question.getId());
                    b.setFeedback(acertou);
                    if (acertou) {
                        user.setTotalPoints(user.getTotalPoints() + 1);
                        user.setCorrectAnswers(user.getCorrectAnswers() + 1);
                    }
                    user.setTotalQuestions(user.getTotalQuestions() + 1);
                    cacheService.SaveUser(user);
                });
            }
            return b.build();
        });
    }
    //    carece testes
    @Override
    public CompletionStage<QuizSummaryResponse> quizScore(UserCredentials in) {
        var b = QuizSummaryResponse.newBuilder();
        var token = in.getToken();
        var name = in.getUserName();
        var user = cacheService.findByLogin(name);
        if (tokenService.validarToken(token) && user != null) {
            b.setCorrectAnswers(user.getCorrectAnswers())
             .setTotalQuestions(user.getTotalQuestions())
             .setTotalPoints(user.getTotalPoints());
        }
        return CompletableFuture.completedStage(b.build());
    }
}
