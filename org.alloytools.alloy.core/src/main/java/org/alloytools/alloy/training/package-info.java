/**
 * LLM Training Infrastructure for Alloy.
 * 
 * This package provides tools for generating training data for fine-tuning
 * Large Language Models (LLMs) on Alloy formal specification tasks.
 * 
 * <h2>Key Components</h2>
 * 
 * <h3>Oracle Service</h3>
 * <ul>
 *   <li>{@link org.alloytools.alloy.training.AlloyOracle} - Wraps the Alloy analyzer
 *       as an oracle service that returns structured, normalized output.</li>
 *   <li>{@link org.alloytools.alloy.training.OracleStatus} - Status codes for
 *       oracle execution (parse-error, type-error, instance-found, etc.)</li>
 *   <li>{@link org.alloytools.alloy.training.OracleResponse} - Structured response
 *       from the oracle with errors, stats, and solutions.</li>
 *   <li>{@link org.alloytools.alloy.training.OracleError} - Structured error
 *       information with location (kind, message, line, column).</li>
 * </ul>
 * 
 * <h3>Training Data Generation</h3>
 * <ul>
 *   <li>{@link org.alloytools.alloy.training.TrainingDataGenerator} - Implements
 *       the compiler-in-the-loop training loop for generating SFT data.</li>
 *   <li>{@link org.alloytools.alloy.training.TrainingExample} - Represents a
 *       single training example with input/output formatting.</li>
 *   <li>{@link org.alloytools.alloy.training.TrainingTaskType} - Task types
 *       (NL→Alloy, fix-errors, generate-assertion, tool-call).</li>
 * </ul>
 * 
 * <h2>Training Loop</h2>
 * 
 * The typical training loop works as follows:
 * <ol>
 *   <li>Generate candidate Alloy code (from an LLM or manually)</li>
 *   <li>Run through the oracle to get structured feedback</li>
 *   <li>Classify results:
 *     <ul>
 *       <li>Successful (instance-found, no-instance) → positive training example</li>
 *       <li>Parse/type errors → "fix-the-code" training task</li>
 *       <li>Timeout → "simplify" or discard</li>
 *     </ul>
 *   </li>
 *   <li>Optionally add human corrections for failed examples</li>
 *   <li>Export to JSONL for fine-tuning</li>
 * </ol>
 * 
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * // Create generator
 * TrainingDataGenerator generator = new TrainingDataGenerator();
 * generator.setGeneration(0);
 * 
 * // Add hand-curated seed examples
 * generator.addPositiveExample(
 *     "Model a simple directed graph with nodes and edges",
 *     "sig Node { edges: set Node }\n" +
 *     "run { some Node } for 3",
 *     TrainingTaskType.NL_TO_ALLOY
 * );
 * 
 * // Process model-generated candidates
 * OracleResponse response = generator.processCandidate(
 *     "Model a binary tree",
 *     modelOutput
 * );
 * 
 * // Export for fine-tuning
 * generator.exportToJsonl(new File("training_data.jsonl"));
 * 
 * // Cleanup
 * generator.shutdown();
 * }</pre>
 * 
 * @see org.alloytools.alloy.training.AlloyOracle
 * @see org.alloytools.alloy.training.TrainingDataGenerator
 */
package org.alloytools.alloy.training;
