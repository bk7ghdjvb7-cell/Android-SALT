package org.salt.savannah.security;

import java.util.HashMap;
import java.util.Map;

/** Five login attempts per 15 minutes. The stored key is a SHA-512 of the email. */
public final class LoginGuard {
    private static final class Bucket {
        long startMillis;
        int count;
    }

    private final Map<String, Bucket> buckets = new HashMap<>();

    public synchronized void begin(String email, long nowMillis) {
        Bucket bucket = buckets.get(storageKey(email));
        if (bucket != null && nowMillis - bucket.startMillis < 15 * 60 * 1000L && bucket.count >= 5) {
            throw new SecurityException("login delayed");
        }
    }

    public synchronized void record(String email, boolean succeeded, long nowMillis) {
        String key = storageKey(email);
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
        boolean fresh = bucket.startMillis == 0 || nowMillis - bucket.startMillis >= 15 * 60 * 1000L;
        if (succeeded || fresh) {
            bucket.startMillis = succeeded ? 0 : nowMillis;
            bucket.count = succeeded ? 0 : 1;
            return;
        }
        bucket.count += 1;
    }

    private static String storageKey(String email) {
        return SaltCrypto.sha512Hex(email.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
