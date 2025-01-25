package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import org.jfsog.grpc_quiz.v1.quiz.*;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Repository.PerguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuizService implements gRPCQuizService {
    private CacheServiceValkey cacheService;
    private BiscuitTokenService tokenService;
    private PerguntaRepository perguntaRepository;
    @Override
    public Source<QuizResponse, NotUsed> playQuiz(Source<QuizRequest, NotUsed> in) {
        return in.map(quizRequest -> {
            var resBuilder = QuizResponse.newBuilder();
            var user = cacheService.findByLogin(quizRequest.getUserName());
            if (tokenService.validarToken(quizRequest.getToken(),
                    "check if user(\"%s\")".formatted(quizRequest.getUserName())) && user != null) {
                resBuilder.setStatus(AuthStatus.SUCCESS);
                perguntaRepository.findById(quizRequest.getQuestionId()).ifPresent(question -> {
                    var isCorrect = question.getResposta().equals(quizRequest.getAnswer());
                    resBuilder.setQuestionId(question.getId()).setIsCorrect(isCorrect);
                    if (isCorrect) {
                        user.setTotalPoints(user.getTotalPoints() + 1);
                        user.setCorrectAnswers(user.getCorrectAnswers() + 1);
                    }
                    user.setTotalQuestions(user.getTotalQuestions() + 1);
                    resBuilder.setPoints(user.getTotalPoints());
                    cacheService.SaveUser(user);
                });
            } else {
                resBuilder.setStatus(AuthStatus.FAILURE);
            }
            return resBuilder.build();
        });
    }
    @Override
    public CompletionStage<QuizSummaryResponse> quizScore(UserCredentials in) {
        var responseBuilder = QuizSummaryResponse.newBuilder();
        var token = in.getToken();
        var name = in.getUserName();
        var user = cacheService.findByLogin(name);
        if (tokenService.validarToken(token) && user != null) {
            responseBuilder.setCorrectAnswers(user.getCorrectAnswers())
                           .setTotalQuestions(user.getTotalQuestions())
                           .setTotalPoints(user.getTotalPoints())
                           .setStatus(AuthStatus.SUCCESS);
        } else {
            responseBuilder.setCorrectAnswers(-1)
                           .setTotalQuestions(-1)
                           .setTotalPoints(-1)
                           .setStatus(AuthStatus.FAILURE);
        }
        return CompletableFuture.completedStage(responseBuilder.build());
    }
}
