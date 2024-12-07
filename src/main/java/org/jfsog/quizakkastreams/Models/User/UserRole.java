package org.jfsog.quizakkastreams.Models.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {
    ADMIN("admin"), USER("user");
    private final String role;
}
