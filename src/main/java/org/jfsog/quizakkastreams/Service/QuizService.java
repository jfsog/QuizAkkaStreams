package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import lombok.AllArgsConstructor;
import org.jfsog.grpc_quiz.v1.quiz.QuizRequest;
import org.jfsog.grpc_quiz.v1.quiz.QuizResponse;
import org.jfsog.grpc_quiz.v1.quiz.QuizSummaryResponse;
import org.jfsog.grpc_quiz.v1.quiz.UserCredentials;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletionStage;

@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuizService implements org.jfsog.grpc_quiz.v1.quiz.gRPCQuizService{
    private final CacheServiceValkey cacheService;
    @Override
    public Source<QuizResponse, NotUsed> playQuiz(Source<QuizRequest, NotUsed> in) {
        return null;
    }
    @Override
    public CompletionStage<QuizSummaryResponse> quizScore(UserCredentials in) {
        var builder=QuizSummaryResponse.newBuilder();
        var token=in.getToken();
        var name=in.getUserName();
        var user=cacheService.findByLogin(name);
        if(user!=null){

        }
        return null;
    }
}
