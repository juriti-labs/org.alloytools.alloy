"""
Compiler-in-the-Loop Training Loop for Alloy

This module implements the full training loop that:
1. Generates candidate Alloy code using the current model
2. Runs candidates through the Alloy oracle
3. Classifies results and creates training examples
4. Optionally fine-tunes the model on successful examples
"""

import json
import logging
import os
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import List, Optional, Dict, Any, Callable

from alloy_oracle import AlloyOracle, OracleResponse, OracleStatus
from data_schema import (
    TrainingExample,
    TrainingDataset,
    TrainingTaskType,
    SeedExample
)
from training_data_generator import TrainingDataGenerator

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class GenerationStats:
    """Statistics for a single generation."""
    generation: int
    total_candidates: int = 0
    successful: int = 0
    parse_errors: int = 0
    type_errors: int = 0
    timeouts: int = 0
    other_errors: int = 0
    
    @property
    def success_rate(self) -> float:
        if self.total_candidates == 0:
            return 0.0
        return self.successful / self.total_candidates
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            "generation": self.generation,
            "total_candidates": self.total_candidates,
            "successful": self.successful,
            "parse_errors": self.parse_errors,
            "type_errors": self.type_errors,
            "timeouts": self.timeouts,
            "other_errors": self.other_errors,
            "success_rate": self.success_rate
        }


@dataclass
class TrainingConfig:
    """Configuration for the training loop."""
    
    # Output directory for training data and logs
    output_dir: str = "./training_output"
    
    # Number of generations to run
    num_generations: int = 3
    
    # Number of candidates to generate per generation
    candidates_per_generation: int = 100
    
    # Maximum examples to use for fine-tuning per generation
    max_examples_per_generation: int = 500
    
    # Number of failures to manually fix per generation
    failures_to_fix_per_generation: int = 50
    
    # Whether to export in OpenAI format
    export_openai_format: bool = True
    
    # Oracle configuration
    oracle_timeout_ms: int = 30000
    oracle_solver: str = "sat4j"
    
    # Model configuration (for when using model generation)
    model_name: str = "gpt-4"
    temperature: float = 0.7
    max_tokens: int = 2048


class ModelInterface:
    """
    Abstract interface for model generation.
    Implement this to connect to your LLM.
    """
    
    def generate(
        self,
        instruction: str,
        context: Optional[str] = None,
        num_candidates: int = 1
    ) -> List[str]:
        """
        Generate Alloy code candidates.
        
        Args:
            instruction: The natural language instruction.
            context: Optional context (e.g., partial code, examples).
            num_candidates: Number of candidates to generate.
            
        Returns:
            List of generated Alloy code strings.
        """
        raise NotImplementedError("Subclass must implement generate()")
    
    def fine_tune(self, training_file: str) -> str:
        """
        Fine-tune the model on new training data.
        
        Args:
            training_file: Path to the training file (JSONL).
            
        Returns:
            The ID or name of the fine-tuned model.
        """
        raise NotImplementedError("Subclass must implement fine_tune()")


class OpenAIModelInterface(ModelInterface):
    """
    Model interface for OpenAI models.
    Requires openai>=1.0.0
    """
    
    def __init__(
        self,
        model: str = "gpt-4",
        api_key: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 2048
    ):
        try:
            from openai import OpenAI
        except ImportError:
            raise ImportError("Please install openai: pip install openai>=1.0.0")
        
        self.model = model
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.client = OpenAI(api_key=api_key)
    
    def generate(
        self,
        instruction: str,
        context: Optional[str] = None,
        num_candidates: int = 1
    ) -> List[str]:
        """Generate Alloy code using OpenAI."""
        system_prompt = """You are an expert in the Alloy formal specification language.
Generate syntactically correct Alloy code that satisfies the given requirements.
Output only the Alloy code, no explanations."""
        
        user_message = instruction
        if context:
            user_message = f"{instruction}\n\nContext:\n{context}"
        
        candidates = []
        for _ in range(num_candidates):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_message}
                    ],
                    temperature=self.temperature,
                    max_tokens=self.max_tokens
                )
                candidates.append(response.choices[0].message.content)
            except Exception as e:
                logger.warning(f"Generation failed: {e}")
        
        return candidates
    
    def fine_tune(self, training_file: str) -> str:
        """Start fine-tuning job on OpenAI."""
        # Upload training file
        with open(training_file, "rb") as f:
            file_response = self.client.files.create(
                file=f,
                purpose="fine-tune"
            )
        
        # Create fine-tuning job
        job = self.client.fine_tuning.jobs.create(
            training_file=file_response.id,
            model=self.model
        )
        
        logger.info(f"Fine-tuning job created: {job.id}")
        return job.id


class TrainingLoop:
    """
    The main training loop that orchestrates compiler-in-the-loop training.
    
    This implements the full pipeline:
    1. Generate candidates with current model
    2. Run through oracle to classify
    3. Create training examples from successful/failed runs
    4. Optionally include human corrections for failures
    5. Fine-tune model on new data
    """
    
    def __init__(
        self,
        config: TrainingConfig,
        model: Optional[ModelInterface] = None,
        oracle: Optional[AlloyOracle] = None
    ):
        """
        Initialize the training loop.
        
        Args:
            config: Training configuration.
            model: Model interface for generation (optional for seed-only mode).
            oracle: Alloy oracle instance (creates default if None).
        """
        self.config = config
        self.model = model
        self.oracle = oracle or AlloyOracle(
            timeout_ms=config.oracle_timeout_ms,
            solver=config.oracle_solver
        )
        
        self.generator = TrainingDataGenerator(oracle=self.oracle)
        self.all_datasets: List[TrainingDataset] = []
        self.stats: List[GenerationStats] = []
        
        # Create output directory
        self.output_dir = Path(config.output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def run_seed_generation(self, seed_examples: List[SeedExample]) -> GenerationStats:
        """
        Run Generation 0 with seed examples.
        
        Args:
            seed_examples: The seed examples to use.
            
        Returns:
            Statistics for this generation.
        """
        logger.info("Starting Generation 0 (Seed)")
        
        self.generator.set_generation(0)
        stats = GenerationStats(generation=0)
        
        # Add seed examples
        count = self.generator.add_seed_examples(seed_examples)
        stats.total_candidates = count
        stats.successful = count  # Seeds are assumed correct
        
        # Save the dataset
        self._save_generation(0)
        self.stats.append(stats)
        
        logger.info(f"Generation 0 complete: {count} seed examples")
        return stats
    
    def run_generation(
        self,
        generation: int,
        instructions: List[str],
        human_corrections: Optional[Dict[str, str]] = None
    ) -> GenerationStats:
        """
        Run a single training generation.
        
        Args:
            generation: The generation number.
            instructions: NL instructions to generate code for.
            human_corrections: Optional dict mapping broken code to corrected code.
            
        Returns:
            Statistics for this generation.
        """
        logger.info(f"Starting Generation {generation}")
        
        if self.model is None:
            raise ValueError("Model interface required for generation > 0")
        
        self.generator.set_generation(generation)
        stats = GenerationStats(generation=generation)
        
        # Generate candidates
        for instruction in instructions:
            try:
                # Generate code with model
                candidates = self.model.generate(
                    instruction,
                    num_candidates=1
                )
                
                if not candidates:
                    continue
                
                candidate_code = candidates[0]
                
                # Process through oracle
                response, example = self.generator.process_candidate(
                    instruction,
                    candidate_code
                )
                
                stats.total_candidates += 1
                
                # Track stats
                if response.status == OracleStatus.PARSE_ERROR:
                    stats.parse_errors += 1
                elif response.status == OracleStatus.TYPE_ERROR:
                    stats.type_errors += 1
                elif response.status == OracleStatus.TIMEOUT:
                    stats.timeouts += 1
                elif response.status in (OracleStatus.INSTANCE_FOUND, OracleStatus.NO_INSTANCE):
                    stats.successful += 1
                else:
                    stats.other_errors += 1
                    
            except Exception as e:
                logger.warning(f"Failed to process instruction: {e}")
                stats.other_errors += 1
        
        # Apply human corrections
        if human_corrections:
            for original, corrected in human_corrections.items():
                self.generator.add_fix_example(original, corrected)
        
        # Save the dataset
        self._save_generation(generation)
        self.stats.append(stats)
        
        logger.info(
            f"Generation {generation} complete: "
            f"{stats.successful}/{stats.total_candidates} successful "
            f"({stats.success_rate:.1%})"
        )
        
        return stats
    
    def run_full_loop(
        self,
        seed_examples: List[SeedExample],
        instruction_generator: Callable[[int], List[str]],
        correction_callback: Optional[Callable[[List[TrainingExample]], Dict[str, str]]] = None
    ) -> List[GenerationStats]:
        """
        Run the full training loop for all generations.
        
        Args:
            seed_examples: Initial seed examples for generation 0.
            instruction_generator: Function that generates instructions for each generation.
            correction_callback: Optional callback to get human corrections for failures.
            
        Returns:
            List of statistics for each generation.
        """
        # Generation 0: Seed data
        self.run_seed_generation(seed_examples)
        
        # Export and potentially fine-tune after seed
        if self.model is not None:
            seed_path = self.output_dir / "gen_0_training.jsonl"
            if self.config.export_openai_format:
                self.generator.export_openai_format(str(seed_path))
                # Optionally fine-tune here
        
        # Subsequent generations
        for gen in range(1, self.config.num_generations + 1):
            # Generate instructions for this generation
            instructions = instruction_generator(gen)[:self.config.candidates_per_generation]
            
            # Get human corrections if callback provided
            corrections = None
            if correction_callback and gen > 1:
                # Get failures from previous generation
                failures = self.generator.get_examples_needing_correction()[:self.config.failures_to_fix_per_generation]
                if failures:
                    corrections = correction_callback(failures)
            
            # Run generation
            self.run_generation(gen, instructions, corrections)
            
            # Export training data
            if self.model is not None:
                gen_path = self.output_dir / f"gen_{gen}_training.jsonl"
                if self.config.export_openai_format:
                    self.generator.export_openai_format(str(gen_path))
        
        # Final export
        self._save_combined_dataset()
        self._save_stats()
        
        return self.stats
    
    def get_combined_dataset(self) -> TrainingDataset:
        """Get all examples from all generations."""
        combined = TrainingDataset()
        for example in self.generator.get_dataset():
            combined.examples.append(example)
        return combined
    
    def _save_generation(self, generation: int) -> None:
        """Save the current generation's dataset."""
        gen_dir = self.output_dir / f"generation_{generation}"
        gen_dir.mkdir(exist_ok=True)
        
        # Save positive examples
        positive_path = gen_dir / "positive_examples.jsonl"
        self.generator.export_jsonl(str(positive_path), include_negative=False)
        
        # Save all examples including failures
        all_path = gen_dir / "all_examples.jsonl"
        self.generator.export_jsonl(str(all_path), include_negative=True)
        
        # Save failures needing correction
        failures = self.generator.get_examples_needing_correction()
        if failures:
            failures_path = gen_dir / "needs_correction.jsonl"
            failures_dataset = TrainingDataset(generation=generation)
            failures_dataset.examples = failures
            failures_dataset.save_jsonl(str(failures_path), include_negative=True)
    
    def _save_combined_dataset(self) -> None:
        """Save the combined dataset from all generations."""
        combined_path = self.output_dir / "combined_training.jsonl"
        combined = self.get_combined_dataset()
        combined.save_jsonl(str(combined_path))
        
        if self.config.export_openai_format:
            openai_path = self.output_dir / "combined_training_openai.jsonl"
            combined.save_openai_format(str(openai_path))
    
    def _save_stats(self) -> None:
        """Save training statistics."""
        stats_path = self.output_dir / "training_stats.json"
        stats_data = {
            "timestamp": datetime.now().isoformat(),
            "config": {
                "num_generations": self.config.num_generations,
                "candidates_per_generation": self.config.candidates_per_generation,
                "oracle_timeout_ms": self.config.oracle_timeout_ms
            },
            "generations": [s.to_dict() for s in self.stats]
        }
        
        with open(stats_path, 'w') as f:
            json.dump(stats_data, f, indent=2)


def create_sample_seed_examples() -> List[SeedExample]:
    """Create a small set of sample seed examples for testing."""
    return [
        SeedExample(
            description="Model a simple directed graph with nodes connected by edges",
            alloy_code="""
sig Node {
    edges: set Node
}

pred someEdges {
    some edges
}

run someEdges for 4 Node
""",
            category="graph",
            difficulty=1,
            description_variants=[
                "Create an Alloy model for a directed graph",
                "Define a graph where nodes can point to other nodes"
            ]
        ),
        
        SeedExample(
            description="Model a binary tree with parent-child relationships",
            alloy_code="""
sig Node {
    left: lone Node,
    right: lone Node
}

fact TreeStructure {
    // No node is its own ancestor
    no n: Node | n in n.^(left + right)
    
    // Each node has at most one parent
    all n: Node | lone p: Node | n in p.left + p.right
}

run {} for 5 Node
""",
            category="tree",
            difficulty=2
        ),
        
        SeedExample(
            description="Model a file system with directories and files",
            alloy_code="""
abstract sig FSObject {
    parent: lone Dir
}

sig File extends FSObject {}

sig Dir extends FSObject {
    contents: set FSObject
}

fact FileSystemStructure {
    // Root has no parent
    one root: Dir | no root.parent
    
    // Parent-contents relationship is consistent
    all x: FSObject, d: Dir | x in d.contents iff x.parent = d
    
    // No cycles
    no d: Dir | d in d.^parent
}

run {} for 5 FSObject
""",
            category="filesystem",
            difficulty=3
        ),
        
        SeedExample(
            description="Model a simple state machine with states and transitions",
            alloy_code="""
sig State {
    transitions: set State
}

one sig InitialState extends State {}
one sig FinalState extends State {}

fact StateMachineRules {
    // Initial state is reachable from itself
    InitialState in InitialState.*transitions
    
    // All states are reachable from initial
    all s: State | s in InitialState.*transitions
}

run {} for 4 State
""",
            category="state-machine",
            difficulty=2
        ),
        
        SeedExample(
            description="Model a linked list with head and tail",
            alloy_code="""
sig Node {
    next: lone Node
}

one sig Head extends Node {}

fact LinkedListRules {
    // No cycles
    no n: Node | n in n.^next
    
    // All nodes reachable from head
    all n: Node | n in Head.*next
}

pred hasTail {
    one n: Node | no n.next
}

run hasTail for 5 Node
""",
            category="list",
            difficulty=2
        )
    ]


def main():
    """Example usage of the training loop."""
    import argparse
    
    parser = argparse.ArgumentParser(description="Alloy Training Loop")
    parser.add_argument("--output", default="./training_output", help="Output directory")
    parser.add_argument("--generations", type=int, default=1, help="Number of generations")
    parser.add_argument("--seed-only", action="store_true", help="Only generate seed data")
    args = parser.parse_args()
    
    config = TrainingConfig(
        output_dir=args.output,
        num_generations=args.generations
    )
    
    # Create seed examples
    seeds = create_sample_seed_examples()
    
    if args.seed_only:
        # Just generate seed data
        loop = TrainingLoop(config)
        stats = loop.run_seed_generation(seeds)
        logger.info(f"Seed generation complete: {stats.successful} examples")
    else:
        # For full loop, would need a model interface
        logger.info("Full training loop requires model interface (--seed-only for testing)")
        loop = TrainingLoop(config)
        stats = loop.run_seed_generation(seeds)
        logger.info(f"Seed generation complete: {stats.successful} examples")
    
    logger.info(f"Output saved to: {config.output_dir}")


if __name__ == "__main__":
    main()
