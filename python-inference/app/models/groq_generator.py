import os
from groq import Groq
from dotenv import load_dotenv


load_dotenv()

class GroqGenerator:
    def __init__(self):
        print("Nural Engine - Initialized groq API client ")
        self.client = Groq()
        self.model_id = "llama-3.3-70b-versatile"
        print("Nural engine - Groq client Redy")
    

    def generate_stream(self, system_context:str, emotion:str, user_prompt:str):

        """
        Generates a streaming response using Groq, injecting the detected emotion into the persona.
        """
        
        # The Persona Engine (This is where the "Human Vibe" comes from)
        # We tell the LLM exactly how to act, and how to react to the user's emotion.

        engineered_system_prompt =(
            "You are a highly empathetic, insightful, and natural human companion. "
            "Do not act like a robotic AI assistant. Speak conversationally, show personality, and ask thoughtful questions. "
            f"Here is the context of past conversations: {system_context}\n\n"
            f"CRITICAL INSTRUCTION: The user's current emotional state is: {emotion.upper()}. "
            "You MUST adapt your tone to match or support this emotion. "
            "If they are sad, be comforting and gentle. If they are joyful, match their high energy."
        )

        # build the message array
        message =[
            {"role": "system", "content": engineered_system_prompt},
            {"role": "user", "content": user_prompt}
        ]

        # call the groq api with stream enabled 
        try:
            stream = self.client.chat.completions.create(
                model=self.model_id,
                messages=message,
                temperature=0.7,
                max_tokens=256,
                top_p=0.9,
                stream=True,

            )

            # teild the chunks as they arrived from the netwoek 
            for chunk in stream:
                if chunk.choices[0].delta.content is not None:
                    yield chunk.choices[0].delta.content
        
        except Exception as e:
            print(f"Nural engine error - groq generation failed {e}")
            yield  "I'm having a little trouble connecting right now, give me a second."
