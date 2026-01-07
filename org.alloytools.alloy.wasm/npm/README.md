# Alloy WebAssembly Module

This package provides the Alloy Analyzer compiled to WebAssembly, allowing you to use Alloy in web browsers and Node.js environments.

## Installation

```bash
npm install @alloytools/alloy-wasm
```

## Usage

### Browser

```html
<!DOCTYPE html>
<html>
<head>
    <title>Alloy WASM Example</title>
</head>
<body>
    <h1>Alloy in the Browser</h1>
    <textarea id="model" rows="10" cols="80">
sig Node {
    edges: set Node
}

pred acyclic {
    no n: Node | n in n.^edges
}

run acyclic for 5
    </textarea>
    <button onclick="runAlloy()">Execute</button>
    <pre id="output"></pre>

    <script src="dist/alloy.js"></script>
    <script>
        async function runAlloy() {
            try {
                const modelText = document.getElementById('model').value;
                const result = AlloyWasm.executeCommand(modelText, '');
                const parsed = JSON.parse(result);
                document.getElementById('output').textContent = JSON.stringify(parsed, null, 2);
            } catch (e) {
                document.getElementById('output').textContent = 'Error: ' + e.message;
            }
        }
    </script>
</body>
</html>
```

### Node.js

```javascript
const alloy = require('@alloytools/alloy-wasm');

const model = `
sig Node {
    edges: set Node
}

pred acyclic {
    no n: Node | n in n.^edges
}

run acyclic for 5
`;

// Parse the model
const parseResult = JSON.parse(alloy.parseModel(model));
console.log('Parse result:', parseResult);

// Execute a command
const execResult = JSON.parse(alloy.executeCommand(model, ''));
console.log('Execution result:', execResult);

if (execResult.success && execResult.satisfiable) {
    console.log('Found solution!');
    console.log('Solution:', JSON.stringify(execResult.solution, null, 2));
} else if (execResult.success) {
    console.log('No solution found (UNSAT)');
} else {
    console.log('Error:', execResult.error);
}
```

### TypeScript

```typescript
import { parseModel, executeCommand, AlloyExecutionResult } from '@alloytools/alloy-wasm';

const model = `
sig Person {
    friends: set Person
}

pred symmetricFriendship {
    all p1, p2: Person | p1 in p2.friends implies p2 in p1.friends
}

run symmetricFriendship for 4
`;

// Parse model
const parseResult = parseModel(model);
if (parseResult.success) {
    console.log(`Found ${parseResult.commandCount} commands`);
}

// Execute command
const result: AlloyExecutionResult = executeCommand(model);
if (result.success && result.satisfiable) {
    console.log('Solution found:', result.solution);
}
```

## API

### `parseModel(modelText: string): AlloyParseResult`

Parses an Alloy model and returns information about its structure.

**Parameters:**
- `modelText`: The Alloy model source code as a string

**Returns:**
A JSON object with:
- `success`: boolean indicating if parsing succeeded
- `moduleName`: name of the module (if successful)
- `commandCount`: number of commands in the model
- `commands`: array of command names
- `error`: error message (if unsuccessful)

### `executeCommand(modelText: string, commandName?: string): AlloyExecutionResult`

Executes an Alloy command and returns the solution.

**Parameters:**
- `modelText`: The Alloy model source code
- `commandName`: Name of the command to execute (optional, uses first command if empty)

**Returns:**
A JSON object with:
- `success`: boolean indicating if execution succeeded
- `satisfiable`: boolean indicating if a solution was found
- `command`: name of the executed command
- `solution`: the instance found (if satisfiable)
- `error`: error message (if unsuccessful)

### `getVersion(): AlloyVersionInfo`

Returns version information about the Alloy WASM module.

**Returns:**
A JSON object with version and backend information.

## Building from Source

To build the WASM module from source:

```bash
git clone https://github.com/AlloyTools/org.alloytools.alloy.git
cd org.alloytools.alloy
./gradlew :org.alloytools.alloy.wasm:build
```

The compiled WASM files will be in `org.alloytools.alloy.wasm/npm/dist/`.

## License

Apache License 2.0 - see LICENSE file for details.

## Links

- [Alloy Tools Website](https://alloytools.org)
- [Alloy Documentation](https://alloytools.org/documentation.html)
- [GitHub Repository](https://github.com/AlloyTools/org.alloytools.alloy)
