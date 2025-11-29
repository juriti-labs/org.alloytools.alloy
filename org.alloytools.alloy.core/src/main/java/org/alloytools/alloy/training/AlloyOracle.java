package org.alloytools.alloy.training;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorSyntax;
import edu.mit.csail.sdg.alloy4.ErrorType;
import edu.mit.csail.sdg.alloy4.Pos;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.ExprVar;
import edu.mit.csail.sdg.ast.Sig;
import edu.mit.csail.sdg.parser.CompModule;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

/**
 * Alloy Oracle service that wraps the Alloy analyzer for LLM training.
 * This provides a clean interface for:
 * - Parsing and type-checking Alloy modules
 * - Executing run/check commands
 * - Returning structured results suitable for training data generation
 */
public class AlloyOracle {
    
    private final A4Options options;
    private final long timeoutMs;
    private final ExecutorService executor;
    
    /**
     * Create an oracle with default options.
     */
    public AlloyOracle() {
        this(new A4Options(), 30000); // 30 second default timeout
    }
    
    /**
     * Create an oracle with custom options and timeout.
     * @param options the A4Options to use for solving
     * @param timeoutMs the timeout in milliseconds
     */
    public AlloyOracle(A4Options options, long timeoutMs) {
        this.options = options;
        this.timeoutMs = timeoutMs;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Execute an Alloy module from a string.
     * This parses, type-checks, and runs the default command.
     * 
     * @param moduleText the Alloy module source code
     * @return an OracleResponse with the result
     */
    public OracleResponse run(String moduleText) {
        return run(moduleText, null);
    }
    
    /**
     * Execute an Alloy module with a specific command.
     * 
     * @param moduleText the Alloy module source code
     * @param commandName the name or index of the command to run (null for first/default)
     * @return an OracleResponse with the result
     */
    public OracleResponse run(String moduleText, String commandName) {
        long startTime = System.currentTimeMillis();
        OracleResponse response = new OracleResponse();
        
        try {
            // Parse the module
            CompModule module = parseModule(moduleText);
            
            // Find the command to execute
            Command command = findCommand(module, commandName);
            if (command == null) {
                response.status = OracleStatus.ERROR;
                response.addError(new OracleError("semantic", "No command found to execute"));
                return response;
            }
            
            response.commandLabel = command.label;
            response.isCheck = command.check;
            
            // Execute with timeout
            OracleResponse result = executeWithTimeout(module, command, response, startTime);
            return result;
            
        } catch (ErrorSyntax e) {
            response.status = OracleStatus.PARSE_ERROR;
            response.addError(toOracleError(e, "parse"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Parse error: " + e.msg;
            return response;
            
        } catch (ErrorType e) {
            response.status = OracleStatus.TYPE_ERROR;
            response.addError(toOracleError(e, "type"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Type error: " + e.msg;
            return response;
            
        } catch (Err e) {
            response.status = OracleStatus.ERROR;
            response.addError(toOracleError(e, "semantic"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Error: " + e.msg;
            return response;
            
        } catch (Exception e) {
            response.status = OracleStatus.ERROR;
            response.addError(new OracleError("internal", e.getMessage()));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Internal error: " + e.getMessage();
            return response;
        }
    }
    
    /**
     * Parse an Alloy module without executing.
     * Useful for syntax/type checking before execution.
     * 
     * @param moduleText the Alloy module source
     * @return an OracleResponse (status will be ERROR if issues, or null for success to be determined by caller)
     */
    public OracleResponse parseOnly(String moduleText) {
        long startTime = System.currentTimeMillis();
        OracleResponse response = new OracleResponse();
        
        try {
            parseModule(moduleText);
            // If we get here, parsing and type-checking succeeded
            response.status = null; // No error, but also not executed
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Module parsed and type-checked successfully";
            return response;
            
        } catch (ErrorSyntax e) {
            response.status = OracleStatus.PARSE_ERROR;
            response.addError(toOracleError(e, "parse"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Parse error: " + e.msg;
            return response;
            
        } catch (ErrorType e) {
            response.status = OracleStatus.TYPE_ERROR;
            response.addError(toOracleError(e, "type"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Type error: " + e.msg;
            return response;
            
        } catch (Err e) {
            response.status = OracleStatus.ERROR;
            response.addError(toOracleError(e, "semantic"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Error: " + e.msg;
            return response;
            
        } catch (Exception e) {
            response.status = OracleStatus.ERROR;
            response.addError(new OracleError("internal", e.getMessage()));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Internal error: " + e.getMessage();
            return response;
        }
    }
    
    /**
     * List all commands in a module.
     * 
     * @param moduleText the Alloy module source
     * @return list of command descriptions, or empty if parse fails
     */
    public List<String> listCommands(String moduleText) {
        List<String> commands = new ArrayList<>();
        try {
            CompModule module = parseModule(moduleText);
            for (Command cmd : module.getAllCommands()) {
                commands.add((cmd.check ? "check " : "run ") + cmd.label);
            }
        } catch (Exception e) {
            // Return empty list on parse failure
        }
        return commands;
    }
    
    /**
     * Shutdown the oracle executor.
     * Call this when done using the oracle.
     */
    public void shutdown() {
        executor.shutdown();
    }
    
    // ---- Internal methods ----
    
    private CompModule parseModule(String moduleText) throws Err {
        return CompUtil.parseEverything_fromString(A4Reporter.NOP, moduleText);
    }
    
    private Command findCommand(CompModule module, String commandName) {
        List<Command> commands = module.getAllCommands();
        if (commands.isEmpty()) {
            return null;
        }
        
        if (commandName == null || commandName.isEmpty()) {
            return commands.get(0);
        }
        
        // Try to parse as index
        try {
            int index = Integer.parseInt(commandName);
            if (index >= 0 && index < commands.size()) {
                return commands.get(index);
            }
        } catch (NumberFormatException e) {
            // Not an index, try name match
        }
        
        // Try name match
        for (Command cmd : commands) {
            if (commandName.equals(cmd.label)) {
                return cmd;
            }
        }
        
        return null;
    }
    
    private OracleResponse executeWithTimeout(CompModule module, Command command, 
                                              OracleResponse response, long startTime) {
        Callable<A4Solution> task = () -> {
            return TranslateAlloyToKodkod.execute_command(
                A4Reporter.NOP, 
                module.getAllReachableSigs(), 
                command, 
                options
            );
        };
        
        Future<A4Solution> future = executor.submit(task);
        
        try {
            A4Solution solution = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            
            response.durationMs = System.currentTimeMillis() - startTime;
            
            if (solution.satisfiable()) {
                response.status = OracleStatus.INSTANCE_FOUND;
                solution.setModule(module);
                response.solution = solution.toDTO();
                response.instanceStats = computeStats(solution);
                
                if (command.check) {
                    response.summary = "Counterexample found - assertion does not hold";
                } else {
                    response.summary = "Instance found - constraints are satisfiable";
                }
            } else {
                response.status = OracleStatus.NO_INSTANCE;
                if (command.check) {
                    response.summary = "No counterexample found - assertion holds within scope";
                } else {
                    response.summary = "No instance found - constraints are unsatisfiable within scope";
                }
            }
            
            return response;
            
        } catch (TimeoutException e) {
            future.cancel(true);
            response.status = OracleStatus.TIMEOUT;
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Solver timed out after " + timeoutMs + "ms";
            return response;
            
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Err) {
                response.status = OracleStatus.ERROR;
                response.addError(toOracleError((Err) cause, "execution"));
                response.summary = "Execution error: " + cause.getMessage();
            } else {
                response.status = OracleStatus.ERROR;
                response.addError(new OracleError("internal", cause.getMessage()));
                response.summary = "Internal error: " + cause.getMessage();
            }
            response.durationMs = System.currentTimeMillis() - startTime;
            return response;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.status = OracleStatus.ERROR;
            response.addError(new OracleError("internal", "Execution interrupted"));
            response.durationMs = System.currentTimeMillis() - startTime;
            response.summary = "Execution was interrupted";
            return response;
        }
    }
    
    private OracleError toOracleError(Err err, String kind) {
        Pos pos = err.pos;
        if (pos == null || pos == Pos.UNKNOWN) {
            return new OracleError(kind, err.msg);
        }
        return new OracleError(kind, err.msg, pos.filename, 
                              pos.y, pos.x, pos.y2, pos.x2);
    }
    
    private InstanceStats computeStats(A4Solution solution) {
        InstanceStats stats = new InstanceStats();
        
        try {
            // Count atoms by iterating through signatures
            for (Sig sig : solution.getAllReachableSigs()) {
                if (!sig.builtin) {
                    int sigAtomCount = solution.eval(sig).size();
                    stats.atomCount += sigAtomCount;
                    stats.signatureCount++;
                    stats.relationCount += sig.getFields().size();
                }
            }
            
            // Count skolems
            List<ExprVar> skolems = solution.getAllSkolems();
            stats.skolemCount = skolems.size();
            
            // Get trace info for temporal models
            if (solution.isTemporal()) {
                stats.traceLength = solution.getTraceLength();
                stats.loopState = solution.getLoopState();
            }
        } catch (Exception e) {
            // Best effort stats collection
        }
        
        return stats;
    }
}
