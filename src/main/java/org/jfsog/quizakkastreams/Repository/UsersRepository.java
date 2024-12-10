package org.jfsog.quizakkastreams.Repository;

import org.jfsog.quizakkastreams.Models.User.Users;
import org.redisson.api.RedissonClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<Users, String> {
    Users findByLogin(String login);
}
