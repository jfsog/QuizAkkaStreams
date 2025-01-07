package org.jfsog.quizakkastreams.Carga;

import io.vavr.Tuple2;
import io.vavr.collection.Stream;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.jfsog.quizakkastreams.Models.Pergunta.Pergunta;
import org.jfsog.quizakkastreams.Repository.PerguntaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class QuestionsLoad {
    private PerguntaRepository perguntaRepository;
//    @PostConstruct
    public void load() {
        int alternativas = 5;
        var m2 = Stream.rangeClosed(1, 100)
                       .shuffle()
                       .zipWith(Stream.iterate(0, i -> i + 1), Tuple2::new)
                       .collect(Collectors.groupingBy(t -> t._2() / alternativas,
                               Collectors.mapping(Tuple2::_1, Collectors.toCollection(TreeSet::new))));
        var perguntas = m2.values()
                          .stream()
                          .map(v -> Pergunta.builder()
                                            .disciplina("Matemática")
                                            .enunciado("Qual o menor número?")
                                            .alternativas(v.stream().map(i -> "" + i).collect(Collectors.toSet()))
                                            .resposta(v.pollFirst() + "")
                                            .build())
                          .toList();
        perguntaRepository.saveAll(perguntas);
        m2.forEach((key, value) -> System.out.println(key + ": " + value));
    }
}
