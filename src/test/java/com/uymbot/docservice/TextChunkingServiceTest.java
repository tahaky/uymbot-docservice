package com.uymbot.docservice;

import com.uymbot.docservice.service.TextChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    private TextChunkingService service;

    @BeforeEach
    void setUp() {
        service = new TextChunkingService();
        // Set chunk size to 10 tokens → 40 chars (for easy testing)
        ReflectionTestUtils.setField(service, "chunkSizeTokens", 10);
    }

    @Test
    void nullText_returnsEmptyList() {
        assertThat(service.split(null)).isEmpty();
    }

    @Test
    void blankText_returnsEmptyList() {
        assertThat(service.split("   ")).isEmpty();
    }

    @Test
    void shortText_returnsSingleChunk() {
        String text = "Short text.";
        List<String> chunks = service.split(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Short text.");
    }

    @Test
    void textExactlyAtLimit_returnsSingleChunk() {
        // 40 chars exactly (10 tokens * 4 chars/token)
        String text = "a".repeat(40);
        List<String> chunks = service.split(text);
        assertThat(chunks).hasSize(1);
    }

    @Test
    void longText_splitsIntoMultipleChunks() {
        // Build text of ~200 chars → should produce at least 5 chunks with limit=40
        String text = "word ".repeat(50); // 250 chars
        List<String> chunks = service.split(text);
        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk must be within limit
        chunks.forEach(c -> assertThat(c.length()).isLessThanOrEqualTo(40));
    }

    @Test
    void paragraphBoundariesAreRespected() {
        // Two paragraphs, each 30 chars — both fit within 40-char limit individually,
        // but together (60 chars) exceed it → should become two chunks
        String para1 = "First paragraph text here."; // 26 chars
        String para2 = "Second paragraph text here."; // 27 chars
        String text = para1 + "\n\n" + para2;

        List<String> chunks = service.split(text);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).isEqualTo(para1);
        assertThat(chunks.get(1)).isEqualTo(para2);
    }

    @Test
    void allChunksAreNonBlank() {
        String text = "Hello world.\n\nFoo bar baz.\n\n" + "Long sentence that definitely goes well beyond the limit. ".repeat(5);
        List<String> chunks = service.split(text);
        chunks.forEach(c -> assertThat(c).isNotBlank());
    }

    @Test
    void defaultChunkSize_handlesTypicalDocument() {
        // Use default chunk size (1000 tokens = 4000 chars)
        TextChunkingService defaultService = new TextChunkingService();
        ReflectionTestUtils.setField(defaultService, "chunkSizeTokens", 1000);

        String shortDoc = "This is a normal document that fits in a single chunk.";
        assertThat(defaultService.split(shortDoc)).hasSize(1);

        // A large document (~10 000 chars) should be split into multiple chunks
        String largeDoc = "paragraph content here. ".repeat(500); // ~12 000 chars
        List<String> chunks = defaultService.split(largeDoc);
        assertThat(chunks).hasSizeGreaterThan(1);
        chunks.forEach(c -> assertThat(c.length()).isLessThanOrEqualTo(4000));
    }
}
