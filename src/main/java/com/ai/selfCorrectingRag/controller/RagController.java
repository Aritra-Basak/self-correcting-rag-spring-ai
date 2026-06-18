package com.ai.selfCorrectingRag.controller;

import com.ai.selfCorrectingRag.dto.QueryRequest;
import com.ai.selfCorrectingRag.service.DocumentIngestionService;
import com.ai.selfCorrectingRag.service.SelfCorrectingRagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final DocumentIngestionService ingestionService;
    private final SelfCorrectingRagService ragService;

    /**
     * Endpoint to upload and ingest a document into ChromaDB.
     * Uses @RequestPart to cleanly handle the multipart file stream.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadDocument(@RequestPart("file") MultipartFile file) {
        log.info("REST request to upload file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File cannot be empty"));
        }

        ingestionService.processAndStore(file);

        // Returning a JSON structure (Map) is cleaner for frontends than a raw string
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Document '" + file.getOriginalFilename() + "' ingested successfully."
        ));
    }

    /**
     * Endpoint to query the self-correcting RAG pipeline.
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> queryPipeline(@RequestBody QueryRequest request) {
        log.info("REST request to query pipeline with: {}", request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query cannot be empty"));
        }

        String answer = ragService.executeSelfCorrectingPipeline(request.getQuery());

        return ResponseEntity.ok(Map.of(
                "query", request.getQuery(),
                "answer", answer
        ));
    }
}