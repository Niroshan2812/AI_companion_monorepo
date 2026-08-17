 -- Enable the pgvector extension within the 'companion_db' database.
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the relational table to store user profiles and cron job state.
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    timezone VARCHAR(50) DEFAULT 'UTC',
    proactive_opt_in BOOLEAN DEFAULT TRUE,
    last_interaction_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    digital_twin_profile TEXT DEFAULT '',
    ai_mode VARCHAR(20) DEFAULT 'V1_BUMP'
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
CREATE TABLE IF NOT EXISTS q_table(
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id),
    state VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    q_value DOUBLE PRECISION DEFAULT 0.0,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, state, action)
)

-- Create an HNSW index to optimize Approximate Nearest Neighbor (ANN) vector search speed.
CREATE INDEX idx_conversation_embedding 
ON conversation_memory 
USING hnsw (embedding vector_l2_ops);