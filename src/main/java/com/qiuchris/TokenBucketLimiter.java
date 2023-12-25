package com.qiuchris;

import java.util.HashMap;
import java.util.Map;

public class TokenBucketLimiter {
    private final long maxTokens;
    private final long leakRate;
    private final Map<String, Bucket> buckets = new HashMap<>();

    public TokenBucketLimiter(long maxTokens, long leakRate) {
        this.maxTokens = maxTokens;
        this.leakRate = leakRate;
    }

    public synchronized boolean canUseToken(String userId) {
        Bucket b = buckets.computeIfAbsent(userId, k -> new Bucket());
        return b.tokens > 0;
    }

    public synchronized void useToken(String userId) {
        Bucket b = buckets.computeIfAbsent(userId, k -> new Bucket());
        long now = System.currentTimeMillis();
        long elapsedTime = now - b.lastFilled;
        b.tokens = Math.min(b.tokens + (elapsedTime / leakRate), maxTokens);
        b.lastFilled = now;

        if (b.tokens > 0) {
            b.tokens--;
        }
    }

    private class Bucket {
        long tokens = maxTokens;
        long lastFilled = System.currentTimeMillis();
    }
}
