# Self-Correcting RAG Pipeline with Spring AI & Ollama

A production-grade, agentic **Retrieval-Augmented Generation (RAG)** pipeline built using modern Java with **Spring Boot 3.x**, **Spring AI**, and a local **Ollama** LLM instance. This architecture elevates standard RAG patterns by introducing an autonomous orchestration layer featuring **Guardrail** and **Evaluator** agents that programmatically check for relevance, mitigate hallucinations, and ensure strict factual consistency.

---

## 🏗️ System Architecture & Workflow

## 🔐 Security & Authentication (GitHub OAuth2)

To ensure the RAG workspace remains secure and access-controlled, the application integrates **Spring Security** with **GitHub OAuth2** for seamless, passwordless authentication.

Unauthenticated users are greeted by a minimalistic public landing page. Upon clicking the login trigger, the system initiates the OAuth 2.0 authorization code flow. Once authenticated, Spring Security establishes a secure session (with CSRF protection enabled for the SPA) and seamlessly transitions the UI into the private RAG chat workspace.

### Authentication Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as Frontend (Browser)
    participant Sec as Spring Security
    participant Git as GitHub OAuth2 Provider

    User->>UI: Access Application (http://localhost:8086)
    UI-->>User: Display Public Landing Page
    User->>UI: Click "Sign in with GitHub"
    UI->>Sec: Route to /oauth2/authorization/github
    Sec->>Git: Redirect to GitHub Authorization Endpoint
    
    Note over User,Git: User authenticates securely on GitHub's domain
    
    Git->>Sec: Return Auth Code via Callback (/login/oauth2/code/github)
    Sec->>Git: Back-channel exchange: Code for Access Token
    Git-->>Sec: Return User Profile Details
    
    Note over Sec: Session Established & CSRF Token Generated
    
    Sec-->>UI: Redirect to Secured RAG Workspace (/)
    UI->>Sec: GET /api/user (Verify Session Identity)
    Sec-->>UI: Return GitHub Username (200 OK)
    UI-->>User: Display Authenticated Chat Interface
```
Standard RAG architectures blindly trust whatever context is retrieved from a vector database, often leading to off-topic answers or hallucinations. This system introduces an intelligent, multi-agent validation loop to enforce data reliability before an answer ever reaches the client.

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Postman/Frontend)
    participant API as RAG REST Controller
    participant Store as ChromaDB (Vector Store)
    participant Guard as Agent 1: Guardrail (Relevance)
    participant Gen as Agent 2: Generator (Drafting)
    participant Eval as Agent 3: Evaluator (Fact-Check)

    %% PHASE 1 Triggered implicitly before
    Note over User,Store: PHASE 1: Knowledge Ingestion happens via /upload endpoint

    %% PHASE 2 Execution
    Note over User,Eval: PHASE 2: AGENTIC SELF-CORRECTING QUERY LOOP
    User->>API: POST /query {"query": "..."}
    API->>Store: Vector Similarity Search (Top-K)
    Store-->>API: Return Raw Context Chunks
    
    API->>Guard: Evaluate Relevance (Query + Context)
    alt Context is Irrelevant
        Guard-->>API: Return "NO"
        API-->>User: 200 OK {"status": "Aborted", "message": "Pipeline halted..."}
    else Context is Relevant
        Guard-->>API: Return "YES"
        
        %% Start loop for generation
        loop Self-Correction (Max Retries = 3)
            API->>Gen: Draft Response (Context-Bounded Prompt)
            Gen-->>API: Return Generated Draft Answer
            
            API->>Eval: Audit Answer (Draft Answer + Raw Context)
            
            alt Hallucination Caught
                Eval-->>API: Return "NO"
                Note over API,Gen: State Updated: System alters prompt instructions to correct model
            else Factually Consistent
                Eval-->>API: Return "YES"
                Note over API: Break Loop
            end
        end
        
        API-->>User: 200 OK {"query": "...", "answer": "Validated Answer"}
    end
```


### 🧠 Agentic Layer Breakdown
* **Guardrail Agent (Relevance Check):** Intercepts out-of-domain or malicious prompts. If the context retrieved from the database cannot truthfully answer the user's question, the pipeline is immediately halted to stop the model from making up information.
* **Generator Agent (Contextual Adaptation):** Focuses the LLM (`gemma4:e4b`) entirely on the retrieved data window to synthesize a clean response.
* **Evaluator Agent (Anti-Hallucination Loop):** Acts as a strict gatekeeper by evaluating the generated answer against the raw ground-truth source blocks. If it detects outside knowledge or hallucinations, it updates the state, alters the system prompt instructions, and forces a recalculation up to a maximum of 3 times.

---

## 🛠️ Tech Stack & Prerequisites

| Technology | Purpose | Version |
| :--- | :--- | :--- |
| **Java** | Core Programming Language | 17+ |
| **Spring Boot** | Enterprise Application Framework | 3.5.x |
| **Spring AI** | Fluent AI/LLM & Vector Database Orchestration | 1.1.6 |
| **Ollama** | Local LLM & Embedding Inference Engine | Latest |
| **ChromaDB** | Vector Database Storage | Latest |
| **Docker / WSL2** | Isolated Infrastructure Management | Latest |

---

## 📦 Local Infrastructure Setup

### 1. Model Pulling (Ollama)
Ensure your local Ollama instance is active and pull both the embedding model and generation model:

```bash
# Pull the generation model
ollama pull gemma4:e4b

# Pull the semantic text embedding model
ollama pull nomic-embed-text

# Verify local models are ready
ollama list
```
---

## ⚙️ Project Configuration (`application.yml`)

Configure your `src/main/resources/application.yml` file to securely bind the Spring AI framework auto-configurations to your local infrastructure services:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: YOUR_GITHUB_CLIENT_ID
            client-secret: YOUR_GITHUB_CLIENT_SECRET
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: gemma4:e4b
      embedding:
        model: nomic-embed-text
    vectorstore:
      chroma:
        initialize-schema: true
        client:
          host: http://localhost
          port: 8000
```
OAuth2 Configuration
To enable this feature locally, you must register a new OAuth application in your GitHub Developer Settings with the callback URL set to http://localhost:8086/login/oauth2/code/github.
---

---

## 🚀 REST API Verification Guide

You can interact with and test the self-correcting RAG pipeline using any standard HTTP Client (e.g., Postman or cURL).

### 1. Ingest Knowledge Document
This endpoint accepts multi-format files, extracts the raw data, fragments it into token-split windows, and generates semantic vector embeddings to store in ChromaDB.

* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/v1/rag/upload`
* **Content-Type:** `multipart/form-data`
* **Multipart Body Key:** `file` (Select any `.pdf`, `.txt`, or `.docx` document)

#### 💻 Execution via cURL:
```bash
curl -X POST http://localhost:8080/api/v1/rag/upload \
  -F "file=@/path/to/your/document.pdf"
```
### 2. Execute Self-Correcting Query Loop
This endpoint takes your search query, retrieves the top similarity context blocks from ChromaDB, and routes them through the autonomous Guardrail, Generator, and Evaluator agent loop to compute a verified response.

* **HTTP Method:** `POST`
* **Endpoint URL:** `http://localhost:8080/api/v1/rag/query`
* **Content-Type:** `application/json`
* **JSON Body Key:** `query` (The natural language question you want to pass to the pipeline)

#### 💻 Execution via cURL:
```bash
curl -X POST http://localhost:8080/api/v1/rag/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What are the core metrics outlined in the document?"}'
```
---