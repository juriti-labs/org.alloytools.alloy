package org.alloytools.alloy.training;

/**
 * Represents a structured error from the Alloy compiler.
 * This provides machine-readable error information for LLM training.
 */
public class OracleError {
    
    /**
     * The kind of error: "parse", "type", "semantic", or "internal".
     */
    public String kind;
    
    /**
     * Human-readable error message.
     */
    public String message;
    
    /**
     * The file path where the error occurred (may be null for in-memory modules).
     */
    public String file;
    
    /**
     * Line number where the error starts (1-based).
     */
    public int lineStart;
    
    /**
     * Column number where the error starts (1-based).
     */
    public int columnStart;
    
    /**
     * Line number where the error ends (1-based).
     */
    public int lineEnd;
    
    /**
     * Column number where the error ends (1-based).
     */
    public int columnEnd;
    
    /**
     * Default constructor for serialization.
     */
    public OracleError() {
    }
    
    /**
     * Create an error with all fields.
     */
    public OracleError(String kind, String message, String file, 
                       int lineStart, int columnStart, 
                       int lineEnd, int columnEnd) {
        this.kind = kind;
        this.message = message;
        this.file = file;
        this.lineStart = lineStart;
        this.columnStart = columnStart;
        this.lineEnd = lineEnd;
        this.columnEnd = columnEnd;
    }
    
    /**
     * Create a simple error without location info.
     */
    public OracleError(String kind, String message) {
        this.kind = kind;
        this.message = message;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(kind).append(": ").append(message);
        if (lineStart > 0) {
            sb.append(" at line ").append(lineStart);
            if (columnStart > 0) {
                sb.append(":").append(columnStart);
            }
        }
        return sb.toString();
    }
}
