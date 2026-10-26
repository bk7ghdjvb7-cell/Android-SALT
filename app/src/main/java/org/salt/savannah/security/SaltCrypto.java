package org.salt.savannah.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-256-GCM and SHA-512. Chat keys are scrypt outputs and are not stored. */
public final class SaltCrypto {
    public static final String PROOF_LABEL = "salt-key-proof-v1";
    public static final int CHAT_N = 16384;
    public static final int CHAT_R = 8;
    public static final int CHAT_P = 1;

    private SaltCrypto() {}

    public static String sha512Hex(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(data);
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                out.append(String.format("%02x", value));
            }
            return out.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-512 unavailable", ex);
        }
    }

    public static byte[] chatKey(String passphrase, byte[] salt) {
        return Scrypt.derive(passphrase.getBytes(StandardCharsets.UTF_8), salt, CHAT_N, CHAT_R, CHAT_P, 32);
    }

    public static String keyProof(byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key, "HmacSHA512"));
            return Base64.getEncoder().encodeToString(mac.doFinal(PROOF_LABEL.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA512 unavailable", ex);
        }
    }

    public static String seal(byte[] plaintext, byte[] key) {
        try {
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            byte[] boxed = cipher.doFinal(plaintext);
            byte[] combined = new byte[nonce.length + boxed.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(boxed, 0, combined, nonce.length, boxed.length);
            return "enc:v1:" + Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-256-GCM failed", ex);
        }
    }

    public static byte[] open(String token, byte[] key) {
        if (!token.startsWith("enc:v1:")) {
            throw new SecurityException("ciphertext required");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(token.substring(7));
            if (combined.length < 12 + 16) {
                throw new SecurityException("ciphertext required");
            }
            byte[] nonce = Arrays.copyOfRange(combined, 0, 12);
            byte[] boxed = Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            return cipher.doFinal(boxed);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("key rejected");
        }
    }
}
