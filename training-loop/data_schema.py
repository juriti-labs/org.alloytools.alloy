"""
Data Schema for Alloy SFT Training

This module defines the data structures and schemas used for
Supervised Fine-Tuning (SFT) training data generation.
"""

from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional, Dict, Any
import json
import uuid


class TrainingTaskType(Enum):
    """Types of tasks for LLM training on Alloy."""
    
    # Natural language to Alloy specification
    # Input: NL description/constraints
    # Output: Alloy module with sigs, facts, preds, check/run
    NL_TO_ALLOY = "nl-to-alloy"
    
    # Fix compiler/parse errors
    # Input: Original Alloy code + compiler error message
    # Output: Corrected Alloy code that compiles
    FIX_PARSE_ERROR = "fix-parse-error"
    
    # Fix type errors
    # Input: Original Alloy code + type error message
    # Output: Corrected Alloy code that type-checks
    FIX_TYPE_ERROR = "fix-type-error"
    
    # Generate property/assertion
    # Input: Alloy module
    # Output: Meaningful assert/check pairs
    GENERATE_ASSERTION = "generate-assertion"
    
    # Tool-call control for function calling
    # Input: User goal + context
    # Output: Structured tool call JSON
    TOOL_CALL = "tool-call"
    
    # Explain counterexample
    # Input: Alloy module + counterexample
    # Output: Natural language explanation
    EXPLAIN_COUNTEREXAMPLE = "explain-counterexample"
    
    # Refine specification based on counterexample
    # Input: Alloy module + counterexample + explanation
    # Output: Refined Alloy module
    REFINE_SPEC = "refine-spec"


@dataclass
class ToolCall:
    """Represents a tool call for function-calling style training."""
    tool: str
    args: Dict[str, Any] = field(default_factory=dict)
    
    def to_dict(self) -> Dict[str, Any]:
        return {"tool": self.tool, "args": self.args}
    
    def to_json(self) -> str:
        return json.dumps(self.to_dict())


@dataclass
class TrainingExample:
    """
    Represents a single training example for SFT (Supervised Fine-Tuning).
    Designed to be serializable to JSONL format for vendor fine-tuning APIs.
    """
    
    # The type of task this example represents
    task_type: TrainingTaskType
    
    # Unique identifier for this example
    id: str = field(default_factory=lambda: f"train-{str(uuid.uuid4())[:8]}")
    
    # The natural language instruction or description
    instruction: str = ""
    
    # Optional Alloy context (existing code, module, etc.)
    alloy_context: Optional[str] = None
    
    # Optional compiler feedback (for fix-error tasks)
    compiler_feedback: Optional[str] = None
    
    # The target output (Alloy code, tool call, etc.)
    target_output: str = ""
    
    # Optional tool call in structured format
    tool_call: Optional[ToolCall] = None
    
    # Metadata about this example
    metadata: Dict[str, str] = field(default_factory=dict)
    
    # Whether this is a positive example (model output was correct)
    is_positive: bool = True
    
    # Generation number (for iterative training)
    generation: int = 0
    
    def format_input(self) -> str:
        """
        Format the input section for SFT training.
        Uses a structured format with XML-like tags.
        """
        parts = []
        
        parts.append("<INSTRUCTION>")
        parts.append(self.instruction)
        parts.append("</INSTRUCTION>")
        
        if self.alloy_context:
            parts.append("<ALLOY_CONTEXT>")
            parts.append(self.alloy_context)
            parts.append("</ALLOY_CONTEXT>")
        
        if self.compiler_feedback:
            parts.append("<COMPILER_FEEDBACK>")
            parts.append(self.compiler_feedback)
            parts.append("</COMPILER_FEEDBACK>")
        
        return "\n".join(parts)
    
    def to_simple_format(self) -> Dict[str, str]:
        """
        Format as a simple input/output pair.
        Suitable for OpenAI-style SFT format.
        """
        return {
            "input": self.format_input(),
            "output": self.target_output
        }
    
    def to_messages_format(self) -> List[Dict[str, str]]:
        """
        Format as chat messages for chat-based fine-tuning.
        """
        messages = [
            {"role": "system", "content": "You are an expert in the Alloy formal specification language. You help users write correct Alloy specifications."},
            {"role": "user", "content": self.format_input()}
        ]
        
        if self.tool_call:
            messages.append({
                "role": "assistant",
                "content": self.target_output,
                "tool_calls": [self.tool_call.to_dict()]
            })
        else:
            messages.append({
                "role": "assistant",
                "content": self.target_output
            })
        
        return messages
    
    def to_jsonl_dict(self) -> Dict[str, Any]:
        """Convert to a dictionary for JSONL serialization."""
        result = {
            "id": self.id,
            "task_type": self.task_type.value,
            "generation": self.generation,
            "input": self.format_input(),
            "output": self.target_output
        }
        
        if self.tool_call:
            result["tool_call"] = self.tool_call.to_dict()
        
        if self.metadata:
            result["metadata"] = self.metadata
        
        return result
    
    def to_jsonl_line(self) -> str:
        """Serialize to a single JSONL line."""
        return json.dumps(self.to_jsonl_dict(), ensure_ascii=False)


@dataclass
class TrainingDataset:
    """A collection of training examples."""
    
    examples: List[TrainingExample] = field(default_factory=list)
    generation: int = 0
    
    def add(self, example: TrainingExample) -> None:
        """Add an example to the dataset."""
        example.generation = self.generation
        self.examples.append(example)
    
    def get_positive_examples(self) -> List[TrainingExample]:
        """Get only positive examples suitable for SFT."""
        return [ex for ex in self.examples if ex.is_positive]
    
    def get_examples_needing_correction(self) -> List[TrainingExample]:
        """Get examples that need human correction."""
        return [ex for ex in self.examples if not ex.is_positive]
    
    def get_by_task_type(self, task_type: TrainingTaskType) -> List[TrainingExample]:
        """Get examples of a specific task type."""
        return [ex for ex in self.examples if ex.task_type == task_type]
    
    def to_jsonl(self, include_negative: bool = False) -> str:
        """
        Serialize to JSONL format.
        
        Args:
            include_negative: Whether to include negative examples.
        """
        if include_negative:
            examples = self.examples
        else:
            examples = self.get_positive_examples()
        
        lines = [ex.to_jsonl_line() for ex in examples]
        return "\n".join(lines)
    
    def save_jsonl(self, path: str, include_negative: bool = False) -> None:
        """Save to a JSONL file."""
        with open(path, 'w', encoding='utf-8') as f:
            f.write(self.to_jsonl(include_negative))
    
    def save_openai_format(self, path: str) -> None:
        """
        Save in OpenAI fine-tuning format (messages array).
        """
        with open(path, 'w', encoding='utf-8') as f:
            for ex in self.get_positive_examples():
                line = json.dumps({"messages": ex.to_messages_format()}, ensure_ascii=False)
                f.write(line + "\n")
    
    def clear(self) -> None:
        """Clear all examples."""
        self.examples.clear()
    
    def __len__(self) -> int:
        return len(self.examples)
    
    def __iter__(self):
        return iter(self.examples)
    
    @classmethod
    def load_jsonl(cls, path: str) -> "TrainingDataset":
        """Load from a JSONL file."""
        dataset = cls()
        with open(path, 'r', encoding='utf-8') as f:
            for line in f:
                if line.strip():
                    data = json.loads(line)
                    example = TrainingExample(
                        task_type=TrainingTaskType(data["task_type"]),
                        id=data.get("id", ""),
                        instruction=data.get("instruction", ""),
                        target_output=data.get("output", ""),
                        generation=data.get("generation", 0),
                        metadata=data.get("metadata", {})
                    )
                    dataset.examples.append(example)
        return dataset


@dataclass
class SeedExample:
    """A seed example for bootstrapping training data."""
    
    # Natural language description
    description: str
    
    # The Alloy code that implements the description
    alloy_code: str
    
    # Category/domain of the example
    category: str = "general"
    
    # Difficulty level (1-5)
    difficulty: int = 1
    
    # Optional variations of the description (for data augmentation)
    description_variants: List[str] = field(default_factory=list)
    
    def to_training_examples(self) -> List[TrainingExample]:
        """Convert to training examples including variants."""
        examples = []
        
        # Main example
        examples.append(TrainingExample(
            task_type=TrainingTaskType.NL_TO_ALLOY,
            instruction=self.description,
            target_output=self.alloy_code,
            metadata={"category": self.category, "difficulty": str(self.difficulty)}
        ))
        
        # Variants
        for variant in self.description_variants:
            examples.append(TrainingExample(
                task_type=TrainingTaskType.NL_TO_ALLOY,
                instruction=variant,
                target_output=self.alloy_code,
                metadata={"category": self.category, "difficulty": str(self.difficulty), "variant": "true"}
            ))
        
        return examples
