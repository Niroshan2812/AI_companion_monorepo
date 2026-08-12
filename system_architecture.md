# Autonomous AI Companion: System Architecture

## 1. Introduction
The core objective of this project is to build an autonomous, state-aware AI companion that goes beyond traditional reactive chatbot paradigms. The system is designed to proactively initiate interactions, recall long-term conversational context, and dynamically adapt to the user's emotional state.

## 2. High-Level Architecture
The system employs a microservices architecture to ensure scalability, isolate workloads (such as heavy machine learning inference), and maintain high performance. The architecture consists of four primary components:
1. **Java API Gateway**: The network edge and orchestrator.
2. **AI Inference Engine**: The Python-based neural execution environment.
3. **Relational Database**: Manages structured user state and telemetry.
4. **Vector Database**: Provides episodic memory for Retrieval-Augmented Generation (RAG).

Communication between the Gateway and the Inference Engine is handled by an **Asynchronous IPC Bridge (gRPC / HTTP/2)**, allowing multiplexed bi-directional streams for fast, chunk-by-chunk token delivery.

---

## 3. Core Components & Responsibilities

### 3.1 Secure API Gateway (Java / Spring WebFlux)
Acts as the central orchestrator and network edge, managing non-blocking I/O via the Netty event loop.
* **Connection Lifecycle Management**: Manages full-duplex WebSocket (`wss://`) connections.
* **Auth & Security Boundaries**: Validates cryptographic JWT signatures to authorize socket upgrades and implements strict input sanitization to neutralize prompt injection vectors.
* **Orchestration & State Machine**: Receives messages, fetches relational state, fetches vector context, invokes the Python engine, and streams responses back to the client.
* **Proactive Scheduling**: Utilizes a Spring `@Scheduled` worker thread to asynchronously poll the relational database for dormant users to trigger proactive conversational loops.
* **Telemetry & Cost Accounting**: Logs token usage, latency metrics, and API consumption to maintain strict cost observability.

### 3.2 AI Inference Engine (Python / PyTorch)
Operates independently of the Java Gateway to bypass the Python Global Interpreter Lock (GIL) and isolate heavy matrix multiplications.
* **Sentiment Pre-processing (Encoder)**: Runs a distilled BERT pipeline (`j-hartmann/emotion-english-distilroberta-base`) to synchronously map raw text into a probability distribution across 7 emotional logits (e.g., joy, sadness, fear). This alters the foundational prompt structure to adjust the AI's tone.
* **Asynchronous Token Generation (Decoder)**: Utilizes an autoregressive LLM (e.g., DialoGPT) running on a background thread to compute causal language tokens.
* **Model Execution & Abstraction**: Loads and executes models (potentially via vLLM/TensorRT) in an isolated environment.
* **Context Window Management**: Processes RAG vector payloads and system prompts, dynamically truncating or summarizing historical context if it exceeds the model's limit.

### 3.3 Relational Database (PostgreSQL / R2DBC)
Accessed via non-blocking R2DBC drivers to query user state without starving Netty threads.
* **Transactional State Management**: Maintains ACID compliance for user data, billing profiles, and application settings.
* **Cron Job Indexing**: Provides optimized B-Tree indexes on fields like `last_interaction_timestamp` so the Java Orchestrator can instantly identify users requiring proactive check-ins.
* **Audit & Telemetry Logging**: Stores immutable logs of API consumption and routing decisions.
* **Data Integrity and Privacy**: Enforces strict foreign key constraints and utilizes Row-Level Security (RLS) to guarantee tenant isolation.

### 3.4 Vector Memory (PostgreSQL + pgvector)
Bypasses stateless amnesia by storing high-dimensional floating-point arrays representing conversational semantic meaning.
* **Episodic Memory Storage**: Persists embeddings of past conversations.
* **High-Speed Similarity Search**: Uses Approximate Nearest Neighbor (ANN) algorithms (like HNSW) and L2 Euclidean distance (`<->`) to rapidly find the most semantically relevant memories to a user's current input.
* **Metadata Filtering**: Combines vector similarity with hard scalar filtering (e.g., `user_id`) to ensure strict data isolation between users during RAG queries.
* **Dynamic Index Updates**: Supports real-time insertions so new conversation vectors are embedded and available for retrieval in the immediate next dialogue turn.

---

## 4. Key Workflows

### 4.1 Reactive Interaction (User-Initiated)
1. **Ingest**: User sends a message via the established WebSocket tunnel.
2. **Sanitize & Auth**: Gateway validates the JWT and sanitizes the input.
3. **Context Retrieval (RAG)**: Gateway queries the Vector DB (`pgvector`) using L2 distance to pull the top 3 most relevant past conversations.
4. **State Retrieval**: Gateway queries the Relational DB for current user state/profile.
5. **IPC Transmission**: Gateway sends the sanitized prompt + context via gRPC to the Python Engine.
6. **Emotional Routing**: Python Engine runs the input through the DistilBERT encoder to classify sentiment and adjusts the prompt accordingly.
7. **Generation**: The Decoder model computes response tokens asynchronously.
8. **Stream**: Tokens are streamed back via gRPC to the Gateway, which multiplexes them down the WebSocket to the user chunk-by-chunk.

### 4.2 Proactive Interaction (System-Initiated Push Notifications)
1. **Monitor**: The Java `@Scheduled` cron job continuously executes non-blocking R2DBC queries against the relational database.
2. **Evaluate**: It checks for users whose `last_interaction_timestamp` exceeds a dormancy threshold (e.g., 24 hours) and who have `proactive_opt_in` enabled.
3. **Trigger**: For matching users, the Gateway independently gathers historical context and triggers a gRPC request to the Python engine to generate a context-aware check-in message.
4. **Push**: The generated message is pushed asynchronously down the established TCP WebSocket tunnel directly to the client without waiting for an incoming frame.
