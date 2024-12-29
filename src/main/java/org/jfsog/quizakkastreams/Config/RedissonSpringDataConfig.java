package org.jfsog.quizakkastreams.Config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonSpringDataConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redisson(@Value("${REDIS_PASSWORD}") String password) {
//        var config = Config.fromYAML(Path.of("redisson.yml").toFile());
//        config.useSingleServer().setAddress("redis://localhost:6379");
//        config.useSingleServer().setPassword(password);
        Config config = new Config();
        config.useSingleServer().setPassword(password).setAddress("redis://localhost:6379");
        config.setCodec(JsonJacksonCodec.INSTANCE);
        config.setProtocol(Protocol.RESP3);
//        var str = config.toYAML();
//        System.out.println(str);
        return Redisson.create(config);
    }
}
