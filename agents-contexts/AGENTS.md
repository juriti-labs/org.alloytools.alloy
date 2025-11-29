# Alloy Tools - Agent Context Documentation

This directory contains context files and documentation designed for LLM agents and automated tools to work effectively with the Alloy formal specification language and tools.

## About Alloy

Alloy is a formal specification language based on first-order relational logic. It supports:
- Declarative modeling with signatures and relations
- First-order logic constraints with transitive closure
- Automated analysis via SAT solving
- Temporal logic (Alloy 6+)

## CLI Usage for Agents

### Self-Discovery

The Alloy CLI provides machine-readable descriptions of its capabilities:

```bash
# Get JSON description of CLI capabilities
alloy describe

# Get YAML description
alloy describe --format yaml
```

### Output Formats

The CLI supports multiple output formats optimized for different use cases:

| Format | Extension | Use Case |
|--------|-----------|----------|
| `json` | `.json` | Programmatic parsing, API integration |
| `yaml` | `.yaml` | Human-readable structured data, configuration |
| `xml` | `.xml` | Alloy XML format, tool integration |
| `dot` | `.dot` | GraphViz visualization |
| `rdf` | `.ttl` | Knowledge graph generation, semantic web |
| `table` | `.md` | Markdown tables, documentation |
| `text` | `.txt` | Plain text output |

### Example Commands

```bash
# Execute a model and get JSON output to stdout
alloy exec -t json -o - model.als

# Generate RDF knowledge graph
alloy exec -t rdf -o output/ model.als

# Find multiple solutions with YAML output
alloy exec -t yaml -r 5 model.als

# List available SAT solvers
alloy solvers

# List commands in a model
alloy commands model.als
```

## Knowledge Graph Generation

The RDF output format generates Turtle format suitable for knowledge graph integration:

- Uses standard RDF/RDFS vocabularies
- Custom Alloy ontology namespace: `http://alloytools.org/ontology#`
- Solution namespace: `http://alloytools.org/solution/`

### RDF Structure

```turtle
@prefix alloy: <http://alloytools.org/ontology#> .
@prefix sol: <http://alloytools.org/solution/> .

sol:solution_0 a alloy:Solution ;
    alloy:command "commandName" ;
    alloy:durationMs 42 .
```

## Verbose Output

Use the `--verbose` flag for detailed diagnostics:

- Level 0: Quiet (default)
- Level 1: Basic progress
- Level 2: Detailed solver information
- Level 3: Debug output

## Integration Tips for LLMs

1. **Start with describe**: Use `alloy describe` to understand available options
2. **Use JSON output**: Prefer `-t json -o -` for structured, parseable output
3. **Check commands first**: Use `alloy commands model.als` to list available commands
4. **Use quiet mode**: Add `-q` to suppress progress for cleaner output parsing
5. **Validate models**: Check for error output before parsing results

## Alloy Language Quick Reference

```alloy
// Signature declaration
sig Person {
    friends: set Person,
    age: one Int
}

// Constraints
fact NoSelfFriend {
    all p: Person | p not in p.friends
}

// Predicates
pred HasFriends[p: Person] {
    some p.friends
}

// Run command
run HasFriends for 5 Person

// Check assertion
assert FriendsSymmetric {
    all p, q: Person | p in q.friends implies q in p.friends
}
check FriendsSymmetric for 10 Person
```

## File Formats

### Input
- `.als` - Alloy source files

### Output (configurable via `-t`)
- `.json` - JSON solution data
- `.yaml` - YAML solution data
- `.xml` - Alloy XML format
- `.dot` - GraphViz graph
- `.ttl` - RDF Turtle format
- `.md` - Markdown tables
- `.txt` - Plain text

## Version Information

This documentation is for Alloy 6.x with LLM-friendly CLI extensions.
