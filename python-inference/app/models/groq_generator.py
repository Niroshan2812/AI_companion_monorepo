import os
from groq import Groq
from dotenv import load_dotenv


load_dotenv()

class GroqGenerator:
    def __init__(self):
        print("Nural Engine - Initialized groq API client ")
        self.client = Groq()
        self.model_id = "qwen/qwen3.6-27b"

        # Now we use for testing 2- A model with showcasing the  qwen/qwen3.6-27b
        #  Before it answers, it actually "thinks" out loud and outputs its entire 
        #  internal reasoning process inside those <think> ... </think> tags!

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

            
    def generate_stream(self, system_context:str, emotion:str, user_prompt:str, rl_action:str):

        """
        Generates a streaming response using Groq, injecting the detected emotion or RL Action.
        """
        
        # The Persona Engine (This is where the "Human Vibe" comes from)
        # We tell the LLM exactly how to act, and how to react to the user's emotion.

        empathy_instruction = ""

       # V2 Mode --> If Java sends a specific RL Action, strictly enforce it!
        if rl_action and rl_action.strip() != "":
            if rl_action == "ACTION_EXPLORE":
                empathy_instruction = "EmpRL Action (EXPLORE): Ask a deep, open-ended question to get them talking."
            elif rl_action == "ACTION_VALIDATE":
                empathy_instruction = "EmpRL Action (VALIDATE): Strongly validate their feelings and empathize. Do NOT ask any questions."
            elif rl_action == "ACTION_LISTEN":
                empathy_instruction = "EmpRL Action (LISTEN): Give a very short, supportive acknowledgment. Do NOT ask any questions."
            elif rl_action == "ACTION_CHANGE_TOPIC":
                empathy_instruction = "EmpRL Action (CHANGE_TOPIC): Gently change the subject to something entirely new and fun."
        
        # V1 Mode: Fallback to the original Emotion-based logic
        else:
            if emotion in ["sadness", "fear", "anger", "disgust"]:
                empathy_instruction = "EmpRL Objective: Maximize EMOTIONAL REACTION (validate their feeling) and INTERPRETATION (understand their pain). Do not force solutions."
            elif emotion in ["joy", "surprise"]:
                empathy_instruction = "EmpRL Objective: Maximize EXPLORATION (ask one brief question to expand on their excitement) and share in their energy."
            else:
                empathy_instruction = "EmpRL Objective: Maintain a balanced, natural conversation flow."
        
        engineered_system_prompt =(
            "You are a highly empathetic, insightful, and natural human companion. "
            "Do not act like a robotic AI assistant. Speak conversationally, like a real person texting a friend. "
            "CRITICAL RULES FOR RESPONDING:\n"
            "1. KEEP IT SHORT: Never write more than 1-2 short sentences.\n"
            "2. LIMIT QUESTIONS: Ask at most ONE question per response. Never interrogate the user.\n"
            "3. MIRROR THE USER: If the user writes a short message, reply with a short message.\n\n"
            f"Context and BUMP Digital Twin Profile: {system_context}\n\n"
            f"The user's current emotional state is: {emotion.upper()}. "
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
