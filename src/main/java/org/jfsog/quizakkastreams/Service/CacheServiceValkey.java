package org.jfsog.quizakkastreams.Service;

import jakarta.annotation.PostConstruct;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    //    @Getter
    private RBloomFilter<String> bloomFilter;
    private RMapCache<String, Users> usersCache;
    @Autowired
    public CacheServiceValkey(RedissonClient redisson,
                              UsersRepository usersRepository,
                              BiscuitTokenService biscuitTokenService) {
        this.redisson = redisson;
        this.usersRepository = usersRepository;
        this.biscuitTokenService = biscuitTokenService;
    }
    @PostConstruct
    private void InitCollections() {
        RBloomFilter<String> temp = redisson.getBloomFilter(USERS_BLOOM_FILTER, StringCodec.INSTANCE);
        usersCache = redisson.getMapCache(USERS_CACHE);
        temp.tryInit(BLOOM_FILTER_EXPECTED_INSERTIONS, BLOOM_FILTER_FALSE_POSITIVE_RATE);
        bloomFilter = temp;
        usersRepository.findAll().forEach(u -> {
            usersCache.fastPutAsync(u.getId(), u);
            bloomFilter.addAsync(u.getLogin());
        });
    }
    private RMapCache<String, String> getUserTokensCache() {
        return redisson.getMapCache(USERS_TOKENS_CACHE, StringCodec.INSTANCE);
    }
    public String getUserToken(Users user) {
        var temp = getUserTokensCache();
        return Optional.ofNullable(temp.get(user.getLogin()))
                       .filter(biscuitTokenService::validarToken)
                       .orElseGet(() -> {
                           try {
                               return temp.putAsync(user.getLogin(), biscuitTokenService.createUserToken(user)).get();
                           } catch (InterruptedException | ExecutionException e) {
                               throw new RuntimeException(e);
                           }
                       });
    }
    @Transactional
    public Users SaveUser(Users user) {
        bloomFilter.addAsync(user.getLogin());
        usersCache.fastPutAsync(user.getLogin(), user, CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        usersRepository.save(user);
        return user;
    }
    public Users findByLogin(String login) {
        try {
            if (!bloomFilter.containsAsync(login).get(10,TimeUnit.SECONDS)) return null;
            var u = usersCache.getAsync(login).get(10,TimeUnit.SECONDS);
            if (u == null) {
                u = usersRepository.findByLogin(login);
                if (u != null) {
                    return usersCache.putAsync(login, u).get(10,TimeUnit.SECONDS);
                }
            }
            return u;
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
