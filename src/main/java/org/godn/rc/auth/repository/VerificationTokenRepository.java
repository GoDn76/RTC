package org.godn.rc.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
@Repository
public class VerificationTokenRepository {
    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "verification_token:";

    public VerificationTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String token, String userId, Long expirationInMinutes) {
        redisTemplate.opsForValue()
                .set(PREFIX + userId, token, Duration.ofMinutes(expirationInMinutes));
    }

    public String getToken(String userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }


    public void delete(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}