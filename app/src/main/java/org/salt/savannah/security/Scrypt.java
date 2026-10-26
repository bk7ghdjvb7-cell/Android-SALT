package org.salt.savannah.security;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 7914 scrypt. Memory-hard password and chat-key stretching. */
public final class Scrypt {
    private Scrypt() {}

    public static byte[] derive(byte[] password, byte[] salt, int n, int r, int p, int dkLen) {
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("N must be a power of two");
        }
        if (r < 1 || p < 1 || dkLen < 1) {
            throw new IllegalArgumentException("invalid scrypt parameters");
        }
        int mfLen = 128 * r;
        byte[] block = pbkdf2(password, salt, 1, p * mfLen);
        for (int i = 0; i < p; i++) {
            romix(block, i * mfLen, n, r);
        }
        return pbkdf2(password, block, 1, dkLen);
    }

    private static void romix(byte[] block, int offset, int n, int r) {
        int length = 128 * r;
        byte[] x = Arrays.copyOfRange(block, offset, offset + length);
        byte[] v = new byte[n * length];
        for (int i = 0; i < n; i++) {
            System.arraycopy(x, 0, v, i * length, length);
            blockMix(x, r);
        }
        for (int i = 0; i < n; i++) {
            int j = integerify(x, r) & (n - 1);
            xor(x, v, j * length);
            blockMix(x, r);
        }
        System.arraycopy(x, 0, block, offset, length);
    }

    private static void blockMix(byte[] block, int r) {
        int blocks = 2 * r;
        byte[] x = Arrays.copyOfRange(block, (blocks - 1) * 64, blocks * 64);
        byte[] y = new byte[blocks * 64];
        for (int i = 0; i < blocks; i++) {
            xorInto(x, block, i * 64);
            salsa208(x);
            System.arraycopy(x, 0, y, i * 64, 64);
        }
        for (int i = 0; i < r; i++) {
            System.arraycopy(y, (i * 2) * 64, block, i * 64, 64);
        }
        for (int i = 0; i < r; i++) {
            System.arraycopy(y, (i * 2 + 1) * 64, block, (i + r) * 64, 64);
        }
    }

    private static int integerify(byte[] block, int r) {
        int offset = (2 * r - 1) * 64;
        return (block[offset] & 0xff)
            | ((block[offset + 1] & 0xff) << 8)
            | ((block[offset + 2] & 0xff) << 16)
            | ((block[offset + 3] & 0xff) << 24);
    }

    private static void xor(byte[] target, byte[] source, int sourceOffset) {
        for (int i = 0; i < target.length; i++) {
            target[i] ^= source[sourceOffset + i];
        }
    }

    private static void xorInto(byte[] target, byte[] source, int sourceOffset) {
        for (int i = 0; i < 64; i++) {
            target[i] ^= source[sourceOffset + i];
        }
    }

    private static void salsa208(byte[] block) {
        int[] b = new int[16];
        for (int i = 0; i < 16; i++) {
            b[i] = (block[i * 4] & 0xff)
                | ((block[i * 4 + 1] & 0xff) << 8)
                | ((block[i * 4 + 2] & 0xff) << 16)
                | ((block[i * 4 + 3] & 0xff) << 24);
        }
        int[] x = b.clone();
        for (int i = 0; i < 8; i += 2) {
            x[4] ^= Integer.rotateLeft(x[0] + x[12], 7);
            x[8] ^= Integer.rotateLeft(x[4] + x[0], 9);
            x[12] ^= Integer.rotateLeft(x[8] + x[4], 13);
            x[0] ^= Integer.rotateLeft(x[12] + x[8], 18);
            x[9] ^= Integer.rotateLeft(x[5] + x[1], 7);
            x[13] ^= Integer.rotateLeft(x[9] + x[5], 9);
            x[1] ^= Integer.rotateLeft(x[13] + x[9], 13);
            x[5] ^= Integer.rotateLeft(x[1] + x[13], 18);
            x[14] ^= Integer.rotateLeft(x[10] + x[6], 7);
            x[2] ^= Integer.rotateLeft(x[14] + x[10], 9);
            x[6] ^= Integer.rotateLeft(x[2] + x[14], 13);
            x[10] ^= Integer.rotateLeft(x[6] + x[2], 18);
            x[3] ^= Integer.rotateLeft(x[15] + x[11], 7);
            x[7] ^= Integer.rotateLeft(x[3] + x[15], 9);
            x[11] ^= Integer.rotateLeft(x[7] + x[3], 13);
            x[15] ^= Integer.rotateLeft(x[11] + x[7], 18);
            x[1] ^= Integer.rotateLeft(x[0] + x[3], 7);
            x[2] ^= Integer.rotateLeft(x[1] + x[0], 9);
            x[3] ^= Integer.rotateLeft(x[2] + x[1], 13);
            x[0] ^= Integer.rotateLeft(x[3] + x[2], 18);
            x[6] ^= Integer.rotateLeft(x[5] + x[4], 7);
            x[7] ^= Integer.rotateLeft(x[6] + x[5], 9);
            x[4] ^= Integer.rotateLeft(x[7] + x[6], 13);
            x[5] ^= Integer.rotateLeft(x[4] + x[7], 18);
            x[11] ^= Integer.rotateLeft(x[10] + x[9], 7);
            x[8] ^= Integer.rotateLeft(x[11] + x[10], 9);
            x[9] ^= Integer.rotateLeft(x[8] + x[11], 13);
            x[10] ^= Integer.rotateLeft(x[9] + x[8], 18);
            x[12] ^= Integer.rotateLeft(x[15] + x[14], 7);
            x[13] ^= Integer.rotateLeft(x[12] + x[15], 9);
            x[14] ^= Integer.rotateLeft(x[13] + x[12], 13);
            x[15] ^= Integer.rotateLeft(x[14] + x[13], 18);
        }
        for (int i = 0; i < 16; i++) {
            b[i] += x[i];
            block[i * 4] = (byte) b[i];
            block[i * 4 + 1] = (byte) (b[i] >>> 8);
            block[i * 4 + 2] = (byte) (b[i] >>> 16);
            block[i * 4 + 3] = (byte) (b[i] >>> 24);
        }
    }

    static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int dkLen) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(password, "HmacSHA256"));
            int hashLen = mac.getMacLength();
            int blocks = (dkLen + hashLen - 1) / hashLen;
            byte[] output = new byte[dkLen];
            byte[] u = new byte[hashLen];
            byte[] t = new byte[hashLen];
            for (int block = 1; block <= blocks; block++) {
                mac.reset();
                mac.update(salt);
                mac.update(new byte[] {
                    (byte) (block >>> 24),
                    (byte) (block >>> 16),
                    (byte) (block >>> 8),
                    (byte) block
                });
                u = mac.doFinal();
                System.arraycopy(u, 0, t, 0, hashLen);
                for (int i = 1; i < iterations; i++) {
                    mac.reset();
                    u = mac.doFinal(u);
                    for (int j = 0; j < hashLen; j++) {
                        t[j] ^= u[j];
                    }
                }
                int offset = (block - 1) * hashLen;
                int take = Math.min(hashLen, dkLen - offset);
                System.arraycopy(t, 0, output, offset, take);
            }
            return output;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }
}
