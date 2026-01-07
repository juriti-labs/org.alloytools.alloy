/**
 * Alloy WebAssembly TypeScript Definitions
 */

export interface AlloyParseResult {
  success: boolean;
  moduleName?: string;
  commandCount?: number;
  commands?: string[];
  error?: string;
}

export interface AlloyExecutionResult {
  success: boolean;
  satisfiable?: boolean;
  command?: string;
  solution?: AlloyInstance;
  error?: string;
}

export interface AlloyInstance {
  atoms: Record<string, string[]>;
  tuples: Record<string, AlloyTuple[]>;
}

export interface AlloyTuple {
  atoms: string[];
}

export interface AlloyVersionInfo {
  success: boolean;
  version?: string;
  backend?: string;
  error?: string;
}

/**
 * Parse an Alloy model and return its structure.
 * @param modelText The Alloy model source code
 * @returns JSON object containing model information
 */
export function parseModel(modelText: string): AlloyParseResult;

/**
 * Execute an Alloy command and return the solution.
 * @param modelText The Alloy model source code
 * @param commandName The name of the command to execute (empty string for default)
 * @returns JSON object containing the solution or error
 */
export function executeCommand(modelText: string, commandName?: string): AlloyExecutionResult;

/**
 * Get version information about the Alloy WASM module.
 * @returns JSON object with version info
 */
export function getVersion(): AlloyVersionInfo;

/**
 * Initialize the Alloy WASM module.
 * Must be called before using any other functions.
 * @returns Promise that resolves when the module is initialized
 */
export function init(): Promise<void>;
