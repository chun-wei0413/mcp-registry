# AI Agent Guide: Using EmbeddingGemma-300m with Sentence-Transformers

## 1. Overview
`google/embeddinggemma-300m` is a lightweight (300M parameters) state-of-the-art embedding model. It is optimized for on-device usage and supports Matryoshka Representation Learning (MRL).

## 2. Environment Setup
Install the necessary dependencies:
```bash
pip install -U sentence-transformers
3. Model Initialization
Use sentence-transformers to load the model. Note: Use bfloat16 for optimal performance on modern CPUs/GPUs.

Python
from sentence_transformers import SentenceTransformer
import torch

model = SentenceTransformer(
    "google/embeddinggemma-300m", 
    device="cuda" if torch.cuda.is_available() else "cpu", # Use "mps" for Apple Silicon
    model_kwargs={"torch_dtype": torch.bfloat16}
)
4. Required Prompt Templates (Crucial)
Unlike BERT-based models, EmbeddingGemma requires specific prefixes to achieve high accuracy. Use the following formats:

Task	Prefix Format
Asymmetric Query	`task: search result
Document/Context	`title: {optional_title}
QA Task	`task: question answering
Implementation Example:

Python
# For the Search Query (User Input)
query_text = "How to implement Aggregate Root in DDD?"
query_prefixed = f"task: search result | query: {query_text}"

# For the Document (Knowledge Base)
doc_text = "An Aggregate Root is a DDD pattern that..."
doc_prefixed = f"title: Domain-Driven Design Concepts | text: {doc_text}"

embeddings = model.encode([query_prefixed, doc_prefixed])
5. Matryoshka Dimension Truncation (Optional)
This model supports truncation. You can reduce the default 768 dimensions to 256 or 128 to save storage in your Vector DB (like Qdrant or Milvus) with minimal accuracy loss.

Python
import torch.nn.functional as F

# 1. Encode with full dimensions (768)
embeddings = model.encode([query_prefixed])

# 2. Truncate to 256 dimensions
embeddings_256 = embeddings[:, :256]

# 3. Re-normalize for Cosine Similarity
embeddings_256 = F.normalize(torch.tensor(embeddings_256), p=2, dim=1)
6. Integration Notes for RAG
Context Length: 2048 tokens. Ensure your chunking strategy stays within this limit.

Normalization: sentence-transformers usually handles normalization, but if manually calculating similarity, ensure vectors are L2-normalized.

Cross-lingual: The model is highly capable of matching Traditional Chinese queries to English documentation.