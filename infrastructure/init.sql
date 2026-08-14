 -- Enable the pgvector extension within the 'companion_db' database.
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the relational table to store user profiles and cron job state.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    proactive_opt_in BOOLEAN DEFAULT TRUE,
    last_interaction_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    digital_twin_profile TEXT DEFAULT ''
);

-- Create the RAG memory table utilizing pgvector's custom data type.
-- The 768 dimension assumes you are using a smaller embedding model (like an all-mpnet or custom BERT). Adjust if using OpenAI (1536).
CREATE TABLE conversation_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    prompt TEXT NOT NULL,
    response TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- Store the dense vector embedding for similarity searches.
    embedding VECTOR(384) 
);

-- Create an HNSW index to optimize Approximate Nearest Neighbor (ANN) vector search speed.
CREATE INDEX idx_conversation_embedding 
ON conversation_memory 
USING hnsw (embedding vector_l2_ops);