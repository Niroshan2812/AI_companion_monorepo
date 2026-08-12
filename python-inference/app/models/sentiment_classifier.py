
import torch
from transformers import pipeline

class SentimentClassifier:
    def __init__(self):

        # initialize HF pipeline for text classification 
        print("Nural engine : Loading DistilRoBERTa emotional classifier into memory " ) 

        self.classifier = pipeline(
            task="text-classification", 
            model="j-hartmann/emotion-english-distilroberta-base", 
            top_k=1 # Only return the highest probability emotion
        )

        print("Nural Engine: Model loaded Sucessfylly")


    def anlyze_emotion(self, text:str) ->str:
        #hanfle edge to prevent tensor shape errors
        if not text or len(text.strip()) == 0:
            return "neutral"

        # pass tthe sanitized text through the transformer model
        # the pipleine handle the automatically tokenization and PyTorch tensor converion

        try:
            prediction = self.classifier(text)
            # Extract the top predicted emotion label from the output dictionary 
            dominat_emotion = prediction[0][0]['label']
            return dominat_emotion

        except Exception as e:
            print(f"Nural Engine error:  Classification failed {e}")
