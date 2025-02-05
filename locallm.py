# mistralai.py
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
from app.config import settings

# Load Mistral model and tokenizer
tokenizer = AutoTokenizer.from_pretrained(settings.MISTRAL_MODEL)
model = AutoModelForCausalLM.from_pretrained(settings.MISTRAL_MODEL, torch_dtype=torch.float16).to("cuda" if torch.cuda.is_available() else "cpu")

def token_size(text):
    return len(tokenizer.encode(text))

async def get_embedding(input_text, model=settings.EMBEDDING_MODEL, dimensions=settings.EMBEDDING_DIMENSIONS):
    inputs = tokenizer(input_text, return_tensors="pt", padding=True, truncation=True)
    with torch.no_grad():
        outputs = model(**inputs)
    return outputs.last_hidden_state.mean(dim=1).tolist()[0]  # Simulated embedding

async def get_embeddings(input_list, model=settings.EMBEDDING_MODEL, dimensions=settings.EMBEDDING_DIMENSIONS):
    return [await get_embedding(text, model, dimensions) for text in input_list]

async def chat_stream(messages, model=settings.MISTRAL_MODEL, temperature=0.1, **kwargs):
    input_text = "\n".join([f"{msg['role'].upper()}: {msg['content']}" for msg in messages])
    inputs = tokenizer(input_text, return_tensors="pt").to(model.device)
    with torch.no_grad():
        outputs = model.generate(**inputs, max_new_tokens=256, temperature=temperature, **kwargs)
    response = tokenizer.decode(outputs[0], skip_special_tokens=True)
    return response
