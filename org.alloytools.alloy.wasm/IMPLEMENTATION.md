# Alloy WebAssembly Implementation Notes

## Overview

This document describes the WebAssembly (WASM) build configuration for Alloy, including the architecture, implementation approach, and current status.

## Architecture

### Core Components

1. **AlloyWasm.java**: Main interface class providing three static methods:
   - `parseModel(String)`: Parse an Alloy model and return structure as JSON
   - `executeCommand(String, String)`: Execute a command and return solution as JSON
   - `getVersion()`: Return version information

2. **JSON Interface**: All inputs and outputs use JSON for easy JavaScript interop:
   - Input: Alloy model text as String
   - Output: JSON strings with structured data
   - Error handling: Consistent error response format

3. **NPM Package**: Ready-to-publish package structure:
   - `package.json`: Package metadata and dependencies
   - `alloy.d.ts`: TypeScript type definitions
   - `dist/`: Output directory for compiled WASM/JS files
   - `README.md`: Usage documentation

### Design Decisions

#### Pure Java API
The interface uses pure Java (no TeaVM-specific annotations) for maximum compatibility with different compilation tools:
- TeaVM
- GraalVM Native Image
- CheerpJ
- Future WASM compilers

#### JSON for Interoperability
JSON was chosen over Protocol Buffers or other formats because:
- Universal browser support
- Easy to debug and inspect
- Natural fit for JavaScript
- Existing Alloy DTO classes serialize well to JSON

#### SAT4J Solver Only
The WASM build uses only SAT4J (pure Java) solver because:
- Native solvers (MiniSat, Glucose, etc.) can't be easily compiled to WASM
- SAT4J provides reasonable performance for small-to-medium models
- Simplifies the build process significantly

## Implementation Status

### Completed ✓

- [x] Basic project structure and build configuration
- [x] AlloyWasm interface class with JSON API
- [x] NPM package structure with TypeScript definitions
- [x] Comprehensive test suite
- [x] Example HTML browser demo
- [x] Build scripts and documentation
- [x] Main README updates

### Limitations and Known Issues

1. **WASM Compilation Not Automated**
   - TeaVM configuration is provided but not fully automated
   - Requires manual Maven setup or alternative tooling
   - Compilation is complex due to Alloy's large dependency tree

2. **Solver Limitations**
   - Only SAT4J solver available (pure Java)
   - Native solvers not feasible in WASM environment
   - May be slower than native for large models

3. **No Visualization**
   - Graphical visualization components not included
   - Only returns data structures (atoms, tuples)
   - Visualization would need to be implemented in JavaScript

4. **File System Limitations**
   - Browser environment has no traditional file system
   - Model imports/includes may not work
   - All models must be self-contained strings

5. **Memory Constraints**
   - Browser memory limits apply
   - Large models may exceed available memory
   - No control over WASM memory configuration from JavaScript

## Build Approaches

### Option 1: TeaVM (Recommended for Production)

TeaVM is an ahead-of-time compiler that produces optimized WASM:

**Pros:**
- Small output size (relative to other options)
- Good performance
- Active development
- Browser-focused

**Cons:**
- Complex configuration
- Limited Java library support
- Requires careful dependency management

**Setup:**
```bash
cd org.alloytools.alloy.wasm
mvn clean package -f teavm-pom.xml
```

### Option 2: GraalVM Native Image (Experimental)

GraalVM can compile Java to WASM using its native image tool:

**Pros:**
- Better Java library support
- Official Oracle support
- Growing ecosystem

**Cons:**
- Larger binary size
- Still experimental for WASM
- Limited browser API access

**Setup:**
Requires GraalVM with WASM backend (experimental feature).

### Option 3: CheerpJ (Commercial)

CheerpJ can run unmodified Java applications in the browser:

**Pros:**
- No code modifications needed
- Complete Java library support
- Mature product

**Cons:**
- Commercial license required
- Larger binary size (includes JVM)
- Performance overhead

### Option 4: Direct JVM in Browser (Future)

Future options may include:
- WebAssembly GC proposal support
- J2WASM (Google's experimental compiler)
- Other emerging tools

## Performance Considerations

Expected performance characteristics:

1. **Parsing**: ~1-5ms for typical models (similar to native)
2. **Execution**: 2-10x slower than native Java (WASM overhead)
3. **Memory**: 50-200MB baseline for WASM runtime
4. **Startup**: 100-500ms to initialize WASM module

These are estimates and will vary based on:
- Browser and JavaScript engine
- WASM compilation tool used
- Model complexity
- Available system resources

## Security Considerations

1. **Sandboxing**: WASM runs in browser sandbox (good for security)
2. **No Network Access**: Compiled code can't make network requests
3. **Memory Safety**: WASM provides memory isolation
4. **Input Validation**: Always validate model text before execution

## Future Enhancements

### Short Term
- [ ] Automate TeaVM compilation in Gradle build
- [ ] Add more comprehensive examples
- [ ] Optimize binary size
- [ ] Add progress callbacks for long-running operations

### Medium Term
- [ ] JavaScript visualization library for solutions
- [ ] Support for model iteration (next() solutions)
- [ ] Web Workers support for async execution
- [ ] Streaming API for large models

### Long Term
- [ ] Port visualization to Canvas/SVG
- [ ] WASM-optimized SAT solver
- [ ] Multi-threaded solving (when WASM supports it)
- [ ] Full IDE in browser

## Testing

Run tests with:
```bash
./gradlew :org.alloytools.alloy.wasm:test
```

Tests cover:
- Version information
- Model parsing (valid and invalid)
- Command execution
- Error handling
- JSON serialization

## Contributing

To contribute to the WASM build:

1. Ensure changes work with pure Java (no TeaVM-specific code)
2. Maintain JSON interface compatibility
3. Add tests for new functionality
4. Update documentation
5. Test with multiple compilation approaches if possible

## Resources

- [TeaVM Documentation](http://teavm.org/)
- [WebAssembly Specification](https://webassembly.org/)
- [Alloy Documentation](https://alloytools.org/documentation.html)
- [GraalVM WASM](https://www.graalvm.org/latest/reference-manual/wasm/)

## License

This WASM build maintains the same Apache License 2.0 as the main Alloy project.
