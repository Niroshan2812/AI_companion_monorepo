import torch
from threading import Thread
from transformers import AutoModelForCausalLM, AutoTokenizer, TextIteratorStreamer


class LLMGenerator:
    def __init__(self):
        # initialize conversation tokenizer and Model weights 
        print("Nural Engine - Loading DialoGPT model")
        self.tokenizer = AutoTokenizer.from_pretrained("microsoft/DialoGPT-small")
        self.model = AutoModelForCausalLM.from_pretrained("microsoft/DialoGPT-small")
        print("Nural Engine - LLM loaded")  

    def generate_stream(self, system_context: str, emotion: str, user_prompt: str):
        # Construct the final prompt injecting RAG memory and the sentiment classification

        engineered_prompt =(
            f"Context: {system_context}\n"
            f"User Emotion: {emotion}\n"
            f"User: {user_prompt}\n"
            f"Companion:"
        )

        # Tokenized the input string into a pyrotch tensor 
        inputs = self.tokenizer([engineered_prompt], return_tensors ="pt")

        # async streamer
        # skip_prompt - true ( only yeild the newly generated AI responce )
        streamer = TextIteratorStreamer(self.tokenizer, skip_prompt=True, skip_special_tokens = True)

        # define the generatioin hyperameters 
        generation_jwargs = dict(
            inputs, 
            streamer = streamer,
            max_new_tokens = 75,
            temperature = 0.7,
            pad_token_id = self.tokenizer.eos_token_id
        )

        # launch the blocking c++/cuda tensor multiplication in a bk thread
        thred = Thread(target=self.model.generate, kwargs=generation_jwargs)
        thred.start()

        return streamer