package org.alloytools.alloy.training;

/**
 * Represents different types of tasks for LLM training on Alloy.
 */
public enum TrainingTaskType {
    /**
     * Natural language to Alloy specification.
     * Input: NL description/constraints
     * Output: Alloy module with sigs, facts, preds, check/run
     */
    NL_TO_ALLOY("nl-to-alloy"),
    
    /**
     * Fix compiler/parse errors.
     * Input: Original Alloy code + compiler error message
     * Output: Corrected Alloy code that compiles
     */
    FIX_PARSE_ERROR("fix-parse-error"),
    
    /**
     * Fix type errors.
     * Input: Original Alloy code + type error message
     * Output: Corrected Alloy code that type-checks
     */
    FIX_TYPE_ERROR("fix-type-error"),
    
    /**
     * Generate property/assertion.
     * Input: Alloy module
     * Output: Meaningful assert/check pairs
     */
    GENERATE_ASSERTION("generate-assertion"),
    
    /**
     * Tool-call control for function calling.
     * Input: User goal + context
     * Output: Structured tool call JSON
     */
    TOOL_CALL("tool-call"),
    
    /**
     * Explain counterexample.
     * Input: Alloy module + counterexample
     * Output: Natural language explanation
     */
    EXPLAIN_COUNTEREXAMPLE("explain-counterexample"),
    
    /**
     * Refine specification based on counterexample.
     * Input: Alloy module + counterexample + explanation
     * Output: Refined Alloy module
     */
    REFINE_SPEC("refine-spec");
    
    private final String id;
    
    TrainingTaskType(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public static TrainingTaskType fromId(String id) {
        for (TrainingTaskType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
