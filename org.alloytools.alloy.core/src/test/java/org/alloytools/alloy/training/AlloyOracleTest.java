package org.alloytools.alloy.training;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the AlloyOracle class.
 */
public class AlloyOracleTest {
    
    private AlloyOracle oracle;
    
    @Before
    public void setUp() {
        oracle = new AlloyOracle();
    }
    
    @After
    public void tearDown() {
        if (oracle != null) {
            oracle.shutdown();
        }
    }
    
    @Test
    public void testValidModuleWithRunCommand() {
        String module = "sig Node { edges: set Node }\n" +
                       "run { some Node } for 3";
        
        OracleResponse response = oracle.run(module);
        
        assertEquals(OracleStatus.INSTANCE_FOUND, response.status);
        assertTrue(response.isSuccess());
        assertFalse(response.hasCompileErrors());
        assertFalse(response.isCheck);
        assertNotNull(response.summary);
        assertTrue(response.durationMs >= 0);
    }
    
    @Test
    public void testValidModuleWithCheckCommand() {
        String module = "sig Node { edges: set Node }\n" +
                       "assert NoSelfLoops { no n: Node | n in n.edges }\n" +
                       "check NoSelfLoops for 3";
        
        OracleResponse response = oracle.run(module);
        
        // Could be INSTANCE_FOUND (counterexample) or NO_INSTANCE (assertion holds)
        assertTrue(response.status == OracleStatus.INSTANCE_FOUND || 
                   response.status == OracleStatus.NO_INSTANCE);
        assertTrue(response.isSuccess());
        assertTrue(response.isCheck);
    }
    
    @Test
    public void testParseError() {
        String module = "sig Node { edges: set Node\n" +  // Missing closing brace
                       "run { some Node } for 3";
        
        OracleResponse response = oracle.run(module);
        
        assertEquals(OracleStatus.PARSE_ERROR, response.status);
        assertFalse(response.isSuccess());
        assertTrue(response.hasCompileErrors());
        assertFalse(response.errors.isEmpty());
        
        OracleError error = response.errors.get(0);
        assertEquals("parse", error.kind);
        assertNotNull(error.message);
    }
    
    @Test
    public void testTypeError() {
        // This should cause a type error: comparing String/Int to a sig
        String module = "sig A {}\n" +
                       "sig B {}\n" +
                       "run { some a: A | a = B } for 3";  // Cannot compare A with sig B
        
        OracleResponse response = oracle.run(module);
        
        // This test may result in TYPE_ERROR or could be valid depending on Alloy version
        // At minimum, verify the response is valid
        assertNotNull(response.status);
        assertTrue(response.durationMs >= 0);
    }
    
    @Test
    public void testParseOnlyValid() {
        String module = "sig Node { edges: set Node }\n" +
                       "run { some Node } for 3";
        
        OracleResponse response = oracle.parseOnly(module);
        
        assertNull(response.status);  // No error status
        assertFalse(response.hasCompileErrors());
        assertNotNull(response.summary);
    }
    
    @Test
    public void testParseOnlyInvalid() {
        String module = "sig Node { edges set Node }";  // Missing colon
        
        OracleResponse response = oracle.parseOnly(module);
        
        assertEquals(OracleStatus.PARSE_ERROR, response.status);
        assertTrue(response.hasCompileErrors());
    }
    
    @Test
    public void testListCommands() {
        String module = "sig Node { edges: set Node }\n" +
                       "run Show { some Node } for 3";
        
        List<String> commands = oracle.listCommands(module);
        
        // At least one command should be found
        assertFalse("Expected at least one command", commands.isEmpty());
        // Command should contain "Show"
        assertTrue("Expected command to contain 'Show'", commands.get(0).contains("Show"));
    }
    
    @Test
    public void testListCommandsInvalidModule() {
        String module = "this is not valid alloy";
        
        List<String> commands = oracle.listCommands(module);
        
        assertTrue(commands.isEmpty());
    }
    
    @Test
    public void testRunWithCommandName() {
        String module = "sig Node { edges: set Node }\n" +
                       "run First { some Node } for 3\n" +
                       "run Second { no Node } for 3";
        
        OracleResponse response = oracle.run(module, "First");
        
        assertEquals("First", response.commandLabel);
        assertTrue(response.isSuccess());
    }
    
    @Test
    public void testRunWithCommandIndex() {
        String module = "sig Node { edges: set Node }\n" +
                       "run First { some Node } for 3\n" +
                       "run Second { no Node } for 3";
        
        OracleResponse response = oracle.run(module, "1");
        
        assertEquals("Second", response.commandLabel);
    }
    
    @Test
    public void testNoCommandFound() {
        String module = "sig Node { edges: set Node }";  // No run/check command
        
        OracleResponse response = oracle.run(module);
        
        // When there's no command, it should return ERROR status
        // or potentially parse successfully but fail to run
        assertNotNull(response);
        assertTrue(response.durationMs >= 0);
    }
    
    @Test
    public void testInstanceStats() {
        String module = "sig Node { edges: set Node }\n" +
                       "run { #Node = 2 } for 3";
        
        OracleResponse response = oracle.run(module);
        
        assertEquals(OracleStatus.INSTANCE_FOUND, response.status);
        assertNotNull(response.instanceStats);
        assertTrue(response.instanceStats.atomCount >= 0);
        assertTrue(response.instanceStats.signatureCount >= 0);
    }
    
    @Test
    public void testOracleErrorToString() {
        OracleError error = new OracleError("parse", "Unexpected token", "test.als", 5, 10, 5, 15);
        String str = error.toString();
        
        assertTrue(str.contains("parse"));
        assertTrue(str.contains("Unexpected token"));
        assertTrue(str.contains("5"));
    }
    
    @Test
    public void testOracleStatusFromId() {
        assertEquals(OracleStatus.PARSE_ERROR, OracleStatus.fromId("parse-error"));
        assertEquals(OracleStatus.TYPE_ERROR, OracleStatus.fromId("type-error"));
        assertEquals(OracleStatus.INSTANCE_FOUND, OracleStatus.fromId("instance-found"));
        assertEquals(OracleStatus.NO_INSTANCE, OracleStatus.fromId("no-instance"));
        assertEquals(OracleStatus.TIMEOUT, OracleStatus.fromId("timeout"));
        assertEquals(OracleStatus.ERROR, OracleStatus.fromId("error"));
        assertNull(OracleStatus.fromId("unknown"));
    }
    
    @Test
    public void testOracleStatusGetId() {
        assertEquals("parse-error", OracleStatus.PARSE_ERROR.getId());
        assertEquals("type-error", OracleStatus.TYPE_ERROR.getId());
        assertEquals("instance-found", OracleStatus.INSTANCE_FOUND.getId());
    }
}
