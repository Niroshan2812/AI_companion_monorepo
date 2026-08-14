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
    
    def generate_bump_profile(self, raw_history:str) ->str:
        """
        Implements the BUMP framework by compressing raw interaction logs into a Digital Twin profile.
        """

        system_prompt =(
            "You are a psychological profiler. Read the following raw interaction logs between an AI and a human user. "
            "Extract their core personality traits, their typical emotional state, what they enjoy talking about, "
            "and how they prefer to communicate. Write a short, single-paragraph 'Digital Twin' profile describing the user. "
            "Do not include pleasantries, just output the paragraph."
        )

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": f"Here are the raw logs:\n{raw_history}"}
        ]

        try:
            response = self.client.chat.completions.create(
                model=self.model_id,
                messages=messages,
                temperature=0.3,
                max_tokens=300,
            )
            return response.choices[0].message.content.strip()
        
        except Exception as e:
            print(f"BUMP error - Error generating BUMP profile  {e}")
            return "Profile generation failed "

            
    def generate_stream(self, system_context:str, emotion:str, user_prompt:str):

        """
        Generates a streaming response using Groq, injecting the detected emotion into the persona.
        """
        
        # The Persona Engine (This is where the "Human Vibe" comes from)
        # We tell the LLM exactly how to act, and how to react to the user's emotion.

        empathy_instruction = ""
        if emotion in ["sadness", "fear", "anger", "disgust"]:
            empathy_instruction = "EmpRL Objective: Maximize EMOTIONAL REACTION (validate their feeling) and INTERPRETATION (understand their pain). Do not force solutions."
        elif emotion in ["joy", "surprise"]:
            empathy_instruction = "EmpRL Objective: Maximize EXPLORATION (ask questions to expand on their excitement) and share in their energy."
        else:
            empathy_instruction = "EmpRL Objective: Maintain a balanced, natural conversation flow."
        engineered_system_prompt =(
            "You are a highly empathetic, insightful, and natural human companion. "
            "Do not act like a robotic AI assistant. Speak conversationally, show personality, and ask thoughtful questions. "
            f"Here is the context of past conversations and the user's BUMP Digital Twin Profile: {system_context}\n\n"
            f"CRITICAL INSTRUCTION: The user's current emotional state is: {emotion.upper()}. "
            f"{empathy_instruction}"
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
