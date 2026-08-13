from sentence_transformers import SentenceTransformer

class EmbeddingGenerator:
    def __init__(self):
        print("Nural Engine - Loading embedding model ")
        self.model = SentenceTransformer('all-MiniLM-L6-v2')
        print("Nural Engine - Loading complete")
        
    def generate(self,text:str) -> list[float]:
        embedding = self.model.encode(text)
        return embedding.tolist()