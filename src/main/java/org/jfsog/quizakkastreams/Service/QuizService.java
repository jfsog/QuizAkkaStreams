package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfsog.grpc_quiz.v1.quiz.*;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Models.Pergunta.Pergunta;
import org.jfsog.quizakkastreams.Repository.PerguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Slf4j
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
            try {
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
            } catch (Exception e) {
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
        if (tokenService.validarToken(token, "check if user(\"%s\")".formatted(name)) && user != null) {
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
    @Override
    public CompletionStage<QuestionCreateResponse> createQuestion(QuestionCreateRequest in) {
        var cred = in.getUserCredentials();
        var question = in.getQuestion();
        var user = cacheService.findByLogin(cred.getUserName());
        var b = QuestionCreateResponse.newBuilder();
        if (user != null &&
            tokenService.validarToken(cred.getToken(), "check if user(\"%s\")".formatted(user.getLogin()))) {
            var p = Pergunta.fromQuestion(question);
            perguntaRepository.save(p);
            b.setMessage("Pergunta criada com sucesso!").setStatus(AuthStatus.SUCCESS);
        } else {
            b.setMessage("User não autorizado").setStatus((AuthStatus.FAILURE));
        }
        return CompletableFuture.completedStage(b.build());
    }
    @Override
    public CompletionStage<QuestionsResponse> pullQuestions(PullQuestionRequest in) {
        var cred = in.getUserCredentials();
        var name = cred.getUserName();
        var token = cred.getToken();
        var user = cacheService.findByLogin(name);
        var b = QuestionsResponse.newBuilder();
        if (user != null && tokenService.validarToken(token, "check if user(\"%s\")".formatted(name))) {
            b.setStatus(AuthStatus.SUCCESS);
            var listP = perguntaRepository.findRandomPerguntas(in.getN()).stream().map(Pergunta::toQuestion).toList();
            b.addAllQuestion(listP);
        } else {
            b.setStatus(AuthStatus.FAILURE);
        }
        return CompletableFuture.completedStage(b.build());
    }
    @Override
    public CompletionStage<QuestionResponse> pullQuestion(PullQuestionRequest in) {
        var cred = in.getUserCredentials();
        var name = cred.getUserName();
        var token = cred.getToken();
        var user = cacheService.findByLogin(name);
        var b = QuestionResponse.newBuilder();
        if (user != null && tokenService.validarToken(token, "check if user(\"%s\")".formatted(name))) {
            b.setStatus(AuthStatus.SUCCESS);
            perguntaRepository.findById(in.getN()).map(Pergunta::toQuestion).ifPresent(b::setQuestion);
        } else {
            b.setStatus(AuthStatus.FAILURE);
        }
        return CompletableFuture.completedStage(b.build());
    }
}
