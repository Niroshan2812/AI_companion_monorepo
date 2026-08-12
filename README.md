# Autonomous AI Companion 

Welcome to the **Autonomous AI Companion** repository! This project aims to evolve beyond standard reactive chatbots (which only speak when spoken to) into an autonomous, state-aware AI companion. The architecture is explicitly designed to proactively initiate interactions, recall long-term conversational context, and dynamically adapt to the user's emotional state.

This repository contains the core microservices and components required to build this next-generation AI architecture.

##  Key Features

* **Proactive Scheduling (Push Notifications):** The AI actively monitors user dormancy. If a user is inactive for a set period, the system independently generates and pushes context-aware check-in messages via WebSockets.
* **Reactive Long-Term Memory (RAG):** Eliminates "stateless amnesia". Every interaction is stored as a vector embedding in PostgreSQL (`pgvector`), allowing the system to instantly recall semantically relevant past conversations to maintain continuous context.
* **Emotional Sentiment Routing:** Employs a tensor classification pipeline to map incoming text to 7 emotional states, dynamically altering the AI's foundational prompt to ensure responses are emotionally intelligent and tonally appropriate.
* **Asynchronous High-Performance Architecture:** Bypasses Python's GIL by completely isolating the neural engine from the Java API Gateway, communicating via a high-speed multiplexed gRPC/HTTP2 bridge.

## System Architecture

The project is built on a scalable, event-driven microservices architecture:

1. **Secure API Gateway (Java / Spring WebFlux):** Acts as the network edge, managing full-duplex WebSocket connections via the Netty event loop. It handles JWT authentication, strict prompt sanitization, state orchestration, and proactive cron scheduling.
2. **AI Inference Engine (Python / PyTorch):** Operates independently to run deep learning models. It uses a DistilBERT encoder for synchronous sentiment classification and an autoregressive LLM decoder (e.g., DialoGPT) for streaming token generation.
3. **Relational Database (PostgreSQL):** Manages ACID-compliant transactional state, user profiles, indexing for proactive cron jobs, and telemetry logging using non-blocking R2DBC drivers.
4. **Vector Database (pgvector):** Provides episodic memory storage. It uses Approximate Nearest Neighbor (ANN) algorithms and L2 Euclidean distance to quickly retrieve semantically relevant memories, securely isolated by user.

For a deep dive into the technical design, workflows, and data pipelines, please read the [System Architecture Document](system_architecture.md).

##  Technology Stack

* **API Gateway:** Java, Spring Boot, Spring WebFlux, Netty
* **Inference Engine:** Python, PyTorch, Hugging Face Transformers
* **IPC / Networking:** WebSockets (wss://), gRPC, HTTP/2, Protobuf
* **Databases:** PostgreSQL, pgvector, Spring Data R2DBC
* **Models:** `j-hartmann/emotion-english-distilroberta-base`, Autoregressive LLMs (e.g., DialoGPT) (For testing purposess)

## Roadmap & Milestones

* **[x] Milestone 1: Core Architecture & System Design**
  - Designed the dual-node microservices architecture (Java Gateway + Python Engine).
  - Defined the reactive and proactive interaction workflows.
  - Specified the long-term memory (RAG) and emotional routing pipelines.
* **[ ] Milestone 2: Infrastructure & Gateway Initialization**
  - Initialize Spring WebFlux project and WebSocket tunnels.
  - Set up PostgreSQL with pgvector and R2DBC schemas.
* **[ ] Milestone 3: AI Engine & gRPC Bridge**
  - Implement the PyTorch inference environment.
  - Establish the bi-directional gRPC stream between Java and Python.
* **[ ] Milestone 4: Memory & Proactivity Implementation**
  - Integrate pgvector semantic search for RAG.
  - Implement the Spring `@Scheduled` worker for proactive engagement and vector similarity context injection.

##  Contributing

Contributions, issues, and feature requests are welcome! Since we are in the early stages of development, please open an issue first to discuss any major changes.

##  License

 see the LICENSE file for details.
