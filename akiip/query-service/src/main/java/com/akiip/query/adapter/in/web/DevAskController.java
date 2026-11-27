package com.akiip.query.adapter.in.web;

import com.akiip.query.application.port.out.LlmPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Day 2 smoke test: proves Spring AI can reach the LLM.
 * Try: http://localhost:8081/api/v1/dev/ask?q=What is Apache Kafka?
 * (No RAG yet: the model answers from its own knowledge. Retrieval starts Day 10.)
 */
@RestController
@RequestMapping("/api/v1/dev")
class DevAskController {

    private final LlmPort llm;

    DevAskController(LlmPort llm) {
        this.llm = llm;
    }

    @GetMapping("/ask")
    Map<String, Object> ask(@RequestParam("q") String question) {
        Instant start = Instant.now();
        String answer = llm.ask(question);
        return Map.of(
                "question", question,
                "answer", answer,
                "latencyMs", Duration.between(start, Instant.now()).toMillis());
    }
}
