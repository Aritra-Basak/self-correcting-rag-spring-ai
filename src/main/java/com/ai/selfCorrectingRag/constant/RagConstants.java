package com.ai.selfCorrectingRag.constant;

public final class RagConstants {

    private RagConstants() {
        // Prevent instantiation
    }

    public static final int MAX_RETRIES = 2;

    // --- SYSTEM PROMPTS ---
    public static final String GUARDRAIL_PROMPT = """
        You are a strict relevance evaluator.
        Does the following context contain the necessary information to answer the user's query?
        Reply ONLY with 'YES' or 'NO'. Do not elaborate.
        
        Context: {context}
        Query: {query}
        """;

    public static final String GENERATOR_PROMPT = """
        You are a helpful assistant. Use ONLY the provided context to answer the user's query.
        If the answer is not in the context, say "I don't know based on the provided documents."
        
        Context: {context}
        """;

    public static final String GENERATOR_RETRY_PROMPT = """
        You are a helpful assistant.
        WARNING: Your previous attempt to answer this query was rejected because it contained external facts or hallucinations not found in the context.
        
        You MUST use ONLY the provided context to answer the user's query. Do not rely on outside knowledge.
        
        Context: {context}
        """;

    public static final String EVALUATOR_PROMPT = """
        You are a strict factual consistency evaluator.
        Does the generated answer rely entirely on the provided context, or does it contain hallucinations/external facts?
        Reply ONLY with 'YES' (it is fully supported by context) or 'NO' (it contains hallucinations).
        
        Context: {context}
        Generated Answer: {answer}
        """;
}
