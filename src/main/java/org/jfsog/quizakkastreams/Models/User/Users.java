package org.jfsog.quizakkastreams.Models.User;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Data
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table()
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(unique = true, nullable = false)
    private String id;
    @Column(unique = true, nullable = false)
    private String login;
    @NotNull
    private String password;
    @NotNull
    private UserRole role = UserRole.USER;
    public Users(String login, String password, UserRole role) {
        this.login = login;
        this.password = password;
        this.role = role;
    }

}
