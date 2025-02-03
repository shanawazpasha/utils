import asyncio
from sentence_transformers import SentenceTransformer
from transformers import AutoTokenizer, AutoModelForCausalLM, TextIteratorStreamer
import torch
from threading import Thread
import json

# Initialize models with quantization
embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
tokenizer = AutoTokenizer.from_pretrained("TheBloke/Mistral-7B-v0.1-GGUF", use_fast=True)
model = AutoModelForCausalLM.from_pretrained(
    "TheBloke/Mistral-7B-v0.1-GGUF",
    device_map='auto',
    load_in_4bit=True,
    torch_dtype=torch.float16
)

def token_size(text):
    return len(tokenizer.encode(text))

async def get_embedding(input, dimensions=384):
    # Run in executor to avoid blocking
    embedding = await asyncio.get_event_loop().run_in_executor(
        None, embedding_model.encode, input
    )
    return embedding.tolist()

async def get_embeddings(inputs, dimensions=384):
    # Run in executor to avoid blocking
    embeddings = await asyncio.get_event_loop().run_in_executor(
        None, embedding_model.encode, inputs
    )
    return embeddings.tolist()

async def chat_stream(messages, temperature=0.1, **kwargs):
    # Convert messages to prompt format
    prompt = ""
    for msg in messages:
        role = msg["role"]
        content = msg["content"]
        prompt += f"{role}: {content}\nassistant: "

    # Tokenize input
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
    
    # Set up streamer
    streamer = TextIteratorStreamer(tokenizer, timeout=10.0, skip_prompt=True, skip_special_tokens=True)
    
    # Generation config
    generation_kwargs = dict(
        inputs=inputs["input_ids"],
        streamer=streamer,
        max_new_tokens=1024,
        temperature=temperature,
        **kwargs
    )

    # Run generation in separate thread
    thread = Thread(target=model.generate, kwargs=generation_kwargs)
    thread.start()

    # Create async generator for streaming
    class StreamWrapper:
        def __init__(self, streamer):
            self.streamer = streamer
            self.final_text = ""

        async def __aiter__(self):
            for text in self.streamer:
                self.final_text += text
                yield type('Event', (), {
                    'type': 'content.delta',
                    'delta': text
                })

        async def get_final_completion(self):
            return type('Completion', (), {
                'choices': [
                    type('Choice', (), {
                        'message': type('Message', (), {
                            'content': self.final_text,
                            'tool_calls': [],
                            'function_call': None
                        })
                    })
                ]
            })

    return StreamWrapper(streamer)
