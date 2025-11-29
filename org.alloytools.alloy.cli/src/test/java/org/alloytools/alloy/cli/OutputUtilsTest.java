package org.alloytools.alloy.cli;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for OutputUtils class.
 * These tests are specifically designed for mutation testing.
 */
public class OutputUtilsTest {

    // ===================== sanitizeId tests =====================

    @Test
    public void testSanitizeId_NullInput() {
        assertEquals("unknown", OutputUtils.sanitizeId(null));
    }

    @Test
    public void testSanitizeId_EmptyString() {
        assertEquals("unknown", OutputUtils.sanitizeId(""));
    }

    @Test
    public void testSanitizeId_WhitespaceOnly() {
        assertEquals("unknown", OutputUtils.sanitizeId("   "));
    }

    @Test
    public void testSanitizeId_NormalString() {
        assertEquals("hello", OutputUtils.sanitizeId("hello"));
    }

    @Test
    public void testSanitizeId_DollarSign() {
        assertEquals("Foo_Bar", OutputUtils.sanitizeId("Foo$Bar"));
    }

    @Test
    public void testSanitizeId_ForwardSlash() {
        assertEquals("path_to_file", OutputUtils.sanitizeId("path/to/file"));
    }

    @Test
    public void testSanitizeId_DoubleQuotes() {
        assertEquals("say \\\"hello\\\"", OutputUtils.sanitizeId("say \"hello\""));
    }

    @Test
    public void testSanitizeId_MultipleSpecialChars() {
        assertEquals("a_b_c", OutputUtils.sanitizeId("a$b/c"));
    }

    @Test
    public void testSanitizeId_LeadingTrailingSpaces() {
        assertEquals("test", OutputUtils.sanitizeId("  test  "));
    }

    // ===================== sanitizeRdfId tests =====================

    @Test
    public void testSanitizeRdfId_NullInput() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId(null));
    }

    @Test
    public void testSanitizeRdfId_EmptyString() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId(""));
    }

    @Test
    public void testSanitizeRdfId_WhitespaceOnly() {
        // Spaces are converted to underscores before trim, so "   " -> "___"
        assertEquals("___", OutputUtils.sanitizeRdfId("   "));
    }

    @Test
    public void testSanitizeRdfId_NormalString() {
        assertEquals("hello", OutputUtils.sanitizeRdfId("hello"));
    }

    @Test
    public void testSanitizeRdfId_DollarSign() {
        assertEquals("Foo_Bar", OutputUtils.sanitizeRdfId("Foo$Bar"));
    }

    @Test
    public void testSanitizeRdfId_ForwardSlash() {
        assertEquals("path_to_file", OutputUtils.sanitizeRdfId("path/to/file"));
    }

    @Test
    public void testSanitizeRdfId_Space() {
        assertEquals("hello_world", OutputUtils.sanitizeRdfId("hello world"));
    }

    @Test
    public void testSanitizeRdfId_DoubleQuotes() {
        assertEquals("hello", OutputUtils.sanitizeRdfId("\"hello\""));
    }

    @Test
    public void testSanitizeRdfId_SingleQuotes() {
        assertEquals("hello", OutputUtils.sanitizeRdfId("'hello'"));
    }

    @Test
    public void testSanitizeRdfId_AngleBrackets() {
        assertEquals("hello", OutputUtils.sanitizeRdfId("<hello>"));
    }

    @Test
    public void testSanitizeRdfId_Colon() {
        assertEquals("a_b", OutputUtils.sanitizeRdfId("a:b"));
    }

    @Test
    public void testSanitizeRdfId_AllSpecialChars() {
        assertEquals("a_b_c_d", OutputUtils.sanitizeRdfId("a$b/c d"));
    }

    @Test
    public void testSanitizeRdfId_OnlySpecialChars() {
        // The colon becomes underscore, so "'<>:" -> "_"
        assertEquals("_", OutputUtils.sanitizeRdfId("\"'<>:"));
    }

    // ===================== escapeRdf tests =====================

    @Test
    public void testEscapeRdf_NullInput() {
        assertEquals("", OutputUtils.escapeRdf(null));
    }

    @Test
    public void testEscapeRdf_EmptyString() {
        assertEquals("", OutputUtils.escapeRdf(""));
    }

    @Test
    public void testEscapeRdf_NormalString() {
        assertEquals("hello", OutputUtils.escapeRdf("hello"));
    }

    @Test
    public void testEscapeRdf_Backslash() {
        assertEquals("a\\\\b", OutputUtils.escapeRdf("a\\b"));
    }

    @Test
    public void testEscapeRdf_DoubleQuotes() {
        assertEquals("say \\\"hello\\\"", OutputUtils.escapeRdf("say \"hello\""));
    }

    @Test
    public void testEscapeRdf_Newline() {
        assertEquals("line1\\nline2", OutputUtils.escapeRdf("line1\nline2"));
    }

    @Test
    public void testEscapeRdf_CarriageReturn() {
        assertEquals("line1\\rline2", OutputUtils.escapeRdf("line1\rline2"));
    }

    @Test
    public void testEscapeRdf_MultipleEscapes() {
        assertEquals("a\\\\b\\\"c\\nd", OutputUtils.escapeRdf("a\\b\"c\nd"));
    }

    // ===================== escapeYaml tests =====================

    @Test
    public void testEscapeYaml_NullInput() {
        assertEquals("", OutputUtils.escapeYaml(null));
    }

    @Test
    public void testEscapeYaml_EmptyString() {
        assertEquals("", OutputUtils.escapeYaml(""));
    }

    @Test
    public void testEscapeYaml_NormalString() {
        assertEquals("hello", OutputUtils.escapeYaml("hello"));
    }

    @Test
    public void testEscapeYaml_Backslash() {
        assertEquals("a\\\\b", OutputUtils.escapeYaml("a\\b"));
    }

    @Test
    public void testEscapeYaml_DoubleQuotes() {
        assertEquals("say \\\"hello\\\"", OutputUtils.escapeYaml("say \"hello\""));
    }

    @Test
    public void testEscapeYaml_Newline() {
        assertEquals("line1\\nline2", OutputUtils.escapeYaml("line1\nline2"));
    }

    @Test
    public void testEscapeYaml_CarriageReturn() {
        assertEquals("line1\\rline2", OutputUtils.escapeYaml("line1\rline2"));
    }

    @Test
    public void testEscapeYaml_Tab() {
        assertEquals("a\\tb", OutputUtils.escapeYaml("a\tb"));
    }

    @Test
    public void testEscapeYaml_MultipleEscapes() {
        assertEquals("a\\\\b\\\"c\\nd\\te", OutputUtils.escapeYaml("a\\b\"c\nd\te"));
    }
}
