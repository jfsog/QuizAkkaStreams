package org.jfsog.quizakkastreams.Repository;

import org.jfsog.quizakkastreams.Models.User.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Users findByLogin(String login);
    boolean existsByLogin(String login);
}
