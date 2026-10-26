package org.salt.savannah.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class SecurityCoreTest {
    public static void main(String[] args) {
        byte[] vector = Scrypt.derive(
            "password".getBytes(StandardCharsets.UTF_8),
            "NaCl".getBytes(StandardCharsets.UTF_8),
            1024,
            8,
            16,
            64
        );
        String actual = hex(vector);
        String expected = "fdbabe1c9d3472007856e7190d01e9fe"
            + "7c6ad7cbc8237830e77376634b373162"
            + "2eaf30d92e22a3886ff109279d9830da"
            + "c727afb94a83ee6d8360cbdfa2cc0640";
        if (!actual.equals(expected)) {
            throw new AssertionError("scrypt vector mismatch " + actual);
        }

        byte[] salt = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        byte[] key = SaltCrypto.chatKey("correct horse battery", salt);
        if (!hex(key).equals("8955628e6f9fb9d3afb73742f6c0875f6b6f8039bbe66c9af58925f60203261d")) {
            throw new AssertionError("chat key mismatch " + hex(key));
        }
        String token = SaltCrypto.seal("hello".getBytes(StandardCharsets.UTF_8), key);
        if (!token.startsWith("enc:v1:")) {
            throw new AssertionError("missing ciphertext prefix");
        }
        byte[] opened = SaltCrypto.open(token, key);
        if (!Arrays.equals(opened, "hello".getBytes(StandardCharsets.UTF_8))) {
            throw new AssertionError("gcm round trip failed");
        }
        boolean rejected = false;
        try {
            SaltCrypto.open(token, SaltCrypto.chatKey("wrong horse battery!", salt));
        } catch (SecurityException ex) {
            rejected = true;
        }
        if (!rejected) {
            throw new AssertionError("wrong key opened ciphertext");
        }

        Set<String> members = new HashSet<>();
        members.add("ada");
        KeyAttemptPolicy policy = new KeyAttemptPolicy();
        policy.wrappedKey = token;
        long now = 1_000_000L;
        if (policy.attempt(false, now, "ada", members::remove) != KeyAttemptPolicy.Result.REJECTED) {
            throw new AssertionError("first failure");
        }
        policy.attempt(false, now, "ada", members::remove);
        if (policy.attempt(false, now, "ada", members::remove) != KeyAttemptPolicy.Result.DELAYED) {
            throw new AssertionError("third failure should delay");
        }
        if (policy.attempt(false, now, "ada", members::remove) != KeyAttemptPolicy.Result.DELAYED) {
            throw new AssertionError("delay should hold");
        }
        long later = policy.lockedUntilMillis + 1;
        if (policy.attempt(false, later, "ada", members::remove) != KeyAttemptPolicy.Result.REJECTED) {
            throw new AssertionError("first post-delay failure");
        }
        if (policy.attempt(false, later, "ada", members::remove) != KeyAttemptPolicy.Result.BLOCKED) {
            throw new AssertionError("second post-delay failure should block");
        }
        if (!policy.wrappedKey.isEmpty() || members.contains("ada")) {
            throw new AssertionError("blocked user kept a key");
        }

        LoginGuard guard = new LoginGuard();
        for (int i = 0; i < 5; i++) {
            guard.begin("Ada@Example.com", now);
            guard.record("ada@example.com", false, now);
        }
        boolean limited = false;
        try {
            guard.begin("ada@example.com", now);
        } catch (SecurityException ex) {
            limited = true;
        }
        if (!limited) {
            throw new AssertionError("login limit");
        }

        PostLimiter posts = new PostLimiter();
        posts.consume("member", now);
        posts.consume("member", now);
        posts.consume("member", now);
        boolean postLimited = false;
        try {
            posts.consume("member", now);
        } catch (SecurityException ex) {
            postLimited = true;
        }
        if (!postLimited) {
            throw new AssertionError("post limit");
        }
        System.out.println("security-core-ok");
    }

    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder();
        for (byte value : data) {
            out.append(String.format("%02x", value));
        }
        return out.toString();
    }
}
