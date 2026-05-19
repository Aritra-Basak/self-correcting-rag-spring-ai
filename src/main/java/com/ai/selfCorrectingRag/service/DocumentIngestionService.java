package com.ai.selfCorrectingRag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public void processAndStore(MultipartFile multipartFile) {
        log.info("Starting ingestion for file: {}", multipartFile.getOriginalFilename());

        try {
            // Save multipart file temporarily to read it as a Resource
            File tempFile = File.createTempFile("rag-upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);
            FileSystemResource resource = new FileSystemResource(tempFile);

            // Extract Text using Tika
            TikaDocumentReader documentReader = new TikaDocumentReader(resource);
            List<Document> documents = documentReader.get();
            log.info("Extracted {} raw documents.", documents.size());

            // Split Text into smaller chunks for the Vector Database
            // This prevents overflowing the LLM's context window
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocuments = splitter.apply(documents);
            log.info("Split into {} chunks for embedding.", splitDocuments.size());

            // Generate Embeddings and Store in ChromaDB
            vectorStore.add(splitDocuments);
            log.info("Successfully embedded and stored chunks in ChromaDB.");

            // Cleanup
            tempFile.delete();

        } catch (IOException e) {
            log.error("Failed to process document ingestion", e);
            throw new RuntimeException("Document processing failed", e);
        }
    }
}