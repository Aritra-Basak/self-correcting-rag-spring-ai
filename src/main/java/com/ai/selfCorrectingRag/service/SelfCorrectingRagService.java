package com.ai.selfCorrectingRag.service;

import com.ai.selfCorrectingRag.constant.RagConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfCorrectingRagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public String executeSelfCorrectingPipeline(String query) {
        log.info("=== Starting Self-Correcting RAG Pipeline for query: '{}' ===", query);

        // STEP 1: RETRIEVAL
        log.info("Step 1: Retrieving relevant documents...");
        List<Document> retrievedDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(4).build()
        );

        if (retrievedDocs.isEmpty()) {
            return "No relevant documents found in the database to answer your query.";
        }

        String contextText = retrievedDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // STEP 2: GUARDRAIL AGENT (Relevance Check)
        log.info("Step 2: Guardrail Agent evaluating relevance...");
        PromptTemplate guardrailTemplate = new PromptTemplate(RagConstants.GUARDRAIL_PROMPT);
        String guardrailMessage = guardrailTemplate.render(Map.of("context", contextText, "query", query));

        String guardrailDecision = chatClient.prompt()
                .user(guardrailMessage)
                .call()
                .content()
                .trim()
                .toUpperCase();

        log.info("Guardrail Decision: {}", guardrailDecision);

        if (!guardrailDecision.contains("YES")) {
            return "The retrieved documents do not contain the relevant information to answer your query. Pipeline stopped to prevent hallucination.";
        }

        // STEP 3 & 4: GENERATION AND EVALUATION RETRY LOOP
        for (int attempt = 1; attempt <= RagConstants.MAX_RETRIES; attempt++) {
            log.info("Step 3: Generator Agent drafting answer (Attempt {} of {})", attempt, RagConstants.MAX_RETRIES);

            // Choose the prompt based on the attempt number
            String currentSystemPrompt = (attempt == 1) ? RagConstants.GENERATOR_PROMPT : RagConstants.GENERATOR_RETRY_PROMPT;

            String generatedAnswer = chatClient.prompt()
                    .system(s -> s.text(currentSystemPrompt).param("context", contextText))
                    .user(query)
                    .call()
                    .content();

            log.info("Step 4: Evaluator Agent checking for factual consistency...");
            PromptTemplate evaluatorTemplate = new PromptTemplate(RagConstants.EVALUATOR_PROMPT);
            String evaluatorMessage = evaluatorTemplate.render(Map.of("context", contextText, "answer", generatedAnswer));

            String evaluatorDecision = chatClient.prompt()
                    .user(evaluatorMessage)
                    .call()
                    .content()
                    .trim()
                    .toUpperCase();

            log.info("Evaluator Decision (Attempt {}): {}", attempt, evaluatorDecision);

            if (evaluatorDecision.contains("YES")) {
                log.info("=== Pipeline Completed Successfully on attempt {} ===", attempt);
                return generatedAnswer;
            } else {
                log.warn("Hallucination detected on attempt {}. Answer rejected.", attempt);
            }
        }

        // FALLBACK: If the loop finishes and max retries are hit without a 'YES'
        log.error("=== Pipeline Failed: Maximum retries ({}) reached. System could not generate a hallucination-free answer. ===", RagConstants.MAX_RETRIES);
        return "I apologize, but I am struggling to generate an answer based strictly on the provided documents without making assumptions. Please try rephrasing your query or providing more specific documents.";
    }
}