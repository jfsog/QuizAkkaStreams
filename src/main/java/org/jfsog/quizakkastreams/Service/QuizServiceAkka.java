package org.jfsog.quizakkastreams.Service;

import akka.NotUsed;
import akka.grpc.javadsl.AkkaGrpcClient;
import akka.stream.javadsl.Source;
import org.jfsog.grpc_quiz.v1.quiz.QuizRequest;
import org.jfsog.grpc_quiz.v1.quiz.QuizResponse;
import org.jfsog.grpc_quiz.v1.quiz.QuizService;

public class QuizServiceAkka implements QuizService {
    @Override
    public Source<QuizResponse, NotUsed> playQuiz(Source<QuizRequest, NotUsed> in) {
        return null;
    }
}
