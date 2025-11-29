# Alloy Training Loop

A Python-based training loop for fine-tuning LLMs on the Alloy formal specification language, using a compiler-in-the-loop approach.

## Overview

This module implements a supervised fine-tuning (SFT) pipeline that uses the Alloy analyzer as an oracle to:

1. **Validate generated code** - Parse and type-check Alloy specifications
2. **Classify results** - Determine success/failure and error types
3. **Generate training data** - Create structured examples for fine-tuning
4. **Iteratively improve** - Run multiple generations with feedback

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   LLM Model     │────▶│  Alloy Oracle    │────▶│ Training Data   │
│  (Generator)    │     │  (Validator)     │     │   Generator     │
└─────────────────┘     └──────────────────┘     └─────────────────┘
        ▲                                                │
        │                                                │
        └────────────────────────────────────────────────┘
                        Fine-tuning Loop
```

## Installation

```bash
# Install Python dependencies
pip install -r requirements.txt

# Build Alloy (from repository root)
./gradlew build
```

## Quick Start

### 1. Generate Seed Data

```bash
python training_loop.py --seed-only --output ./my_training_data
```

### 2. Run the Oracle Directly

```python
from alloy_oracle import AlloyOracle

oracle = AlloyOracle()

# Test an Alloy module
result = oracle.run("""
sig Node {
    edges: set Node
}

run {} for 3 Node
""")

print(f"Status: {result.status}")
print(f"Summary: {result.summary}")
```

### 3. Generate Training Examples

```python
from training_data_generator import TrainingDataGenerator
from seed_data import get_builtin_seeds

# Create generator
generator = TrainingDataGenerator()

# Add seed examples
seeds = get_builtin_seeds()
generator.add_seed_examples(seeds)

# Export for fine-tuning
generator.export_openai_format("training_data.jsonl")
```

### 4. Full Training Loop

```python
from training_loop import TrainingLoop, TrainingConfig, OpenAIModelInterface
from seed_data import get_builtin_seeds

# Configure the loop
config = TrainingConfig(
    output_dir="./training_output",
    num_generations=3,
    candidates_per_generation=100
)

# Set up model interface (optional - for generation)
model = OpenAIModelInterface(
    model="gpt-4",
    api_key="your-api-key"
)

# Create and run the loop
loop = TrainingLoop(config, model=model)
seeds = get_builtin_seeds()

# Run Generation 0 with seeds
loop.run_seed_generation(seeds)

# Or run the full loop
def generate_instructions(generation: int) -> list:
    return [
        "Model a binary tree",
        "Create a graph with no cycles",
        # ... more instructions
    ]

loop.run_full_loop(seeds, generate_instructions)
```

## Task Types

The training data supports several task types:

| Task Type | Input | Output |
|-----------|-------|--------|
| `nl-to-alloy` | Natural language description | Alloy code |
| `fix-parse-error` | Broken code + error message | Corrected code |
| `fix-type-error` | Code with type errors + message | Corrected code |
| `generate-assertion` | Alloy module | Assert + check |
| `tool-call` | User goal + context | Tool call JSON |

## Data Format

### Simple Format (input/output)

```json
{
  "input": "<INSTRUCTION>\nModel a directed graph\n</INSTRUCTION>",
  "output": "sig Node { edges: set Node } run {} for 3"
}
```

### OpenAI Chat Format

```json
{
  "messages": [
    {"role": "system", "content": "You are an Alloy expert..."},
    {"role": "user", "content": "Model a directed graph"},
    {"role": "assistant", "content": "sig Node { edges: set Node }..."}
  ]
}
```

## Oracle Status Codes

| Status | Description |
|--------|-------------|
| `parse-error` | Syntax error in the code |
| `type-error` | Type checking failed |
| `instance-found` | Solver found a satisfying instance |
| `no-instance` | No instance found (UNSAT) |
| `timeout` | Solver timed out |
| `error` | Other execution error |

## Training Schedule

A recommended training schedule:

### Generation 0 (Seed)
- ~500-1k hand-crafted examples
- Fine-tune for 1-2 epochs

### Generation 1-3
- Generate 1-2k candidates per generation
- Keep all successful examples
- Manually fix 50-100 interesting failures
- Fine-tune for 1 epoch

After 2-3 generations, expect significant improvement in:
- Valid Alloy syntax generation
- Correct use of idioms (sig, fact, pred, assert)
- Proper command generation (run, check)

## Module Reference

### `alloy_oracle.py`
- `AlloyOracle` - Main oracle class wrapping Alloy CLI
- `OracleStatus` - Execution status enum
- `OracleResponse` - Structured response from oracle

### `data_schema.py`
- `TrainingExample` - Single training example
- `TrainingDataset` - Collection of examples
- `TrainingTaskType` - Task type enum
- `SeedExample` - Seed data structure

### `training_data_generator.py`
- `TrainingDataGenerator` - Generates training examples from oracle results

### `training_loop.py`
- `TrainingLoop` - Main training loop
- `TrainingConfig` - Configuration dataclass
- `ModelInterface` - Abstract model interface
- `OpenAIModelInterface` - OpenAI implementation

### `seed_data.py`
- `load_alloy_file()` - Load model from .als file
- `create_seed_dataset()` - Create seeds from directory
- `get_builtin_seeds()` - Get built-in examples

## Configuration

```python
config = TrainingConfig(
    # Output
    output_dir="./training_output",
    
    # Training loop
    num_generations=3,
    candidates_per_generation=100,
    max_examples_per_generation=500,
    failures_to_fix_per_generation=50,
    
    # Oracle
    oracle_timeout_ms=30000,
    oracle_solver="sat4j",
    
    # Model
    model_name="gpt-4",
    temperature=0.7,
    max_tokens=2048,
    
    # Export
    export_openai_format=True
)
```

## Using with Local Models

For local fine-tuning with Hugging Face Transformers:

```python
from training_loop import ModelInterface

class LocalModelInterface(ModelInterface):
    def __init__(self, model_path):
        from transformers import AutoModelForCausalLM, AutoTokenizer
        self.model = AutoModelForCausalLM.from_pretrained(model_path)
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
    
    def generate(self, instruction, context=None, num_candidates=1):
        # Implementation here
        pass
    
    def fine_tune(self, training_file):
        # Implementation here
        pass
```

## Loading Existing Alloy Models

Load seed data from the included Alloy examples:

```python
from seed_data import create_seed_dataset

# Load from extra models directory
seeds = create_seed_dataset(
    "../org.alloytools.alloy.extra/extra/models/examples",
    output_file="seeds.jsonl"
)
```

## Extending

### Custom Task Types

```python
from data_schema import TrainingTaskType

# Add to the enum in data_schema.py:
EXPLAIN_SPEC = "explain-spec"  # Explain Alloy code in NL
```

### Custom Model Interface

```python
class MyModelInterface(ModelInterface):
    def generate(self, instruction, context=None, num_candidates=1):
        # Your generation logic
        return ["sig Node {} run {}"]
    
    def fine_tune(self, training_file):
        # Your fine-tuning logic
        return "model-v2"
```

## License

Same as the parent Alloy project.
