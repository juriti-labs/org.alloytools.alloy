"""
Alloy Training Loop Package

This package provides a compiler-in-the-loop training loop for fine-tuning
LLMs on the Alloy formal specification language.

Main components:
- alloy_oracle: Wrapper for the Alloy analyzer
- data_schema: Training data structures
- training_data_generator: Generate training examples from oracle results
- training_loop: Main training loop
- seed_data: Utilities for creating seed examples
"""

from .alloy_oracle import (
    AlloyOracle,
    OracleStatus,
    OracleError,
    OracleResponse,
    InstanceStats,
    run_alloy
)

from .data_schema import (
    TrainingExample,
    TrainingDataset,
    TrainingTaskType,
    ToolCall,
    SeedExample
)

from .training_data_generator import TrainingDataGenerator

from .training_loop import (
    TrainingLoop,
    TrainingConfig,
    GenerationStats,
    ModelInterface,
    OpenAIModelInterface,
    create_sample_seed_examples
)

from .seed_data import (
    AlloyModel,
    load_alloy_file,
    load_models_from_directory,
    create_seed_dataset,
    get_builtin_seeds,
    extract_model_components
)

__version__ = "1.0.0"

__all__ = [
    # Oracle
    "AlloyOracle",
    "OracleStatus",
    "OracleError",
    "OracleResponse",
    "InstanceStats",
    "run_alloy",
    
    # Data schema
    "TrainingExample",
    "TrainingDataset",
    "TrainingTaskType",
    "ToolCall",
    "SeedExample",
    
    # Training
    "TrainingDataGenerator",
    "TrainingLoop",
    "TrainingConfig",
    "GenerationStats",
    "ModelInterface",
    "OpenAIModelInterface",
    "create_sample_seed_examples",
    
    # Seed data
    "AlloyModel",
    "load_alloy_file",
    "load_models_from_directory",
    "create_seed_dataset",
    "get_builtin_seeds",
    "extract_model_components",
]
