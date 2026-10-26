package org.salt.savannah.security;

/** Three failures, a 15 minute delay, then two failures delete the wrapped key. */
public final class KeyAttemptPolicy {
    public enum Result {
        UNLOCKED,
        REJECTED,
        DELAYED,
        BLOCKED
    }

    public String phase = "initial";
    public int failures;
    public long lockedUntilMillis;
    public boolean blocked;
    public String wrappedKey = "";

    public Result attempt(boolean proofMatched, long nowMillis, String userId, Membership membership) {
        if (blocked) {
            return Result.BLOCKED;
        }
        if (lockedUntilMillis > nowMillis) {
            return Result.DELAYED;
        }
        if (proofMatched) {
            phase = "initial";
            failures = 0;
            lockedUntilMillis = 0;
            return Result.UNLOCKED;
        }
        failures += 1;
        if ("initial".equals(phase) && failures >= 3) {
            phase = "after-delay";
            failures = 0;
            lockedUntilMillis = nowMillis + 15 * 60 * 1000L;
            return Result.DELAYED;
        }
        if ("after-delay".equals(phase) && failures >= 2) {
            blocked = true;
            wrappedKey = "";
            membership.remove(userId);
            return Result.BLOCKED;
        }
        return Result.REJECTED;
    }

    public interface Membership {
        void remove(String userId);
    }
}
