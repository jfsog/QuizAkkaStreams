package org.jfsog.quizakkastreams.Models.Pergunta;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    //@lob
    @NotNull(message = "O texto da pergunta não pode ser nulo.")
//    @Column(unique = true)
    private String enunciado;
    @ElementCollection(fetch = FetchType.EAGER)
    @Size(min = 2, max = 5, message = "A questão deve ter entre duas e cinco alternativas.")
    private Set<String> alternativas;
    //@lob
    @NotNull(message = "A resposta correta não pode ser nula.")
    private String resposta;
    //@lob
    @NotNull(message = "A disciplina não pode ser nula.")
    private String disciplina;
    public void setResposta(@NotNull(message = "A resposta correta não pode ser nula.") String resposta) {
        if (this.resposta != null) alternativas.remove(this.resposta);
        alternativas.add(this.resposta = resposta);
    }
}
