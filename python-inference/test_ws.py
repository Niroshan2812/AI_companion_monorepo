import asyncio
import websockets

async def test():
    async with websockets.connect('ws://127.0.0.1:8080/ws/companion?token=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0LXVzZXItdXVpZC0xMjM0IiwiaWF0IjoxNzg2NDk2NTM1LCJleHAiOjE3ODY1ODI5MzV9.Z-3KPVpI1NRIoO80grqt0dntjQ0Sa7_9IQvvAmH1DjID9tEgg3mhxfm3qNMiqip6oiNaeaJfKBr3MmChSRpZlg') as ws:
        await ws.send('hello sara')
        print(await ws.recv())

asyncio.run(test())
