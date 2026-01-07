package org.alloytools.alloy.wasm;

import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.lib.json.JSONCodec;

/**
 * WebAssembly interface for Alloy.
 * This class provides a JSON-based API for executing Alloy models from JavaScript.
 */
public class AlloyWasm {
    
    private static final JSONCodec jsonCodec = new JSONCodec();
    
    /**
     * Parse an Alloy model and return its structure as JSON.
     * 
     * @param modelText The Alloy model source code
     * @return JSON string containing model information (commands, signatures, etc.)
     */
    public static String parseModel(String modelText) {
        try {
            Map<String, String> cache = new HashMap<>();
            CompModule module = CompUtil.parseOneModule(modelText);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("moduleName", module.path); // Use path field instead of getModelName()
            
            // Extract commands
            List<Command> commands = module.getAllCommands();
            result.put("commandCount", commands.size());
            
            String[] commandNames = new String[commands.size()];
            for (int i = 0; i < commands.size(); i++) {
                commandNames[i] = commands.get(i).label;
            }
            result.put("commands", commandNames);
            
            return jsonCodec.enc().put(result).toString();
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    /**
     * Execute an Alloy command and return the solution as JSON.
     * 
     * @param modelText The Alloy model source code
     * @param commandName The name of the command to execute (empty for default)
     * @return JSON string containing the solution or error
     */
    public static String executeCommand(String modelText, String commandName) {
        try {
            // Parse the model
            Map<String, String> cache = new HashMap<>();
            CompModule module = CompUtil.parseOneModule(modelText);
            
            // Find the command
            List<Command> commands = module.getAllCommands();
            Command cmd = null;
            
            if (commandName == null || commandName.isEmpty()) {
                // Use first command
                if (commands.isEmpty()) {
                    return createErrorResponse("No commands found in model");
                }
                cmd = commands.get(0);
            } else {
                // Find named command
                for (Command c : commands) {
                    if (c.label.equals(commandName)) {
                        cmd = c;
                        break;
                    }
                }
                if (cmd == null) {
                    return createErrorResponse("Command not found: " + commandName);
                }
            }
            
            // Execute the command
            A4Options options = new A4Options();
            // Use SAT4J solver (pure Java, no native dependencies)
            options.solver = kodkod.engine.satlab.SATFactory.find("sat4j").orElse(kodkod.engine.satlab.SATFactory.DEFAULT);
            
            A4Solution solution = TranslateAlloyToKodkod.execute_command(
                null, module.getAllReachableSigs(), cmd, options
            );
            
            // Convert solution to JSON
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("satisfiable", solution.satisfiable());
            result.put("command", cmd.label);
            
            if (solution.satisfiable()) {
                // Convert solution to DTO for JSON serialization
                org.alloytools.alloy.dto.SolutionDTO solutionDTO = solution.toDTO();
                result.put("solution", solutionDTO);
            }
            
            return jsonCodec.enc().put(result).toString();
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    /**
     * Get the version information.
     * 
     * @return JSON string with version info
     */
    public static String getVersion() {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("version", "6.0.0-WASM");
            result.put("backend", "WebAssembly");
            return jsonCodec.enc().put(result).toString();
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
    
    /**
     * Create an error response in JSON format.
     */
    private static String createErrorResponse(String message) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", message);
            return jsonCodec.enc().put(result).toString();
        } catch (Exception e) {
            return "{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}";
        }
    }
    
    /**
     * Create an error response from an exception.
     */
    private static String createErrorResponse(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }
        return createErrorResponse(message);
    }
    
    /**
     * Simple JSON string escaping.
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
