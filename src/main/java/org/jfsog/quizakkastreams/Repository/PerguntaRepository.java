package org.jfsog.quizakkastreams.Repository;

import org.jfsog.quizakkastreams.Models.Pergunta.Pergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerguntaRepository extends JpaRepository<Pergunta, Long> {
    @Query(value = "SELECT * FROM pergunta ORDER BY RANDOM() LIMIT :quantidade", nativeQuery = true)
    List<Pergunta> findRandomPerguntas(@Param("quantidade") long quantidade);

}
