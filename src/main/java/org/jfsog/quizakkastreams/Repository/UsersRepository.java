package org.jfsog.quizakkastreams.Repository;

import org.jfsog.quizakkastreams.Models.User.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, String> {}
