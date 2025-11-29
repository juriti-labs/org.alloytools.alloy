"""
Seed Data Utilities for Alloy Training

This module provides utilities for:
- Loading seed examples from Alloy model files
- Generating synthetic NL descriptions
- Creating augmented training data from existing models
"""

import argparse
import json
import os
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Dict, Optional, Tuple

from data_schema import SeedExample, TrainingExample, TrainingTaskType


@dataclass
class AlloyModel:
    """Represents an Alloy model with metadata."""
    path: str
    code: str
    name: str
    category: str = "general"
    description: Optional[str] = None
    commands: List[str] = field(default_factory=list)
    signatures: List[str] = field(default_factory=list)
    predicates: List[str] = field(default_factory=list)
    assertions: List[str] = field(default_factory=list)


def extract_model_components(code: str) -> Dict[str, List[str]]:
    """
    Extract structural components from Alloy code.
    
    Returns:
        Dict with 'signatures', 'predicates', 'facts', 'assertions', 'commands'.
    """
    components = {
        'signatures': [],
        'predicates': [],
        'facts': [],
        'assertions': [],
        'commands': []
    }
    
    # Extract signatures
    sig_pattern = r'(abstract\s+)?sig\s+(\w+)'
    for match in re.finditer(sig_pattern, code):
        components['signatures'].append(match.group(2))
    
    # Extract predicates
    pred_pattern = r'pred\s+(\w+)'
    for match in re.finditer(pred_pattern, code):
        components['predicates'].append(match.group(1))
    
    # Extract facts
    fact_pattern = r'fact\s+(\w+)?'
    for match in re.finditer(fact_pattern, code):
        name = match.group(1) or "anonymous"
        components['facts'].append(name)
    
    # Extract assertions
    assert_pattern = r'assert\s+(\w+)'
    for match in re.finditer(assert_pattern, code):
        components['assertions'].append(match.group(1))
    
    # Extract run/check commands
    cmd_pattern = r'(run|check)\s+(\w+)?'
    for match in re.finditer(cmd_pattern, code):
        cmd_type = match.group(1)
        cmd_name = match.group(2) or "default"
        components['commands'].append(f"{cmd_type} {cmd_name}")
    
    return components


def load_alloy_file(path: str, category: str = "general") -> Optional[AlloyModel]:
    """
    Load an Alloy model from a file.
    
    Args:
        path: Path to the .als file.
        category: Category to assign to this model.
        
    Returns:
        AlloyModel or None if loading fails.
    """
    try:
        with open(path, 'r', encoding='utf-8') as f:
            code = f.read()
        
        name = Path(path).stem
        components = extract_model_components(code)
        
        # Try to extract description from module comment
        description = None
        doc_match = re.search(r'^/\*\*(.*?)\*/', code, re.DOTALL)
        if doc_match:
            description = doc_match.group(1).strip()
            # Clean up comment formatting
            description = re.sub(r'^\s*\*\s*', '', description, flags=re.MULTILINE)
            description = description.strip()
        
        return AlloyModel(
            path=path,
            code=code,
            name=name,
            category=category,
            description=description,
            commands=components['commands'],
            signatures=components['signatures'],
            predicates=components['predicates'],
            assertions=components['assertions']
        )
    except Exception as e:
        print(f"Failed to load {path}: {e}")
        return None


def load_models_from_directory(
    directory: str,
    category: Optional[str] = None,
    recursive: bool = True
) -> List[AlloyModel]:
    """
    Load all Alloy models from a directory.
    
    Args:
        directory: Path to directory containing .als files.
        category: Category to assign (if None, uses subdirectory names).
        recursive: Whether to search subdirectories.
        
    Returns:
        List of loaded AlloyModel instances.
    """
    models = []
    dir_path = Path(directory)
    
    if recursive:
        files = dir_path.rglob("*.als")
    else:
        files = dir_path.glob("*.als")
    
    for file_path in files:
        # Determine category from path
        if category:
            cat = category
        else:
            relative = file_path.relative_to(dir_path)
            if len(relative.parts) > 1:
                cat = relative.parts[0]
            else:
                cat = "general"
        
        model = load_alloy_file(str(file_path), category=cat)
        if model:
            models.append(model)
    
    return models


def generate_description_from_model(model: AlloyModel) -> str:
    """
    Generate a natural language description from model structure.
    
    This is a simple heuristic-based approach. For better results,
    use a large language model.
    """
    parts = []
    
    if model.description:
        return model.description
    
    # Describe based on signatures
    if model.signatures:
        sigs = ", ".join(model.signatures[:3])
        if len(model.signatures) > 3:
            sigs += f" and {len(model.signatures) - 3} more"
        parts.append(f"Model containing {sigs}")
    
    # Describe predicates/facts
    if model.predicates:
        parts.append(f"with predicates: {', '.join(model.predicates[:3])}")
    
    # Describe assertions
    if model.assertions:
        parts.append(f"and assertions: {', '.join(model.assertions[:3])}")
    
    # Describe commands
    if model.commands:
        parts.append(f"Commands: {', '.join(model.commands[:2])}")
    
    return ". ".join(parts) if parts else f"Alloy model: {model.name}"


def model_to_seed_example(
    model: AlloyModel,
    description: Optional[str] = None,
    difficulty: int = 2
) -> SeedExample:
    """
    Convert an AlloyModel to a SeedExample.
    
    Args:
        model: The AlloyModel to convert.
        description: Optional override for the description.
        difficulty: Difficulty rating (1-5).
        
    Returns:
        SeedExample instance.
    """
    desc = description or generate_description_from_model(model)
    
    return SeedExample(
        description=desc,
        alloy_code=model.code,
        category=model.category,
        difficulty=difficulty
    )


def create_seed_dataset(
    models_directory: str,
    output_file: Optional[str] = None
) -> List[SeedExample]:
    """
    Create a seed dataset from a directory of Alloy models.
    
    Args:
        models_directory: Path to directory with .als files.
        output_file: Optional path to save the dataset.
        
    Returns:
        List of SeedExample instances.
    """
    models = load_models_from_directory(models_directory)
    seeds = []
    
    for model in models:
        # Estimate difficulty based on complexity
        complexity = (
            len(model.signatures) +
            len(model.predicates) +
            len(model.assertions)
        )
        
        if complexity <= 3:
            difficulty = 1
        elif complexity <= 6:
            difficulty = 2
        elif complexity <= 10:
            difficulty = 3
        elif complexity <= 15:
            difficulty = 4
        else:
            difficulty = 5
        
        seed = model_to_seed_example(model, difficulty=difficulty)
        seeds.append(seed)
    
    if output_file:
        # Save as JSONL
        with open(output_file, 'w', encoding='utf-8') as f:
            for seed in seeds:
                line = json.dumps({
                    "description": seed.description,
                    "alloy_code": seed.alloy_code,
                    "category": seed.category,
                    "difficulty": seed.difficulty,
                    "variants": seed.description_variants
                }, ensure_ascii=False)
                f.write(line + "\n")
    
    return seeds


def create_assertion_examples_from_model(
    model: AlloyModel
) -> List[Tuple[str, str, str]]:
    """
    Extract assertion examples from a model.
    
    Returns:
        List of (module_code, assertion_code, description) tuples.
    """
    examples = []
    
    # Find assertion blocks
    assert_pattern = r'(assert\s+\w+\s*\{[^}]+\})'
    check_pattern = r'(check\s+\w+[^\n]*)'
    
    assertions = re.findall(assert_pattern, model.code)
    checks = re.findall(check_pattern, model.code)
    
    # Create examples pairing assertions with checks
    for assertion in assertions:
        # Find the assertion name
        name_match = re.search(r'assert\s+(\w+)', assertion)
        if name_match:
            assert_name = name_match.group(1)
            
            # Find corresponding check
            for check in checks:
                if assert_name in check:
                    # Module without the assertion
                    module_without = re.sub(
                        r'assert\s+' + assert_name + r'\s*\{[^}]+\}\s*check\s+' + assert_name + r'[^\n]*\n?',
                        '',
                        model.code
                    )
                    
                    # The assertion and check to generate
                    assertion_code = f"{assertion}\n\n{check}"
                    
                    # Description
                    desc = f"Generate an assertion for {model.name} that checks {assert_name}"
                    
                    examples.append((module_without.strip(), assertion_code, desc))
    
    return examples


# Built-in seed examples for getting started
BUILTIN_SEEDS = [
    SeedExample(
        description="Model a simple graph with nodes and directed edges. Find an instance with at least one edge.",
        alloy_code="""sig Node {
    edges: set Node
}

pred hasEdges {
    some edges
}

run hasEdges for 4 Node
""",
        category="graph",
        difficulty=1,
        description_variants=[
            "Create an Alloy specification for a directed graph",
            "Define a graph structure where nodes can connect to other nodes",
            "Model a network of nodes with connections between them"
        ]
    ),
    
    SeedExample(
        description="Model an acyclic directed graph and verify there are no cycles.",
        alloy_code="""sig Node {
    edges: set Node
}

pred acyclic {
    no n: Node | n in n.^edges
}

fact NoSelfLoops {
    no n: Node | n in n.edges
}

assert AcyclicGraph {
    acyclic
}

check AcyclicGraph for 5 Node
""",
        category="graph",
        difficulty=2,
        description_variants=[
            "Create a directed acyclic graph (DAG) in Alloy",
            "Model a graph that has no cycles and verify this property"
        ]
    ),
    
    SeedExample(
        description="Model a binary tree where each node has at most two children.",
        alloy_code="""sig Node {
    left: lone Node,
    right: lone Node
}

fact BinaryTreeStructure {
    // No node is its own descendant
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
        description="Model a file system with directories containing files and subdirectories.",
        alloy_code="""abstract sig Entry {
    parent: lone Dir
}

sig File extends Entry {}

sig Dir extends Entry {
    contents: set Entry
}

fact FileSystemRules {
    // One root directory with no parent
    one d: Dir | no d.parent
    
    // Contents and parent are inverses
    all e: Entry, d: Dir | e in d.contents iff e.parent = d
    
    // No directory cycles
    no d: Dir | d in d.^parent
}

run {} for 6 Entry
""",
        category="filesystem",
        difficulty=3
    ),
    
    SeedExample(
        description="Model a linked list with a head pointer.",
        alloy_code="""sig Node {
    next: lone Node,
    data: one Int
}

one sig Head in Node {}

fact LinkedListRules {
    // No cycles
    no n: Node | n in n.^next
    
    // All nodes reachable from head
    all n: Node | n in Head.*next
}

pred hasMultipleNodes {
    #Node > 1
}

run hasMultipleNodes for 5 Node, 5 Int
""",
        category="list",
        difficulty=2
    ),
    
    SeedExample(
        description="Model an address book with people who can have friends and family relationships.",
        alloy_code="""sig Person {
    friends: set Person,
    family: set Person
}

fact SocialRules {
    // Friendship is symmetric
    friends = ~friends
    
    // No one is their own friend
    no p: Person | p in p.friends
    
    // Family is transitive
    family.family in family
}

run {} for 4 Person
""",
        category="social",
        difficulty=2
    ),
    
    SeedExample(
        description="Model a state machine with initial and final states.",
        alloy_code="""sig State {
    transitions: set State
}

one sig Initial extends State {}
sig Final extends State {}

fact ReachabilityRules {
    // Final states have no outgoing transitions
    no f: Final | some f.transitions
    
    // All states reachable from initial
    all s: State | s in Initial.*transitions
    
    // At least one final state is reachable
    some f: Final | f in Initial.*transitions
}

run {} for 5 State
""",
        category="automata",
        difficulty=2
    ),
    
    SeedExample(
        description="Model mutual exclusion with two processes and a lock.",
        alloy_code="""sig Process {
    var holding: lone Lock
}

sig Lock {}

pred init {
    no holding
}

pred acquire[p: Process, l: Lock] {
    // Lock is free
    no q: Process | l in q.holding
    // Process acquires lock
    holding' = holding + p->l
}

pred release[p: Process, l: Lock] {
    // Process holds lock
    l in p.holding
    // Process releases lock
    holding' = holding - p->l
}

pred stutter {
    holding' = holding
}

pred trans {
    some p: Process, l: Lock | acquire[p, l] or release[p, l]
    or stutter
}

fact Behavior {
    init and always trans
}

assert MutualExclusion {
    always (all l: Lock | lone p: Process | l in p.holding)
}

check MutualExclusion for 3 Process, 1 Lock, 5 steps
""",
        category="concurrency",
        difficulty=4
    ),
    
    SeedExample(
        description="Model a simple ordering relation that is total and transitive.",
        alloy_code="""sig Element {
    lt: set Element
}

fact TotalOrder {
    // Transitive
    all a, b, c: Element | (a->b in lt and b->c in lt) implies a->c in lt
    
    // Antisymmetric
    all a, b: Element | (a->b in lt and b->a in lt) implies a = b
    
    // Total
    all a, b: Element | a != b implies (a->b in lt or b->a in lt)
    
    // Irreflexive
    no e: Element | e->e in lt
}

run {} for 4 Element
""",
        category="ordering",
        difficulty=3
    ),
    
    SeedExample(
        description="Model a simple database with tables and foreign key constraints.",
        alloy_code="""sig Table {
    rows: set Row,
    primaryKey: set Column
}

sig Row {
    values: Column -> lone Value
}

sig Column {}
sig Value {}

sig ForeignKey {
    source: one Table,
    target: one Table,
    sourceCol: one Column,
    targetCol: one Column
}

fact ForeignKeyConstraint {
    all fk: ForeignKey |
        let srcRows = fk.source.rows |
        let tgtRows = fk.target.rows |
        all sr: srcRows | some tr: tgtRows |
            sr.values[fk.sourceCol] = tr.values[fk.targetCol]
}

run {} for 2 Table, 4 Row, 3 Column, 4 Value, 1 ForeignKey
""",
        category="database",
        difficulty=4
    )
]


def get_builtin_seeds() -> List[SeedExample]:
    """Get the built-in seed examples."""
    return BUILTIN_SEEDS.copy()


def main():
    """Example usage."""
    parser = argparse.ArgumentParser(description="Alloy Seed Data Utilities")
    parser.add_argument("--models-dir", help="Directory containing .als files")
    parser.add_argument("--output", help="Output file path")
    parser.add_argument("--list-builtin", action="store_true", help="List built-in seeds")
    args = parser.parse_args()
    
    if args.list_builtin:
        seeds = get_builtin_seeds()
        for i, seed in enumerate(seeds, 1):
            print(f"\n{i}. {seed.category} (difficulty {seed.difficulty})")
            print(f"   {seed.description[:80]}...")
    elif args.models_dir:
        seeds = create_seed_dataset(args.models_dir, args.output)
        print(f"Created {len(seeds)} seed examples")
    else:
        print("Use --list-builtin or --models-dir")


if __name__ == "__main__":
    main()
