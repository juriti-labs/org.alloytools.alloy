package org.alloytools.alloy.training;

/**
 * Represents the status of an Alloy oracle execution.
 * These statuses are used to classify results for LLM training purposes.
 */
public enum OracleStatus {
    /**
     * The module failed to parse (syntax error).
     */
    PARSE_ERROR("parse-error"),
    
    /**
     * The module parsed but had type-checking errors.
     */
    TYPE_ERROR("type-error"),
    
    /**
     * A satisfying instance was found (for run commands) 
     * or a counterexample was found (for check commands).
     */
    INSTANCE_FOUND("instance-found"),
    
    /**
     * No instance satisfies the constraints (for run commands)
     * or the assertion holds (for check commands).
     */
    NO_INSTANCE("no-instance"),
    
    /**
     * The solver timed out before finding a result.
     */
    TIMEOUT("timeout"),
    
    /**
     * An unexpected error occurred during execution.
     */
    ERROR("error");
    
    private final String id;
    
    OracleStatus(String id) {
        this.id = id;
    }
    
    /**
     * Returns the machine-readable identifier for this status.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns the status for a given identifier.
     * @param id the identifier (e.g., "parse-error")
     * @return the corresponding OracleStatus, or null if not found
     */
    public static OracleStatus fromId(String id) {
        for (OracleStatus status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return null;
    }
}
