# Alloy WebAssembly Module

This module provides a WebAssembly build of Alloy with a JSON interface for use in browsers and Node.js.

## Overview

The WASM module allows you to:
- Parse Alloy models in the browser
- Execute Alloy commands and get solutions
- Use Alloy as an NPM package in web applications

## Architecture

The module consists of:
- **AlloyWasm.java**: Main Java interface class with JSON-based API
- **NPM Package**: Package configuration for distribution
- **Build Scripts**: Configuration for compiling Java to WebAssembly

## Building the WASM Module

### Prerequisites

- Java 17 or later
- Gradle 7.2 or later (included via gradlew)
- Maven 3.8 or later (for TeaVM compilation)

### Option 1: Using Gradle (Recommended)

First, build the Alloy core libraries:

```bash
cd /path/to/org.alloytools.alloy
./gradlew :org.alloytools.alloy.wasm:build
```

This creates the compiled Java classes that can be used with TeaVM.

### Option 2: Using TeaVM with Maven

TeaVM (http://teavm.org) is used to compile Java bytecode to WebAssembly.

1. First build the dependencies:
```bash
./gradlew :org.alloytools.alloy.core:build
./gradlew :org.alloytools.api:build
./gradlew :org.alloytools.pardinus.core:build
```

2. Then compile to WASM using the provided Maven configuration:
```bash
cd org.alloytools.alloy.wasm
mvn clean package -f teavm-pom.xml
```

This will generate:
- `target/wasm/` - WebAssembly files
- `target/js/` - JavaScript wrapper files

3. Copy the generated files to the npm package:
```bash
cp target/wasm/* npm/dist/
cp target/js/alloy.js npm/dist/
```

### Option 3: Alternative WASM Compilers

If TeaVM doesn't work for your use case, consider:

- **CheerpJ**: Commercial tool for compiling Java to WebAssembly with better library support
- **GraalVM**: Native image compilation with WASM backend (experimental)

## NPM Package

### Publishing

After building the WASM files:

```bash
cd org.alloytools.alloy.wasm/npm
npm pack
```

To publish to NPM:

```bash
npm publish
```

### Installation

Users can install the package:

```bash
npm install @alloytools/alloy-wasm
```

## Usage Examples

See the [NPM README](npm/README.md) for detailed usage examples.

Quick example:

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

const result = JSON.parse(alloy.executeCommand(model, ''));
console.log('Satisfiable:', result.satisfiable);
```

## Browser Demo

A complete browser demo is available in `examples/browser-demo.html`. Open it in a web browser after building the WASM module.

## API Reference

### AlloyWasm Class

The main interface provides three static methods:

#### `parseModel(String modelText): String`

Parses an Alloy model and returns model information as JSON.

**Returns:**
```json
{
  "success": true,
  "moduleName": "example",
  "commandCount": 2,
  "commands": ["run1", "check1"]
}
```

#### `executeCommand(String modelText, String commandName): String`

Executes an Alloy command and returns the solution as JSON.

**Parameters:**
- `modelText`: Alloy source code
- `commandName`: Command to execute (empty string for first command)

**Returns:**
```json
{
  "success": true,
  "satisfiable": true,
  "command": "acyclic",
  "solution": {
    "atoms": {...},
    "tuples": {...}
  }
}
```

#### `getVersion(): String`

Returns version information.

**Returns:**
```json
{
  "success": true,
  "version": "6.0.0-WASM",
  "backend": "WebAssembly"
}
```

## Current Limitations

This is an initial implementation with some limitations:

1. **Native SAT Solvers**: Only SAT4J (pure Java solver) is included. Native solvers (MiniSat, Glucose) are not available in WebAssembly.

2. **Visualization**: The graphical visualization components are not included.

3. **File I/O**: File system operations are limited in the browser environment.

4. **Performance**: WASM performance may be slower than native Java for complex models.

5. **Memory**: Large models may exceed browser memory limits.

## Future Improvements

Potential enhancements:

- [ ] Add support for multiple solutions (next() iteration)
- [ ] Include more SAT solvers compiled to WASM
- [ ] Add visualization generation (SVG/Canvas output)
- [ ] Optimize binary size
- [ ] Add streaming API for large models
- [ ] Provide worker thread support
- [ ] Add TypeScript strict types

## Contributing

Contributions are welcome! Please see the main [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

Apache License 2.0 - see [LICENSE](../LICENSE) for details.

## Resources

- [Alloy Website](https://alloytools.org)
- [TeaVM Documentation](http://teavm.org/docs/)
- [WebAssembly Specification](https://webassembly.org/)
