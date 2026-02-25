package com.uymbot.docservice.service;

import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Deterministic, offline hash-based text embedding.
 * Produces a 384-dimensional L2-normalised float vector from character tri-grams.
 * Suitable for local/offline use and testing.
 * For production, replace with a sentence-transformer model or external API.
 */
@Service
public class EmbeddingService {

    private static final int DIM = 384;

    public float[] embed(String text) {
        float[] vec = new float[DIM];
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            int len = text.length();
            for (int i = 0; i < len - 2; i++) {
                byte[] ngram = text.substring(i, i + 3).getBytes(StandardCharsets.UTF_8);
                byte[] hash = digest.digest(ngram);
                for (int j = 0; j + 3 < hash.length; j += 4) {
                    int idx = (j / 4) % DIM;
                    float val = ByteBuffer.wrap(hash, j, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
                    if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                        vec[idx] += val;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        // L2 normalise
        float norm = 0f;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0f) {
            for (int i = 0; i < DIM; i++) vec[i] /= norm;
        }
        return vec;
    }
}
