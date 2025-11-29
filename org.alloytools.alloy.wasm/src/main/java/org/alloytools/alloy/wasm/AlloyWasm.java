package org.alloytools.alloy.wasm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alloytools.alloy.dto.FieldDTO;
import org.alloytools.alloy.dto.InstanceDTO;
import org.alloytools.alloy.dto.SigDefDTO;
import org.alloytools.alloy.dto.SolutionDTO;
import org.alloytools.alloy.dto.TuplesDTO;
import org.alloytools.alloy.training.AlloyOracle;
import org.alloytools.alloy.training.InstanceStats;
import org.alloytools.alloy.training.OracleError;
import org.alloytools.alloy.training.OracleResponse;
import org.alloytools.alloy.training.OracleStatus;

import edu.mit.csail.sdg.translator.A4Options;

/**
 * WebAssembly/JavaScript facade for the Alloy analyzer.
 * <p>
 * This class provides a simple, stateless API designed for:
 * - Browser-based Alloy modeling tools
 * - LLM fine-tuning applications (like Jurity Alloy Forge Studio)
 * - Educational platforms
 * - Online validators/testers
 * <p>
 * All methods return JSON strings for easy JavaScript interoperability.
 * <p>
 * Example usage from JavaScript:
 * <pre>
 * const alloy = new AlloyWasm();
 * const result = alloy.execute(`
 *     sig Person { friend: set Person }
 *     run { some p: Person | p in p.friend } for 3
 * `);
 * const data = JSON.parse(result);
 * if (data.status === 'INSTANCE_FOUND') {
 *     console.log('Found instance with', data.instanceStats.atomCount, 'atoms');
 * }
 * </pre>
 */
public class AlloyWasm {
    
    private static final long DEFAULT_TIMEOUT_MS = 30000;
    
    /**
     * Create a new AlloyWasm instance.
     */
    public AlloyWasm() {
    }
    
    /**
     * Execute an Alloy module and return JSON result.
     * Runs the first command in the module.
     * 
     * @param moduleText the Alloy module source code
     * @return JSON string containing execution result
     */
    public String execute(String moduleText) {
        return execute(moduleText, null, DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * Execute an Alloy module with options.
     * 
     * @param moduleText the Alloy module source code
     * @param commandName the command name or index to run (null for first)
     * @param timeoutMs timeout in milliseconds
     * @return JSON string containing execution result
     */
    public String execute(String moduleText, String commandName, long timeoutMs) {
        A4Options options = new A4Options();
        AlloyOracle oracle = new AlloyOracle(options, timeoutMs);
        
        try {
            OracleResponse response = oracle.run(moduleText, commandName);
            return toJson(response);
        } finally {
            oracle.shutdown();
        }
    }
    
    /**
     * Parse and type-check an Alloy module without executing.
     * Useful for syntax validation and error checking.
     * 
     * @param moduleText the Alloy module source code
     * @return JSON string containing parse result
     */
    public String parse(String moduleText) {
        A4Options options = new A4Options();
        AlloyOracle oracle = new AlloyOracle(options, DEFAULT_TIMEOUT_MS);
        
        try {
            OracleResponse response = oracle.parseOnly(moduleText);
            return toJson(response);
        } finally {
            oracle.shutdown();
        }
    }
    
    /**
     * List all commands in an Alloy module.
     * 
     * @param moduleText the Alloy module source code
     * @return JSON array of command descriptions
     */
    public String listCommands(String moduleText) {
        A4Options options = new A4Options();
        AlloyOracle oracle = new AlloyOracle(options, DEFAULT_TIMEOUT_MS);
        
        try {
            List<String> commands = oracle.listCommands(moduleText);
            return toJsonArray(commands);
        } finally {
            oracle.shutdown();
        }
    }
    
    /**
     * Get version information about this WASM module.
     * 
     * @return JSON string with version info
     */
    public String getVersion() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"name\":\"alloy-wasm\",");
        sb.append("\"version\":\"6.3.0\",");
        sb.append("\"alloyVersion\":\"6\",");
        sb.append("\"description\":\"Alloy Analyzer for WebAssembly/JavaScript\",");
        sb.append("\"capabilities\":[\"parse\",\"execute\",\"validate\"]");
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Get a list of available SAT solvers.
     * Note: In WASM mode, only pure Java solvers (Sat4j) are available.
     * 
     * @return JSON array of solver names
     */
    public String getSolvers() {
        // In WASM, only Sat4j is available (pure Java)
        return "[\"sat4j\"]";
    }
    
    // ---- JSON serialization helpers ----
    
    private String toJson(OracleResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // Status
        sb.append("\"status\":");
        if (response.status != null) {
            sb.append("\"").append(response.status.getId()).append("\"");
        } else {
            sb.append("\"success\"");
        }
        sb.append(",");
        
        // Duration
        sb.append("\"durationMs\":").append(response.durationMs).append(",");
        
        // Command info
        sb.append("\"isCheck\":").append(response.isCheck).append(",");
        if (response.commandLabel != null) {
            sb.append("\"commandLabel\":\"").append(escapeJson(response.commandLabel)).append("\",");
        }
        
        // Summary
        if (response.summary != null) {
            sb.append("\"summary\":\"").append(escapeJson(response.summary)).append("\",");
        }
        
        // Errors
        sb.append("\"errors\":[");
        for (int i = 0; i < response.errors.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(errorToJson(response.errors.get(i)));
        }
        sb.append("],");
        
        // Instance stats
        if (response.instanceStats != null) {
            sb.append("\"instanceStats\":").append(statsToJson(response.instanceStats)).append(",");
        }
        
        // Solution (if present)
        if (response.solution != null) {
            sb.append("\"solution\":").append(solutionToJson(response.solution)).append(",");
        }
        
        // Has solution flag (convenience)
        sb.append("\"hasSolution\":").append(response.solution != null);
        
        sb.append("}");
        return sb.toString();
    }
    
    private String errorToJson(OracleError error) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"kind\":\"").append(escapeJson(error.kind)).append("\",");
        sb.append("\"message\":\"").append(escapeJson(error.message)).append("\"");
        if (error.lineStart > 0) {
            sb.append(",\"lineStart\":").append(error.lineStart);
            sb.append(",\"columnStart\":").append(error.columnStart);
            sb.append(",\"lineEnd\":").append(error.lineEnd);
            sb.append(",\"columnEnd\":").append(error.columnEnd);
        }
        if (error.file != null && !error.file.isEmpty()) {
            sb.append(",\"file\":\"").append(escapeJson(error.file)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String statsToJson(InstanceStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"atomCount\":").append(stats.atomCount).append(",");
        sb.append("\"signatureCount\":").append(stats.signatureCount).append(",");
        sb.append("\"relationCount\":").append(stats.relationCount).append(",");
        sb.append("\"tupleCount\":").append(stats.tupleCount).append(",");
        sb.append("\"skolemCount\":").append(stats.skolemCount).append(",");
        sb.append("\"traceLength\":").append(stats.traceLength).append(",");
        sb.append("\"loopState\":").append(stats.loopState);
        sb.append("}");
        return sb.toString();
    }
    
    private String solutionToJson(SolutionDTO solution) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"duration\":").append(solution.duration).append(",");
        sb.append("\"incremental\":").append(solution.incremental).append(",");
        sb.append("\"loopstate\":").append(solution.loopstate).append(",");
        sb.append("\"utctime\":").append(solution.utctime).append(",");
        if (solution.localtime != null) {
            sb.append("\"localtime\":\"").append(escapeJson(solution.localtime)).append("\",");
        }
        if (solution.timezone != null) {
            sb.append("\"timezone\":\"").append(escapeJson(solution.timezone)).append("\",");
        }
        
        // Instances
        sb.append("\"instances\":[");
        for (int i = 0; i < solution.instances.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(instanceToJson(solution.instances.get(i)));
        }
        sb.append("],");
        
        // Signatures
        sb.append("\"sigs\":{");
        boolean firstSig = true;
        for (Map.Entry<String, SigDefDTO> entry : solution.sigs.entrySet()) {
            if (!firstSig) sb.append(",");
            firstSig = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            sb.append(sigDefToJson(entry.getValue()));
        }
        sb.append("}");
        
        sb.append("}");
        return sb.toString();
    }
    
    private String instanceToJson(InstanceDTO instance) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"state\":").append(instance.state).append(",");
        
        // Values
        sb.append("\"values\":{");
        boolean firstSig = true;
        for (Map.Entry<String, Map<String, String[][]>> sigEntry : instance.values.entrySet()) {
            if (!firstSig) sb.append(",");
            firstSig = false;
            sb.append("\"").append(escapeJson(sigEntry.getKey())).append("\":{");
            
            boolean firstField = true;
            for (Map.Entry<String, String[][]> fieldEntry : sigEntry.getValue().entrySet()) {
                if (!firstField) sb.append(",");
                firstField = false;
                sb.append("\"").append(escapeJson(fieldEntry.getKey())).append("\":");
                sb.append(tuplesToJson(fieldEntry.getValue()));
            }
            sb.append("}");
        }
        sb.append("},");
        
        // Skolems
        sb.append("\"skolems\":{");
        boolean firstSkolem = true;
        for (Map.Entry<String, TuplesDTO> skolemEntry : instance.skolems.entrySet()) {
            if (!firstSkolem) sb.append(",");
            firstSkolem = false;
            sb.append("\"").append(escapeJson(skolemEntry.getKey())).append("\":");
            sb.append(tuplesDtoToJson(skolemEntry.getValue()));
        }
        sb.append("}");
        
        sb.append("}");
        return sb.toString();
    }
    
    private String sigDefToJson(SigDefDTO sig) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (sig.name != null) {
            sb.append("\"name\":\"").append(escapeJson(sig.name)).append("\",");
        }
        if (sig.cardinality != null) {
            sb.append("\"cardinality\":\"").append(sig.cardinality.toString()).append("\",");
        }
        sb.append("\"isEnum\":").append(sig.isEnum).append(",");
        sb.append("\"meta\":").append(sig.meta).append(",");
        sb.append("\"builtin\":").append(sig.builtin);
        if (sig.type != null) {
            sb.append(",\"type\":\"").append(escapeJson(sig.type)).append("\"");
        }
        if (!sig.fields.isEmpty()) {
            sb.append(",\"fields\":{");
            boolean firstField = true;
            for (Map.Entry<String, FieldDTO> entry : sig.fields.entrySet()) {
                if (!firstField) sb.append(",");
                firstField = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
                sb.append(fieldToJson(entry.getValue()));
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String fieldToJson(FieldDTO field) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (field.name != null) {
            sb.append("\"name\":\"").append(escapeJson(field.name)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
    
    private String tuplesToJson(String[][] tuples) {
        if (tuples == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < tuples.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("[");
            String[] tuple = tuples[i];
            for (int j = 0; j < tuple.length; j++) {
                if (j > 0) sb.append(",");
                sb.append("\"").append(escapeJson(tuple[j])).append("\"");
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String tuplesDtoToJson(TuplesDTO tuplesDto) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"arity\":").append(tuplesDto.arity).append(",");
        sb.append("\"data\":").append(tuplesToJson(tuplesDto.data));
        sb.append("}");
        return sb.toString();
    }
    
    private String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(items.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
