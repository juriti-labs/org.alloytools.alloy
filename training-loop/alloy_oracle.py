"""
Alloy Oracle - Python Wrapper for Alloy Analyzer

This module wraps the Alloy CLI to provide an oracle service for LLM training.
It provides a clean interface for:
- Parsing and type-checking Alloy modules
- Executing run/check commands  
- Returning structured results suitable for training data generation
"""

import json
import os
import subprocess
import tempfile
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import List, Optional, Dict, Any


class OracleStatus(Enum):
    """Status codes returned by the Alloy oracle."""
    PARSE_ERROR = "parse-error"
    TYPE_ERROR = "type-error"
    INSTANCE_FOUND = "instance-found"
    NO_INSTANCE = "no-instance"
    TIMEOUT = "timeout"
    ERROR = "error"
    SUCCESS = "success"  # For parse-only operations

    @classmethod
    def from_id(cls, id_str: str) -> "OracleStatus":
        """Convert status ID string to enum."""
        for status in cls:
            if status.value == id_str:
                return status
        return cls.ERROR


@dataclass
class OracleError:
    """Represents a structured error from the Alloy oracle."""
    kind: str  # "parse", "type", "semantic", "execution", "internal"
    message: str
    file: Optional[str] = None
    line_start: int = 0
    column_start: int = 0
    line_end: int = 0
    column_end: int = 0

    def __str__(self) -> str:
        if self.line_start > 0:
            return f"{self.kind}:{self.line_start}:{self.column_start}: {self.message}"
        return f"{self.kind}: {self.message}"


@dataclass
class InstanceStats:
    """Statistics about an Alloy instance/solution."""
    atom_count: int = 0
    signature_count: int = 0
    relation_count: int = 0
    tuple_count: int = 0
    skolem_count: int = 0
    trace_length: int = 1
    loop_state: int = -1


@dataclass
class OracleResponse:
    """Structured response from the Alloy oracle."""
    status: Optional[OracleStatus] = None
    duration_ms: int = 0
    is_check: bool = False
    command_label: Optional[str] = None
    summary: Optional[str] = None
    errors: List[OracleError] = field(default_factory=list)
    instance_stats: Optional[InstanceStats] = None
    solution: Optional[Dict[str, Any]] = None
    raw_stdout: str = ""
    raw_stderr: str = ""

    def has_compile_errors(self) -> bool:
        """Check if there are parse or type errors."""
        return self.status in (OracleStatus.PARSE_ERROR, OracleStatus.TYPE_ERROR)

    def is_success(self) -> bool:
        """Check if execution was successful (compiled and ran)."""
        return self.status in (OracleStatus.INSTANCE_FOUND, OracleStatus.NO_INSTANCE, OracleStatus.SUCCESS)

    def add_error(self, error: OracleError) -> None:
        """Add an error to the response."""
        self.errors.append(error)


class AlloyOracle:
    """
    Alloy Oracle service that wraps the Alloy CLI for LLM training.
    
    This provides a clean interface for:
    - Parsing and type-checking Alloy modules
    - Executing run/check commands
    - Returning structured results suitable for training data generation
    """
    
    def __init__(
        self,
        alloy_jar_path: Optional[str] = None,
        java_path: str = "java",
        timeout_ms: int = 30000,
        solver: str = "sat4j"
    ):
        """
        Initialize the Alloy oracle.
        
        Args:
            alloy_jar_path: Path to the Alloy dist JAR. If None, tries to find it.
            java_path: Path to Java executable.
            timeout_ms: Timeout for solver operations in milliseconds.
            solver: SAT solver to use (sat4j, minisat, etc.)
        """
        self.java_path = java_path
        self.timeout_ms = timeout_ms
        self.solver = solver
        
        if alloy_jar_path:
            self.alloy_jar_path = Path(alloy_jar_path)
        else:
            self.alloy_jar_path = self._find_alloy_jar()
        
        if not self.alloy_jar_path.exists():
            raise FileNotFoundError(
                f"Alloy JAR not found at {self.alloy_jar_path}. "
                "Please build the project with './gradlew build' or specify the path."
            )

    def _find_alloy_jar(self) -> Path:
        """Find the Alloy distribution JAR file."""
        # Look relative to this file's location
        script_dir = Path(__file__).parent
        possible_paths = [
            # Relative to training-loop folder
            script_dir.parent / "org.alloytools.alloy.dist" / "target" / "org.alloytools.alloy.dist.jar",
            # From repository root
            Path("org.alloytools.alloy.dist/target/org.alloytools.alloy.dist.jar"),
            # Absolute path common in CI
            Path("/home/runner/work/org.alloytools.alloy/org.alloytools.alloy/org.alloytools.alloy.dist/target/org.alloytools.alloy.dist.jar"),
        ]
        
        for path in possible_paths:
            if path.exists():
                return path
        
        # Default to the most common location
        return possible_paths[0]

    def run(self, module_text: str, command_name: Optional[str] = None) -> OracleResponse:
        """
        Execute an Alloy module and return structured results.
        
        Args:
            module_text: The Alloy module source code.
            command_name: Optional command name or index to run (default: first command).
            
        Returns:
            OracleResponse with execution results.
        """
        return self._execute_oracle(module_text, command_name, parse_only=False)

    def parse_only(self, module_text: str) -> OracleResponse:
        """
        Parse and type-check an Alloy module without executing commands.
        
        Args:
            module_text: The Alloy module source code.
            
        Returns:
            OracleResponse with parse/type-check results.
        """
        return self._execute_oracle(module_text, None, parse_only=True)

    def list_commands(self, module_text: str) -> List[str]:
        """
        List all commands in a module.
        
        Args:
            module_text: The Alloy module source code.
            
        Returns:
            List of command descriptions, or empty list if parse fails.
        """
        try:
            with tempfile.NamedTemporaryFile(mode='w', suffix='.als', delete=False) as f:
                f.write(module_text)
                temp_path = f.name
            
            try:
                cmd = [
                    self.java_path, "-jar", str(self.alloy_jar_path),
                    "commands", temp_path
                ]
                
                result = subprocess.run(
                    cmd,
                    capture_output=True,
                    text=True,
                    timeout=10
                )
                
                if result.returncode == 0:
                    return [line.strip() for line in result.stdout.strip().split('\n') if line.strip()]
                return []
            finally:
                os.unlink(temp_path)
        except Exception:
            return []

    def _execute_oracle(
        self,
        module_text: str,
        command_name: Optional[str],
        parse_only: bool
    ) -> OracleResponse:
        """Internal method to execute the Alloy oracle."""
        import time
        start_time = time.time()
        response = OracleResponse()
        
        try:
            # Write module to temporary file
            with tempfile.NamedTemporaryFile(mode='w', suffix='.als', delete=False) as f:
                f.write(module_text)
                temp_path = f.name
            
            try:
                # Build command
                cmd = [
                    self.java_path, "-jar", str(self.alloy_jar_path),
                    "oracle"
                ]
                
                if parse_only:
                    cmd.append("--parseOnly")
                
                if command_name:
                    cmd.extend(["--command", command_name])
                
                cmd.extend(["--timeout", str(self.timeout_ms)])
                cmd.append(temp_path)
                
                # Execute with timeout
                timeout_seconds = (self.timeout_ms / 1000) + 10  # Extra buffer
                result = subprocess.run(
                    cmd,
                    capture_output=True,
                    text=True,
                    timeout=timeout_seconds
                )
                
                response.raw_stdout = result.stdout
                response.raw_stderr = result.stderr
                
                # Parse JSON response
                if result.stdout.strip():
                    try:
                        json_response = json.loads(result.stdout)
                        response = self._parse_json_response(json_response)
                        response.raw_stdout = result.stdout
                        response.raw_stderr = result.stderr
                    except json.JSONDecodeError:
                        # If not JSON, try to extract error information
                        response = self._parse_text_response(result)
                else:
                    response = self._parse_text_response(result)
                    
            finally:
                os.unlink(temp_path)
                
        except subprocess.TimeoutExpired:
            response.status = OracleStatus.TIMEOUT
            response.summary = f"Solver timed out after {self.timeout_ms}ms"
            
        except FileNotFoundError as e:
            response.status = OracleStatus.ERROR
            response.add_error(OracleError("internal", str(e)))
            response.summary = f"Failed to execute Alloy: {e}"
            
        except Exception as e:
            response.status = OracleStatus.ERROR
            response.add_error(OracleError("internal", str(e)))
            response.summary = f"Internal error: {e}"
        
        response.duration_ms = int((time.time() - start_time) * 1000)
        return response

    def _parse_json_response(self, json_data: Dict[str, Any]) -> OracleResponse:
        """Parse JSON response from the oracle command."""
        response = OracleResponse()
        
        status_str = json_data.get("status", "error")
        response.status = OracleStatus.from_id(status_str)
        response.duration_ms = json_data.get("durationMs", 0)
        response.is_check = json_data.get("isCheck", False)
        response.command_label = json_data.get("commandLabel")
        response.summary = json_data.get("summary")
        
        # Parse errors
        for err_data in json_data.get("errors", []):
            error = OracleError(
                kind=err_data.get("kind", "unknown"),
                message=err_data.get("message", ""),
                file=err_data.get("file"),
                line_start=err_data.get("lineStart", 0),
                column_start=err_data.get("columnStart", 0),
                line_end=err_data.get("lineEnd", 0),
                column_end=err_data.get("columnEnd", 0)
            )
            response.add_error(error)
        
        # Parse instance stats
        stats_data = json_data.get("instanceStats")
        if stats_data:
            response.instance_stats = InstanceStats(
                atom_count=stats_data.get("atomCount", 0),
                signature_count=stats_data.get("signatureCount", 0),
                relation_count=stats_data.get("relationCount", 0),
                tuple_count=stats_data.get("tupleCount", 0),
                skolem_count=stats_data.get("skolemCount", 0),
                trace_length=stats_data.get("traceLength", 1),
                loop_state=stats_data.get("loopState", -1)
            )
        
        # Store solution if present
        if json_data.get("hasSolution"):
            response.solution = json_data.get("solution")
        
        return response

    def _parse_text_response(self, result: subprocess.CompletedProcess) -> OracleResponse:
        """Parse text response when JSON is not available."""
        response = OracleResponse()
        
        stderr = result.stderr.lower() if result.stderr else ""
        stdout = result.stdout.lower() if result.stdout else ""
        combined = stderr + stdout
        
        if "syntax error" in combined or "parse error" in combined:
            response.status = OracleStatus.PARSE_ERROR
            response.add_error(OracleError("parse", result.stderr or result.stdout))
            response.summary = "Parse error"
        elif "type error" in combined:
            response.status = OracleStatus.TYPE_ERROR
            response.add_error(OracleError("type", result.stderr or result.stdout))
            response.summary = "Type error"
        elif result.returncode != 0:
            response.status = OracleStatus.ERROR
            response.add_error(OracleError("unknown", result.stderr or result.stdout or "Unknown error"))
            response.summary = f"Execution failed with code {result.returncode}"
        else:
            response.status = OracleStatus.SUCCESS
            response.summary = "Execution completed"
        
        return response


# Convenience function for quick oracle usage
def run_alloy(module_text: str, command: Optional[str] = None) -> OracleResponse:
    """
    Quick function to run Alloy code through the oracle.
    
    Args:
        module_text: The Alloy module source code.
        command: Optional command to run.
        
    Returns:
        OracleResponse with execution results.
    """
    oracle = AlloyOracle()
    return oracle.run(module_text, command)
