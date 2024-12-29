package org.jfsog.quizakkastreams.Models.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jfsog.grpc_quiz.v1.quiz.UserRole;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false)
    private String id;
    @Column(unique = true, nullable = false)
    private String login;
    @NotNull
    private String password;
    private UserRole role = UserRole.USER;
    private Integer totalQuestions = 0;
    private Integer correctAnswers = 0;
    private Long totalPoints = 0L;
    public Users(String login, String password, UserRole role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }
}
