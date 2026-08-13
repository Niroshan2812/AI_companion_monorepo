import asyncio
import time
from concurrent import futures
import grpc

from app.grpc_server import ai_companion_pb2
from app.grpc_server import ai_companion_pb2_grpc
from app.models.sentiment_classifier import SentimentClassifier
#from app.models.llm_generator import LLMGenerator
from app.models.groq_generator import GroqGenerator



class InferenceServiceServicer(ai_companion_pb2_grpc.InferenceServiceServicer):

    # inject the  Deep leaning model into gRPC service via composition
    def __init__ (self, sentiment_model: SentimentClassifier, llm_model: GroqGenerator ):
        self.llm_model = llm_model
        self.sentiment_model = sentiment_model


    # overide the streamTokens RPC method in the .proto file 
    async def StreamTokens(self, request, context):
        # Extract fields from  the inbound binary request 
        user_id = request.user_id
        prompt = request.sanitized_prompt

        # Extract the R2DBC db state injected by the Java API gateway 
        db_context = request.vector_context

        print(f"gRPC service - context {db_context} | prompt {prompt}")

        # classify user emotion 
        emotion = self.sentiment_model.anlyze_emotion(prompt)
        print(f"Nural engine - Detected emotion{emotion}")

        # trigger asyncc LLM generation thread 
        streamer = self.llm_model.generate_stream(db_context, emotion, prompt)

        
        # yield tokens one by one to maintain async server-streaming over gRPC
        # Iterate over the thread-safe token queue as the GPU yields them.
        for new_token  in streamer:
            if new_token:
                chunk = ai_companion_pb2.TokenChunk(
                    token=new_token, 
                    is_complete=False
                )
                yield chunk
                # contol back asyncio event looop prevent blocking
                await asyncio.sleep(0.01)

        # signal the java netty stream that tensor generation is fully comleate             
        # send the completino single token
        yield ai_companion_pb2.TokenChunk(token="", is_complete = True)

async def serve():
    # Instantiate an async gRPC server
    server = grpc.aio.server()

    sentiment_classifier = SentimentClassifier()
    llm_genetator = GroqGenerator()

    ai_companion_pb2_grpc.add_InferenceServiceServicer_to_server(
        InferenceServiceServicer(sentiment_classifier, llm_genetator),server
    )

    # bind to an internal port 
    #listen_addr = '[::]:50051'
    listen_addr = '127.0.0.1:50051'
    server.add_insecure_port(listen_addr)

    print(f"[gRPC Server] Service initialized and listening on {listen_addr}...")
    await server.start()
    await server.wait_for_termination()


if __name__ =='__main__':
    asyncio.run(serve())