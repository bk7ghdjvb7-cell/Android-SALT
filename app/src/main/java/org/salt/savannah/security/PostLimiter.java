package org.salt.savannah.security;

import java.util.HashMap;
import java.util.Map;

/** Membership and community public posts: 3 per hour. */
public final class PostLimiter {
    private static final class Bucket {
        long startMillis;
        int count;
    }

    private final Map<String, Bucket> buckets = new HashMap<>();

    public synchronized void consume(String userId, long nowMillis) {
        String key = SaltCrypto.sha512Hex(userId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        if (bucket.startMillis == 0 || nowMillis - bucket.startMillis >= 60 * 60 * 1000L) {
            bucket.startMillis = nowMillis;
            bucket.count = 0;
        }
        if (bucket.count >= 3) {
            throw new SecurityException("post limit");
        }
        bucket.count += 1;
    }
}
