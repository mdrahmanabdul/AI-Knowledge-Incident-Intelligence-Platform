package com.akiip.query.application.port.out;

/**
 * PORT: what the core needs from "an LLM", with no Spring AI / Ollama / Bedrock types.
 * Adapters in adapter/out/llm implement it. Switching Ollama -> Bedrock = a new adapter, same port.
 */
public interface LlmPort {

    String ask(String question);
}
