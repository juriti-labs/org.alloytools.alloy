package org.alloytools.alloy.training;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Represents a single training example for SFT (Supervised Fine-Tuning).
 * Designed to be serializable to JSONL format for vendor fine-tuning APIs.
 */
public class TrainingExample {
    
    /**
     * The type of task this example represents.
     */
    public TrainingTaskType taskType;
    
    /**
     * Unique identifier for this example.
     */
    public String id;
    
    /**
     * The natural language instruction or description.
     */
    public String instruction;
    
    /**
     * Optional Alloy context (existing code, module, etc.).
     */
    public String alloyContext;
    
    /**
     * Optional compiler feedback (for fix-error tasks).
     */
    public String compilerFeedback;
    
    /**
     * The target output (Alloy code, tool call, etc.).
     */
    public String targetOutput;
    
    /**
     * Optional tool call in structured format.
     */
    public ToolCall toolCall;
    
    /**
     * Metadata about this example.
     */
    public Map<String, String> metadata = new LinkedHashMap<>();
    
    /**
     * Oracle response that validated this example (if applicable).
     */
    public OracleResponse oracleResponse;
    
    /**
     * Whether this is a positive example (model output was correct).
     */
    public boolean isPositive = true;
    
    /**
     * Generation number (for iterative training).
     */
    public int generation;
    
    /**
     * Default constructor.
     */
    public TrainingExample() {
    }
    
    /**
     * Create a training example with basic fields.
     */
    public TrainingExample(TrainingTaskType taskType, String instruction, String targetOutput) {
        this.taskType = taskType;
        this.instruction = instruction;
        this.targetOutput = targetOutput;
    }
    
    /**
     * Format the input section for SFT training.
     * Uses a structured format that can be customized.
     */
    public String formatInput() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("<INSTRUCTION>\n");
        sb.append(instruction);
        sb.append("\n</INSTRUCTION>\n");
        
        if (alloyContext != null && !alloyContext.isEmpty()) {
            sb.append("<ALLOY_CONTEXT>\n");
            sb.append(alloyContext);
            sb.append("\n</ALLOY_CONTEXT>\n");
        }
        
        if (compilerFeedback != null && !compilerFeedback.isEmpty()) {
            sb.append("<COMPILER_FEEDBACK>\n");
            sb.append(compilerFeedback);
            sb.append("\n</COMPILER_FEEDBACK>\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Format this example as a simple input/output pair.
     * Suitable for OpenAI-style SFT format.
     */
    public Map<String, String> toSimpleFormat() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("input", formatInput());
        result.put("output", targetOutput);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("TrainingExample{type=%s, id=%s, positive=%s}", 
            taskType, id, isPositive);
    }
    
    /**
     * Represents a tool call for function-calling style training.
     */
    public static class ToolCall {
        public String tool;
        public Map<String, Object> args = new LinkedHashMap<>();
        
        public ToolCall() {
        }
        
        public ToolCall(String tool) {
            this.tool = tool;
        }
        
        public ToolCall arg(String key, Object value) {
            this.args.put(key, value);
            return this;
        }
    }
}
