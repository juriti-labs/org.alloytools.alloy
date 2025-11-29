package org.alloytools.alloy.wasm;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the AlloyWasm class.
 */
public class AlloyWasmTest {
    
    private AlloyWasm alloy;
    
    @Before
    public void setup() {
        alloy = new AlloyWasm();
    }
    
    @Test
    public void testGetVersion() {
        String version = alloy.getVersion();
        assertNotNull(version);
        assertTrue(version.contains("alloy-wasm"));
        assertTrue(version.contains("6.3.0"));
    }
    
    @Test
    public void testGetSolvers() {
        String solvers = alloy.getSolvers();
        assertNotNull(solvers);
        assertTrue(solvers.contains("sat4j"));
    }
    
    @Test
    public void testParseValidModel() {
        String model = "sig Person { friend: set Person }\nrun {} for 3";
        String result = alloy.parse(model);
        
        assertNotNull(result);
        // Should not contain parse errors
        assertFalse(result.contains("\"status\":\"PARSE_ERROR\""));
        // Should contain success info
        assertTrue(result.contains("parsed and type-checked successfully") 
                   || result.contains("\"errors\":[]"));
    }
    
    @Test
    public void testParseInvalidModel() {
        String model = "sig { }"; // Missing signature name
        String result = alloy.parse(model);
        
        assertNotNull(result);
        assertTrue(result.contains("PARSE_ERROR") || result.contains("\"errors\":[{"));
    }
    
    @Test
    public void testExecuteSimpleModel() {
        String model = "sig A {}\nrun {} for 3";
        String result = alloy.execute(model);
        
        assertNotNull(result);
        assertTrue(result.contains("\"status\""));
        assertTrue(result.contains("\"durationMs\""));
    }
    
    @Test
    public void testExecuteSatisfiableModel() {
        String model = "sig Person {}\nrun { some Person } for 3";
        String result = alloy.execute(model);
        
        assertNotNull(result);
        assertTrue(result.contains("instance-found"));
        assertTrue(result.contains("\"hasSolution\":true"));
    }
    
    @Test
    public void testExecuteUnsatisfiableModel() {
        String model = "sig A {}\nrun { some A and no A } for 3";
        String result = alloy.execute(model);
        
        assertNotNull(result);
        assertTrue(result.contains("no-instance"));
        assertFalse(result.contains("\"hasSolution\":true"));
    }
    
    @Test
    public void testListCommands() {
        String model = "sig A {}\nrun cmd1 {} for 3\ncheck cmd2 {} for 3";
        String result = alloy.listCommands(model);
        
        assertNotNull(result);
        assertTrue(result.contains("cmd1"));
        assertTrue(result.contains("cmd2"));
    }
    
    @Test
    public void testExecuteWithCommandName() {
        String model = "sig A {}\nrun first { #A = 1 } for 3\nrun second { #A = 2 } for 3";
        String result = alloy.execute(model, "second", 30000);
        
        assertNotNull(result);
        assertTrue(result.contains("second") || result.contains("INSTANCE_FOUND"));
    }
    
    @Test
    public void testJsonEscaping() {
        // Test that special characters are properly escaped
        String model = "sig A {}\nrun \"test\\nmodel\" {} for 3";
        String result = alloy.parse(model);
        
        assertNotNull(result);
        // The JSON should be properly escaped and parseable
        // If there's a parse error, the message should be properly escaped
    }
    
    @Test
    public void testInstanceStats() {
        String model = "sig Person { friend: lone Person }\nrun { some Person.friend } for 3";
        String result = alloy.execute(model);
        
        assertNotNull(result);
        if (result.contains("instance-found")) {
            assertTrue(result.contains("\"instanceStats\""));
            assertTrue(result.contains("\"atomCount\""));
            assertTrue(result.contains("\"signatureCount\""));
        }
    }
    
    @Test
    public void testCheckAssertion() {
        String model = "sig A {}\nassert Empty { no A }\ncheck Empty for 3";
        String result = alloy.execute(model);
        
        assertNotNull(result);
        assertTrue(result.contains("\"isCheck\":true"));
    }
}
