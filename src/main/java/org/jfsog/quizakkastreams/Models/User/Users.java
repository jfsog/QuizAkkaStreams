package org.jfsog.quizakkastreams.Models.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.jfsog.grpc_quiz.v1.quiz.UserRole;

@Entity
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
    public Users(String login, String password, UserRole role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }
}
