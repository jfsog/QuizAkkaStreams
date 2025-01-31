package org.jfsog.quizakkastreams.Models.Pergunta;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jfsog.grpc_quiz.v1.quiz.Question;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pergunta {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @NotNull(message = "O texto da pergunta não pode ser nulo.")
    private String enunciado;
    @ElementCollection(fetch = FetchType.EAGER)
    @Size(min = 2, max = 5, message = "A questão deve ter entre duas e cinco alternativas.")
    private Set<String> alternativas;
    @NotNull(message = "A resposta correta não pode ser nula.")
    private String resposta;
    @NotNull(message = "A disciplina não pode ser nula.")
    private String disciplina;
    public static Pergunta fromQuestion(Question q) {
        var p = Pergunta.builder()
                        .disciplina(q.getDisciplina())
                        .enunciado(q.getEnunciado())
                        .alternativas(new HashSet<>(q.getAlternativasList()))
                        .build();
        p.setResposta(q.getAnswer());
        return p;
    }
    public Question toQuestion() {
        return Question.newBuilder()
                       .setDisciplina(disciplina)
                       .setEnunciado(enunciado)
                       .addAllAlternativas(alternativas)
                       .setAnswer(resposta)
                       .build();
    }
    public void setResposta(@NotNull(message = "A resposta correta não pode ser nula.") String resposta) {
        if (this.resposta != null) alternativas.remove(this.resposta);
        alternativas.add(this.resposta = resposta);
    }
}
