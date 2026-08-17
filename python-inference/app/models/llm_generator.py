import torch
from threading import Thread
from transformers import AutoModelForCausalLM, AutoTokenizer, TextIteratorStreamer


class LLMGenerator:
    def __init__(self):
        # initialize conversation tokenizer and Model weights 
        print("Nural Engine - Loading DialoGPT model")
        self.tokenizer = AutoTokenizer.from_pretrained("microsoft/DialoGPT-small" , clean_up_tokenization_spaces = False)
        self.model = AutoModelForCausalLM.from_pretrained("microsoft/DialoGPT-small")
        print("Nural Engine - LLM loaded")  

    def generate_stream(self, system_context: str, emotion: str, user_prompt: str):
        # Construct the final prompt injecting RAG memory and the sentiment classification
        # engineered_prompt =(
        #     f"Context: {system_context}\n"
        #     f"User Emotion: {emotion}\n"
        #     f"User: {user_prompt}\n"
        #     f"Companion:"
        # )

        # format the prompt as a natural sentence also append EOS token so the model knows its turn to reply 
        engineered_prompt = f"{user_prompt} I am currently feeling {emotion}." + self.tokenizer.eos_token

        # Tokenized the input string into a pyrotch tensor 
        inputs = self.tokenizer([engineered_prompt], return_tensors ="pt")

        # async streamer
        # skip_prompt - true ( only yeild the newly generated AI responce )
        streamer = TextIteratorStreamer(self.tokenizer, skip_prompt=True, skip_special_tokens = True)

        # define the generatioin hyperameters 

        # Fix 
#         The model is generating so much internal thought process that it hits our hard-coded
#          max_tokens=256 limit before it ever gets a chance to actually output the final message to the user!  
#        since you want to keep the <think> blocks visible for testing (which is a great idea for debugging the Q-learning actions!), we just need to give the model a much larger token budget so it can finish its thoughts.

        generation_jwargs = dict(
            inputs, 
            streamer = streamer,
            max_new_tokens = 2048,
            do_sample = True, # enabel non-greedy decoding, activate temp 
            temperature = 0.7,
            top_p = 0.9, # only considers tokens within the top 90% cumulative prob mass
            pad_token_id = self.tokenizer.eos_token_id
        )

        # launch the blocking c++/cuda tensor multiplication in a bk thread
        thred = Thread(target=self.model.generate, kwargs=generation_jwargs)
        thred.start()

        return streamer