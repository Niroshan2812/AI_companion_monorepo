import asyncio
import time
from concurrent import futures
import grpc

from app.grpc_server import ai_companion_pb2
from app.grpc_server import ai_companion_pb2_grpc
from app.models.sentiment_classifier import SentimentClassifier



class InferenceServiceServicer(ai_companion_pb2_grpc.InferenceServiceServicer):

    # inject the  Deep leaning model into gRPC service via composition
    def __init__ (self, sentiment_model: SentimentClassifier):
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

        # detected tensor outout
        dynamic_responce = f"I sense that you are feeling {emotion}."
        mock_response_tokens = dynamic_responce.split(" ")
        


        # yield tokens one by one to maintain async server-streaming over gRPC
        for token  in mock_response_tokens:
            chunk = ai_companion_pb2.TokenChunk(
                token = token + " ", 
                is_complete = False
            )
            yield chunk
            # simulate autogressive LLM generation latency 
            await asyncio.sleep(0.05)

        # send the completino single token
        yield ai_companion_pb2.TokenChunk(token="", is_complete = True)

async def serve():
    # Instantiate an async gRPC server
    server = grpc.aio.server()

    sentiment_classifier = SentimentClassifier()

    ai_companion_pb2_grpc.add_InferenceServiceServicer_to_server(
        InferenceServiceServicer(sentiment_classifier),server
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