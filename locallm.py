from sentence_transformers import SentenceTransformer
from transformers import AutoTokenizer, AutoModelForCausalLM
import torch
from typing import List, AsyncIterator, Dict
import asyncio
from dataclasses import dataclass

class LocalLLM:
    def __init__(self):
        # Initialize embedding model
        self.embedding_model = SentenceTransformer('all-MiniLM-L6-v2')
        
        # Initialize tokenizer for token counting
        self.tokenizer = AutoTokenizer.from_pretrained("mistralai/Mistral-7B-v0.1")
        
        # Load text generation model (lazy loading on first use)
        self._gen_model = None
        self._gen_tokenizer = None

    def token_size(self, text: str) -> int:
        """Count tokens in text using the loaded tokenizer"""
        return len(self.tokenizer.encode(text))

    async def get_embedding(self, input: str, dimensions: int = 384) -> List[float]:
        """Generate embeddings for a single input text"""
        # Run in executor to avoid blocking
        embedding = await asyncio.get_event_loop().run_in_executor(
            None, self.embedding_model.encode, input
        )
        return embedding.tolist()

    async def get_embeddings(self, inputs: List[str], dimensions: int = 384) -> List[List[float]]:
        """Generate embeddings for multiple input texts"""
        # Run in executor to avoid blocking
        embeddings = await asyncio.get_event_loop().run_in_executor(
            None, self.embedding_model.encode, inputs
        )
        return embeddings.tolist()

    def _ensure_gen_model_loaded(self):
        """Lazy load the generation model when needed"""
        if self._gen_model is None:
            # Load model with 4-bit quantization to reduce memory usage
            self._gen_tokenizer = AutoTokenizer.from_pretrained("mistralai/Mistral-7B-v0.1")
            self._gen_model = AutoModelForCausalLM.from_pretrained(
                "mistralai/Mistral-7B-v0.1",
                device_map="auto",
                load_in_4bit=True
            )

    async def chat_stream(
        self, 
        messages: List[Dict[str, str]], 
        temperature: float = 0.1,
        **kwargs
    ) -> AsyncIterator[str]:
        """Stream chat completions"""
        self._ensure_gen_model_loaded()
        
        # Convert messages to prompt format
        prompt = ""
        for msg in messages:
            role = msg["role"]
            content = msg["content"]
            prompt += f"{role}: {content}\nassistant: "

        # Tokenize input
        inputs = self._gen_tokenizer(prompt, return_tensors="pt").to(self._gen_model.device)
        
        # Generate with streaming
        streamer = TextIteratorStreamer(self._gen_tokenizer, timeout=10.0, skip_prompt=True, skip_special_tokens=True)
        generation_kwargs = dict(
            inputs=inputs,
            streamer=streamer,
            max_new_tokens=1024,
            temperature=temperature,
            **kwargs
        )

        # Run generation in a separate thread
        thread = Thread(target=self._gen_model.generate, kwargs=generation_kwargs)
        thread.start()

        # Stream the output
        for text in streamer:
            yield text

# Create singleton instance
llm = LocalLLM()

# Export the same interface as the original openai.py
token_size = llm.token_size
get_embedding = llm.get_embedding
get_embeddings = llm.get_embeddings
chat_stream = llm.chat_stream
