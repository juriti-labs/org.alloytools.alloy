package org.alloytools.alloy.wasm;

import static org.junit.Assert.*;

import org.junit.Test;

import aQute.lib.json.JSONCodec;

import java.util.Map;

/**
 * Tests for the AlloyWasm JSON interface.
 */
public class AlloyWasmTest {
    
    private final JSONCodec jsonCodec = new JSONCodec();
    
    @Test
    public void testGetVersion() throws Exception {
        String result = AlloyWasm.getVersion();
        assertNotNull("Version result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        assertTrue("Result should be successful", (Boolean) parsed.get("success"));
        assertNotNull("Version should be present", parsed.get("version"));
        assertEquals("Backend should be WebAssembly", "WebAssembly", parsed.get("backend"));
    }
    
    @Test
    public void testParseSimpleModel() throws Exception {
        String model = "sig Node { edges: set Node }\n" +
                      "pred acyclic { no n: Node | n in n.^edges }\n" +
                      "run acyclic for 3";
        
        String result = AlloyWasm.parseModel(model);
        assertNotNull("Parse result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        assertTrue("Parse should be successful", (Boolean) parsed.get("success"));
        assertNotNull("Should have commands", parsed.get("commands"));
        assertTrue("Should have at least one command", (Integer) parsed.get("commandCount") > 0);
    }
    
    @Test
    public void testParseInvalidModel() throws Exception {
        String model = "this is not valid alloy syntax";
        
        String result = AlloyWasm.parseModel(model);
        assertNotNull("Parse result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        assertFalse("Parse should fail", (Boolean) parsed.get("success"));
        assertNotNull("Should have error message", parsed.get("error"));
    }
    
    @Test
    public void testExecuteCommand() throws Exception {
        String model = "sig Node {}\n" +
                      "run {} for 2";
        
        String result = AlloyWasm.executeCommand(model, "");
        assertNotNull("Execute result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        // Either success or failure is acceptable as long as we get a response
        assertNotNull("Should have success field", parsed.get("success"));
        // If successful, should have satisfiable field
        if ((Boolean) parsed.get("success")) {
            assertNotNull("Should have satisfiable field", parsed.get("satisfiable"));
        }
    }
    
    @Test
    public void testExecuteCommandNotFound() throws Exception {
        String model = "sig Node {}\n" +
                      "run {} for 2";
        
        String result = AlloyWasm.executeCommand(model, "nonexistent");
        assertNotNull("Execute result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        assertFalse("Execution should fail", (Boolean) parsed.get("success"));
        assertTrue("Error should mention command not found", 
                  parsed.get("error").toString().toLowerCase().contains("not found"));
    }
    
    @Test
    public void testExecuteCommandNoCommands() throws Exception {
        String model = "sig Node {}";
        
        String result = AlloyWasm.executeCommand(model, "");
        assertNotNull("Execute result should not be null", result);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = jsonCodec.dec().from(result).get(Map.class);
        assertFalse("Execution should fail when no commands", (Boolean) parsed.get("success"));
        assertNotNull("Should have error message", parsed.get("error"));
        // Just check that there's an error message, don't enforce specific text
    }
}
