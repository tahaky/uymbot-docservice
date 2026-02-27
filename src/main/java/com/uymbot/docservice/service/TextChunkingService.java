package com.uymbot.docservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits long text into chunks that fit within OpenAI's embedding token limit.
 * <p>
 * OpenAI text-embedding-3-small supports up to 8191 tokens per call.
 * We target {@code openai.chunk-size} tokens (default 1000) so that each
 * chunk is well within the limit and retrieval stays precise.
 * Approximation: 1 token ≈ 4 characters (English text).
 * </p>
 * Splitting order: paragraph → sentence → hard character limit.
 */
@Slf4j
@Service
public class TextChunkingService {

    /** Approximate characters per token for English text. */
    private static final int CHARS_PER_TOKEN = 4;

    /** Target chunk size in tokens (configurable). */
    @Value("${openai.chunk-size:1000}")
    private int chunkSizeTokens;

    /**
     * Splits {@code text} into a list of non-blank chunks, each at most
     * {@code chunkSizeTokens * CHARS_PER_TOKEN} characters long.
     *
     * @param text raw document text
     * @return ordered list of chunks; single-element list when the text is
     *         short enough to fit in one chunk
     */
    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int maxChars = chunkSizeTokens * CHARS_PER_TOKEN;

        if (text.length() <= maxChars) {
            return List.of(text.strip());
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        // First split on paragraph boundaries (\n\n or more)
        String[] paragraphs = text.split("\n\n+");

        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }

            if (current.length() + paragraph.length() + 2 <= maxChars) {
                // Fits in the current chunk
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(paragraph);
            } else if (paragraph.length() > maxChars) {
                // Paragraph itself is too large — flush current and split by sentences
                if (current.length() > 0) {
                    chunks.add(current.toString().strip());
                    current = new StringBuilder();
                }
                splitBySentences(paragraph, maxChars, chunks, current);
            } else {
                // Start a new chunk with this paragraph
                if (current.length() > 0) {
                    chunks.add(current.toString().strip());
                }
                current = new StringBuilder(paragraph);
            }
        }

        if (current.length() > 0) {
            chunks.add(current.toString().strip());
        }

        log.debug("Split text ({} chars) into {} chunks (maxChars={})",
                text.length(), chunks.size(), maxChars);
        return chunks;
    }

    /** Splits a single oversized paragraph on sentence boundaries. */
    private void splitBySentences(String text, int maxChars,
                                   List<String> chunks, StringBuilder current) {
        String[] sentences = text.split("(?<=[.!?])\\s+");

        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }

            if (current.length() + sentence.length() + 1 <= maxChars) {
                if (current.length() > 0) {
                    current.append(" ");
                }
                current.append(sentence);
            } else if (sentence.length() > maxChars) {
                // Single sentence is longer than the limit — hard split
                if (current.length() > 0) {
                    chunks.add(current.toString().strip());
                    current.delete(0, current.length());
                }
                for (int i = 0; i < sentence.length(); i += maxChars) {
                    chunks.add(sentence.substring(i, Math.min(i + maxChars, sentence.length())).strip());
                }
            } else {
                if (current.length() > 0) {
                    chunks.add(current.toString().strip());
                    current.delete(0, current.length());
                }
                current.append(sentence);
            }
        }
        // remaining text stays in `current` — caller adds it to chunks
    }
}
