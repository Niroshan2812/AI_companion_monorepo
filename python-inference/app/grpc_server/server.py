import asyncio
import time
from concurrent import futures
import grpc

import ai_companion_pb2
import ai_companion_pb2_grpc


class InferenceServiceServicer(ai_companion_pb2_grpc.InferenceServiceServicer):

    # overide the streamTokens RPC method in the .proto file 
    async def StreamTokens(self, request, context):
        # Extract fields from  the inbound binary request 
        user_id = request.user_id
        prompt = request.sanitized_prompt

        print (f"[gRPC Server] Recevied request from User: {user_id} | Prompt: '{prompt}'")

        # Simulate the token stream 
        mock_response_tokens = [
            "Hello! ", "I ", "am ", "Sara. ", "I ", "received ", 
            "your ", "message: ", f"'{prompt}'. ", "How ", "can ", 
            "I ", "help ", "you ", "today?"
        ]


        # yield tokens one by one to maintain async server-streaming over gRPC
        for token  in mock_response_tokens:
            chunk = ai_companion_pb2.TokenChunk(
                token = token, 
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

    # register servicer implmentation with the server instance
    ai_companion_pb2_grpc.add_InferenceServiceServicer_to_server(
        InferenceServiceServicer(),server
    )

    # bind to an internal port 
    listen_addr = '[::]:50051'
    server.add_insecure_port(listen_addr)

    print(f"[gRPC Server] Service initialized and listening on {listen_addr}...")
    await server.start()
    await server.wait_for_termination()


if __name__ =='__main__':
    asyncio.run(serve())