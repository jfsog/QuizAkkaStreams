package org.jfsog.quizakkastreams.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.jfsog.quizakkastreams.Repository.UsersRepository;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CacheServiceValkey {
    private static final String USERS_CACHE = "Users:Cache";
    private static final String USERS_TOKENS_CACHE = "Users:Cache:tokens";
    private final RedissonClient redisson;
    private final UsersRepository usersRepository;
    private final BiscuitTokenService biscuitTokenService;
    private RMapCache<String, Users> usersCache;
    public RLock getUserLock(String username) {
        return redisson.getLock("lock:" + username);
    }
    @PostConstruct
    private void InitCollections() {
        usersCache = redisson.getMapCache(USERS_CACHE);
        usersRepository.findAll().forEach(u -> usersCache.fastPut(u.getLogin(), u));
        log.info("Init collections");
    }
    private RMapCache<String, String> getUserTokensCache() {
        return redisson.getMapCache(USERS_TOKENS_CACHE, StringCodec.INSTANCE);
    }
    public String getUserToken(Users user) {
        var map = getUserTokensCache();
        var token = map.get(user.getLogin());
        if (token == null) {
            token = biscuitTokenService.createUserToken(user);
            map.put(user.getLogin(), token);
        }
        return token;
    }
    @Transactional
    public Users SaveUser(Users user) {
        var saved = usersRepository.save(user);
        usersCache.fastPut(user.getLogin(), saved);
        return saved;
    }
    @Transactional
    public boolean existsByLogin(String username) {
        return findByLogin(username) != null;
    }
    @Transactional
    public Users findByLogin(String login) {
        return Optional.ofNullable(usersCache.get(login)).orElseGet(() -> {
            var u = usersRepository.findByLogin(login);
            if (u != null) usersCache.fastPut(login, u);
            return u;
        });
    }

    @PreDestroy
    private void cleanUp() {
        redisson.getKeys().flushall();
    }
}
