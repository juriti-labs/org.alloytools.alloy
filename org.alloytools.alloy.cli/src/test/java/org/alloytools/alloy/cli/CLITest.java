package org.alloytools.alloy.cli;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for CLI utility methods.
 * These tests are designed for mutation testing to ensure code quality.
 */
public class CLITest {

    // Note: Since sanitizeId and sanitizeRdfId are private methods in CLI class,
    // we test them indirectly through reflection or by testing public behaviors.
    // For now, we test what we can access.

    @Test
    public void testOutputTypeEnumValues() {
        // Verify all output types are present
        CLI.OutputType[] types = CLI.OutputType.values();
        assertEquals(8, types.length);
        
        // Check specific values
        assertEquals(CLI.OutputType.none, CLI.OutputType.valueOf("none"));
        assertEquals(CLI.OutputType.text, CLI.OutputType.valueOf("text"));
        assertEquals(CLI.OutputType.table, CLI.OutputType.valueOf("table"));
        assertEquals(CLI.OutputType.json, CLI.OutputType.valueOf("json"));
        assertEquals(CLI.OutputType.xml, CLI.OutputType.valueOf("xml"));
        assertEquals(CLI.OutputType.yaml, CLI.OutputType.valueOf("yaml"));
        assertEquals(CLI.OutputType.dot, CLI.OutputType.valueOf("dot"));
        assertEquals(CLI.OutputType.rdf, CLI.OutputType.valueOf("rdf"));
    }

    @Test
    public void testOutputTypeEnumOrdinals() {
        // Verify ordinal values are as expected
        assertEquals(0, CLI.OutputType.none.ordinal());
        assertEquals(1, CLI.OutputType.text.ordinal());
        assertEquals(2, CLI.OutputType.table.ordinal());
        assertEquals(3, CLI.OutputType.json.ordinal());
        assertEquals(4, CLI.OutputType.xml.ordinal());
        assertEquals(5, CLI.OutputType.yaml.ordinal());
        assertEquals(6, CLI.OutputType.dot.ordinal());
        assertEquals(7, CLI.OutputType.rdf.ordinal());
    }

    @Test
    public void testCLIToString() {
        // Test toString method
        CLI cli = new CLI();
        assertEquals("CLI", cli.toString());
    }

    @Test
    public void testOutputTypeNameMethod() {
        // Test name() method for each enum value
        assertEquals("none", CLI.OutputType.none.name());
        assertEquals("text", CLI.OutputType.text.name());
        assertEquals("table", CLI.OutputType.table.name());
        assertEquals("json", CLI.OutputType.json.name());
        assertEquals("xml", CLI.OutputType.xml.name());
        assertEquals("yaml", CLI.OutputType.yaml.name());
        assertEquals("dot", CLI.OutputType.dot.name());
        assertEquals("rdf", CLI.OutputType.rdf.name());
    }

    @Test
    public void testOutputTypeCompareTo() {
        // Test compareTo for enum ordering
        assertTrue(CLI.OutputType.none.compareTo(CLI.OutputType.text) < 0);
        assertTrue(CLI.OutputType.yaml.compareTo(CLI.OutputType.none) > 0);
        assertEquals(0, CLI.OutputType.json.compareTo(CLI.OutputType.json));
    }

    @Test
    public void testOutputTypeEquals() {
        // Test equals
        assertEquals(CLI.OutputType.yaml, CLI.OutputType.valueOf("yaml"));
        assertNotEquals(CLI.OutputType.yaml, CLI.OutputType.json);
        assertNotEquals(CLI.OutputType.yaml, null);
        assertNotEquals(CLI.OutputType.yaml, "yaml");
    }

    @Test
    public void testCLICreation() {
        // Test CLI instance creation
        CLI cli = new CLI();
        assertNotNull(cli);
        assertNotNull(cli.toString());
        assertFalse(cli.toString().isEmpty());
    }

    @Test
    public void testOutputTypeHashCode() {
        // Test hashCode consistency
        assertEquals(CLI.OutputType.yaml.hashCode(), CLI.OutputType.yaml.hashCode());
        assertEquals(CLI.OutputType.valueOf("json").hashCode(), CLI.OutputType.json.hashCode());
    }

    @Test
    public void testOutputTypeDeclaringClass() {
        // Test declaring class
        assertEquals(CLI.OutputType.class, CLI.OutputType.yaml.getDeclaringClass());
    }
}
