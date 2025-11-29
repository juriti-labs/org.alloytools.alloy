package org.alloytools.alloy.training;

import java.util.ArrayList;
import java.util.List;

import org.alloytools.alloy.dto.SolutionDTO;

/**
 * Represents the response from the Alloy Oracle service.
 * This provides normalized output suitable for LLM training pipelines.
 */
public class OracleResponse {
    
    /**
     * The execution status.
     */
    public OracleStatus status;
    
    /**
     * List of structured errors (for PARSE_ERROR and TYPE_ERROR).
     */
    public List<OracleError> errors = new ArrayList<>();
    
    /**
     * Summary of the model/instance (for INSTANCE_FOUND).
     */
    public InstanceStats instanceStats;
    
    /**
     * The full solution DTO when an instance was found.
     */
    public SolutionDTO solution;
    
    /**
     * Execution time in milliseconds.
     */
    public long durationMs;
    
    /**
     * Whether the result came from a check command (true) or run command (false).
     */
    public boolean isCheck;
    
    /**
     * The command label that was executed.
     */
    public String commandLabel;
    
    /**
     * Raw stdout output from the execution.
     */
    public String stdout;
    
    /**
     * Raw stderr output from the execution.
     */
    public String stderr;
    
    /**
     * Human-readable summary of the result.
     */
    public String summary;
    
    /**
     * Default constructor.
     */
    public OracleResponse() {
    }
    
    /**
     * Create a response with a status.
     */
    public OracleResponse(OracleStatus status) {
        this.status = status;
    }
    
    /**
     * Add an error to this response.
     */
    public OracleResponse addError(OracleError error) {
        this.errors.add(error);
        return this;
    }
    
    /**
     * Check if the execution was successful (found instance or verified assertion).
     */
    public boolean isSuccess() {
        return status == OracleStatus.INSTANCE_FOUND || status == OracleStatus.NO_INSTANCE;
    }
    
    /**
     * Check if there were compilation errors.
     */
    public boolean hasCompileErrors() {
        return status == OracleStatus.PARSE_ERROR || status == OracleStatus.TYPE_ERROR;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OracleResponse{status=").append(status);
        if (commandLabel != null) {
            sb.append(", command=").append(commandLabel);
        }
        if (!errors.isEmpty()) {
            sb.append(", errors=").append(errors.size());
        }
        sb.append(", duration=").append(durationMs).append("ms}");
        return sb.toString();
    }
}
