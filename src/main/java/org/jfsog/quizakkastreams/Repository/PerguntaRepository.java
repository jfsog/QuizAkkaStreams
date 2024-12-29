package org.jfsog.quizakkastreams.Repository;

import org.jfsog.quizakkastreams.Models.Pergunta.Pergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerguntaRepository extends JpaRepository<Pergunta,Long > {
}
