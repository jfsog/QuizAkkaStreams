package org.jfsog.quizakkastreams.Service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.jfsog.quizakkastreams.Biscuit.BiscuitTokenService;
import org.jfsog.quizakkastreams.Models.User.Users;
import org.jfsog.quizakkastreams.Repository.UsersRepository;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceValkey {
    private static final String USERS_CACHE = "Users:Cache";
    private static final String USERS_TOKENS_CACHE = "Users:Cache:tokens";
    private static final String USERS_BLOOM_FILTER = "Users:BloomFilter:login";
    private static final long BLOOM_FILTER_EXPECTED_INSERTIONS = 1_000_000L;
    private static final double BLOOM_FILTER_FALSE_POSITIVE_RATE = 0.15;
    private static final long CACHE_EXPIRATION_MINUTES = 1L;
    private final RedissonClient redisson;
    private final UsersRepository usersRepository;
    private final BiscuitTokenService biscuitTokenService;
    @Getter
    private RBloomFilter<String> bloomFilter;
    @Autowired
    public CacheServiceValkey(RedissonClient redisson,
                              UsersRepository usersRepository,
                              BiscuitTokenService biscuitTokenService) {
        this.redisson = redisson;
        this.usersRepository = usersRepository;
        this.biscuitTokenService = biscuitTokenService;
    }
    private RMapCache<String, Users> getUsersCache() {
        return redisson.getMapCache(USERS_CACHE);
    }
    @PostConstruct
    private void initBloomFilter() {
        bloomFilter = redisson.getBloomFilter(USERS_BLOOM_FILTER, StringCodec.INSTANCE);
        bloomFilter.tryInitAsync(BLOOM_FILTER_EXPECTED_INSERTIONS, BLOOM_FILTER_FALSE_POSITIVE_RATE);
    }
    private RMapCache<String, String> getUserTokensCache() {
        return redisson.getMapCache(USERS_TOKENS_CACHE, StringCodec.INSTANCE);
    }
    public String getUserToken(Users user) {
        var temp = getUserTokensCache();
        return Optional.ofNullable(temp.get(user.getLogin()))
                       .filter(biscuitTokenService::validarToken)
                       .orElseGet(() -> temp.put(user.getLogin(), biscuitTokenService.createUserToken(user)));
    }
    @Transactional
    public Users SaveUser(Users user) {
        getBloomFilter().add(user.getLogin());
        getUsersCache().fastPut(user.getLogin(), user, CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        usersRepository.save(user);
        return user;
    }
    public Users findByLogin(String login) {
        if (!getBloomFilter().contains(login)) return null;
        return Optional.ofNullable(getUsersCache().get(login)).orElseGet(() -> {
            var user = usersRepository.findByLogin(login);
            if (user != null) getUsersCache().put(login, user);
            return user;
        });
    }
}
