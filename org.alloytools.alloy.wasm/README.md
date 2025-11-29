# Alloy WASM Module

This module provides WebAssembly/JavaScript support for running Alloy directly in web browsers.

## Overview

The Alloy WASM module allows you to:
- Parse and execute Alloy models in the browser
- Validate Alloy syntax and types
- Get structured JSON responses suitable for web applications

This module is designed for integration with:
- **Jurity Alloy Forge Studio** - LLM fine-tuning applications
- Educational platforms
- Online Alloy validators
- Web-based modeling tools

## Building

### Building the Java Module

```bash
./gradlew :org.alloytools.alloy.wasm:build
```

### Building JavaScript/WASM Output

The module uses [TeaVM](https://teavm.org/) to compile Java to JavaScript/WebAssembly:

```bash
./gradlew :org.alloytools.alloy.wasm:wasmBuild
```

This creates the following files in `build/wasm/`:
- `classes.js` - Compiled JavaScript
- Demo HTML file

## API

### AlloyWasm Class

The main entry point is the `AlloyWasm` class which provides these methods:

#### `execute(moduleText: String): String`

Execute an Alloy module and return JSON result.

**Parameters:**
- `moduleText` - The Alloy module source code

**Returns:** JSON string with execution result

**Example:**
```javascript
const alloy = new AlloyWasm();
const result = JSON.parse(alloy.execute(`
    sig Person { friend: set Person }
    run { some p: Person | p in p.^friend } for 3
`));

if (result.status === 'instance-found') {
    console.log('Found instance with', result.instanceStats.atomCount, 'atoms');
}
```

#### `execute(moduleText: String, commandName: String, timeoutMs: long): String`

Execute with specific command and timeout.

**Parameters:**
- `moduleText` - The Alloy module source code
- `commandName` - Command name or index (null for first)
- `timeoutMs` - Timeout in milliseconds

#### `parse(moduleText: String): String`

Parse and type-check without executing.

**Example:**
```javascript
const result = JSON.parse(alloy.parse(`
    sig Person { friend: set Person }
    run {} for 3
`));

if (result.errors.length === 0) {
    console.log('Model is valid!');
}
```

#### `listCommands(moduleText: String): String`

List all commands in a module.

**Returns:** JSON array of command descriptions

#### `getVersion(): String`

Get version information.

**Returns:** JSON object with version info

#### `getSolvers(): String`

Get available SAT solvers (only Sat4j in WASM mode).

## Response Format

### Execute Response

```json
{
    "status": "instance-found",
    "durationMs": 123,
    "isCheck": false,
    "commandLabel": "run$1",
    "summary": "Instance found - constraints are satisfiable",
    "errors": [],
    "instanceStats": {
        "atomCount": 3,
        "signatureCount": 1,
        "relationCount": 1,
        "tupleCount": 2,
        "skolemCount": 0,
        "traceLength": 1,
        "loopState": -1
    },
    "hasSolution": true,
    "solution": { ... }
}
```

### Status Values

| Status | Description |
|--------|-------------|
| `instance-found` | Satisfiable - an instance was found |
| `no-instance` | Unsatisfiable within the given scope |
| `parse-error` | Syntax error in the model |
| `type-error` | Type checking error |
| `timeout` | Solver timed out |
| `error` | Other execution error |

### Error Format

```json
{
    "kind": "parse",
    "message": "Expected signature name",
    "lineStart": 1,
    "columnStart": 5,
    "lineEnd": 1,
    "columnEnd": 10,
    "file": ""
}
```

## Integration with Jurity Alloy Forge Studio

This module is designed for integration with [Jurity Alloy Forge Studio](https://github.com/juriti-labs/jurity-alloy-forge-studio), an LLM fine-tuning application for Alloy models.

### Example Integration

```javascript
// Import the Alloy WASM module
import { AlloyWasm } from './alloy-wasm.js';

// Create an instance
const alloy = new AlloyWasm();

// Function to validate training examples
async function validateTrainingExample(alloyCode) {
    const result = JSON.parse(alloy.parse(alloyCode));
    return {
        valid: result.errors.length === 0,
        errors: result.errors
    };
}

// Function to execute and get structured output
async function executeForTraining(alloyCode) {
    const result = JSON.parse(alloy.execute(alloyCode));
    return {
        satisfiable: result.status === 'INSTANCE_FOUND',
        stats: result.instanceStats,
        solution: result.solution
    };
}
```

## Demo

The module includes a demo HTML page at `src/main/web/index.html` that provides:
- Interactive Alloy editor
- Run/Parse buttons
- Example models
- Structured output display

## Limitations

When running in WASM/JavaScript mode:

1. **SAT Solver**: Only Sat4j (pure Java) is available. Native solvers cannot be used.
2. **Performance**: JavaScript execution is slower than native JVM.
3. **Memory**: Browser memory limits apply.
4. **Concurrency**: Single-threaded execution.

## License

MIT License - See the main project LICENSE file.
