package org.godn.rc.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
public class PasswordResetTokenRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "password_reset:";

    public PasswordResetTokenRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String token, String userId) {
        redisTemplate.opsForValue()
                .set(PREFIX + userId, token, Duration.ofMinutes(15));
    }

    public String getToken(String userId) {
        return redisTemplate.opsForValue().get(PREFIX + userId);
    }


    public void delete(String userId) {
        redisTemplate.delete(PREFIX + userId);
    }
}