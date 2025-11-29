/**
 * Alloy WebAssembly (WASM) module.
 * <p>
 * This package provides a simple, browser-compatible interface to the Alloy analyzer.
 * It is designed for:
 * <ul>
 *   <li>Web-based Alloy modeling tools</li>
 *   <li>LLM fine-tuning applications (like Jurity Alloy Forge Studio)</li>
 *   <li>Educational platforms</li>
 *   <li>Online validators and testers</li>
 * </ul>
 * <p>
 * The main entry point is {@link org.alloytools.alloy.wasm.AlloyWasm}.
 * <p>
 * <strong>Building for WASM:</strong>
 * <pre>
 * ./gradlew :org.alloytools.alloy.wasm:wasmBuild
 * </pre>
 * <p>
 * <strong>Usage in JavaScript:</strong>
 * <pre>
 * // Load the WASM module
 * import { AlloyWasm } from './alloy-wasm.js';
 * 
 * const alloy = new AlloyWasm();
 * 
 * // Execute an Alloy model
 * const result = JSON.parse(alloy.execute(`
 *     sig Person { friend: set Person }
 *     run { some p: Person | p in p.friend } for 3
 * `));
 * 
 * if (result.status === 'INSTANCE_FOUND') {
 *     console.log('Found instance!', result.instanceStats);
 * }
 * </pre>
 * 
 * @see org.alloytools.alloy.wasm.AlloyWasm
 */
package org.alloytools.alloy.wasm;
