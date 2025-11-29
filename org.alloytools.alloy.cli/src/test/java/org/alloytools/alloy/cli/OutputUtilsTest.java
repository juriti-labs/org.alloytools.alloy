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

    // Additional tests to kill mutations
    @Test
    public void testSanitizeId_SingleCharacter() {
        assertEquals("a", OutputUtils.sanitizeId("a"));
    }

    @Test
    public void testSanitizeId_SingleDollar() {
        assertEquals("_", OutputUtils.sanitizeId("$"));
    }

    @Test
    public void testSanitizeId_SingleSlash() {
        assertEquals("_", OutputUtils.sanitizeId("/"));
    }

    @Test
    public void testSanitizeId_SingleQuote() {
        assertEquals("\\\"", OutputUtils.sanitizeId("\""));
    }

    @Test
    public void testSanitizeId_MixedContent() {
        assertEquals("a_b_c\\\"d", OutputUtils.sanitizeId("a$b/c\"d"));
    }

    @Test
    public void testSanitizeId_OnlySpacesAfterReplace() {
        // Dollar signs become underscores, so we need content that becomes empty after trim
        assertEquals("_", OutputUtils.sanitizeId("$"));
    }

    @Test
    public void testSanitizeId_NotNull() {
        assertNotNull(OutputUtils.sanitizeId("test"));
    }

    @Test
    public void testSanitizeId_NotEmpty() {
        assertFalse(OutputUtils.sanitizeId("test").isEmpty());
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

    // Additional tests for mutation killing
    @Test
    public void testSanitizeRdfId_SingleCharacter() {
        assertEquals("a", OutputUtils.sanitizeRdfId("a"));
    }

    @Test
    public void testSanitizeRdfId_SingleDollar() {
        assertEquals("_", OutputUtils.sanitizeRdfId("$"));
    }

    @Test
    public void testSanitizeRdfId_SingleSlash() {
        assertEquals("_", OutputUtils.sanitizeRdfId("/"));
    }

    @Test
    public void testSanitizeRdfId_SingleSpace() {
        assertEquals("_", OutputUtils.sanitizeRdfId(" "));
    }

    @Test
    public void testSanitizeRdfId_SingleDoubleQuote() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId("\""));
    }

    @Test
    public void testSanitizeRdfId_SingleSingleQuote() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId("'"));
    }

    @Test
    public void testSanitizeRdfId_SingleLessThan() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId("<"));
    }

    @Test
    public void testSanitizeRdfId_SingleGreaterThan() {
        assertEquals("unknown", OutputUtils.sanitizeRdfId(">"));
    }

    @Test
    public void testSanitizeRdfId_SingleColon() {
        assertEquals("_", OutputUtils.sanitizeRdfId(":"));
    }

    @Test
    public void testSanitizeRdfId_NotNull() {
        assertNotNull(OutputUtils.sanitizeRdfId("test"));
    }

    @Test
    public void testSanitizeRdfId_NotEmpty() {
        assertFalse(OutputUtils.sanitizeRdfId("test").isEmpty());
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

    // Additional tests for mutation killing
    @Test
    public void testEscapeRdf_SingleBackslash() {
        assertEquals("\\\\", OutputUtils.escapeRdf("\\"));
    }

    @Test
    public void testEscapeRdf_SingleQuote() {
        assertEquals("\\\"", OutputUtils.escapeRdf("\""));
    }

    @Test
    public void testEscapeRdf_SingleNewline() {
        assertEquals("\\n", OutputUtils.escapeRdf("\n"));
    }

    @Test
    public void testEscapeRdf_SingleCarriageReturn() {
        assertEquals("\\r", OutputUtils.escapeRdf("\r"));
    }

    @Test
    public void testEscapeRdf_NotNull() {
        assertNotNull(OutputUtils.escapeRdf("test"));
    }

    @Test
    public void testEscapeRdf_PreservesUnescaped() {
        assertEquals("abc123", OutputUtils.escapeRdf("abc123"));
    }

    @Test
    public void testEscapeRdf_AllEscapesInOrder() {
        // Test order: backslash, quote, newline, carriage return
        assertEquals("\\\\\\\"\\n\\r", OutputUtils.escapeRdf("\\\"\n\r"));
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

    // Additional tests for mutation killing
    @Test
    public void testEscapeYaml_SingleBackslash() {
        assertEquals("\\\\", OutputUtils.escapeYaml("\\"));
    }

    @Test
    public void testEscapeYaml_SingleQuote() {
        assertEquals("\\\"", OutputUtils.escapeYaml("\""));
    }

    @Test
    public void testEscapeYaml_SingleNewline() {
        assertEquals("\\n", OutputUtils.escapeYaml("\n"));
    }

    @Test
    public void testEscapeYaml_SingleCarriageReturn() {
        assertEquals("\\r", OutputUtils.escapeYaml("\r"));
    }

    @Test
    public void testEscapeYaml_SingleTab() {
        assertEquals("\\t", OutputUtils.escapeYaml("\t"));
    }

    @Test
    public void testEscapeYaml_NotNull() {
        assertNotNull(OutputUtils.escapeYaml("test"));
    }

    @Test
    public void testEscapeYaml_PreservesUnescaped() {
        assertEquals("abc123", OutputUtils.escapeYaml("abc123"));
    }

    @Test
    public void testEscapeYaml_AllEscapesInOrder() {
        // Test order: backslash, quote, newline, carriage return, tab
        assertEquals("\\\\\\\"\\n\\r\\t", OutputUtils.escapeYaml("\\\"\n\r\t"));
    }

    // ===================== Boundary and edge case tests =====================

    @Test
    public void testSanitizeId_VeryLongString() {
        String longStr = "a".repeat(1000);
        assertEquals(longStr, OutputUtils.sanitizeId(longStr));
    }

    @Test
    public void testSanitizeRdfId_VeryLongString() {
        String longStr = "a".repeat(1000);
        assertEquals(longStr, OutputUtils.sanitizeRdfId(longStr));
    }

    @Test
    public void testEscapeRdf_VeryLongString() {
        String longStr = "a".repeat(1000);
        assertEquals(longStr, OutputUtils.escapeRdf(longStr));
    }

    @Test
    public void testEscapeYaml_VeryLongString() {
        String longStr = "a".repeat(1000);
        assertEquals(longStr, OutputUtils.escapeYaml(longStr));
    }

    @Test
    public void testSanitizeId_UnicodeCharacters() {
        assertEquals("héllo", OutputUtils.sanitizeId("héllo"));
    }

    @Test
    public void testSanitizeRdfId_UnicodeCharacters() {
        assertEquals("héllo", OutputUtils.sanitizeRdfId("héllo"));
    }

    @Test
    public void testEscapeRdf_UnicodeCharacters() {
        assertEquals("héllo", OutputUtils.escapeRdf("héllo"));
    }

    @Test
    public void testEscapeYaml_UnicodeCharacters() {
        assertEquals("héllo", OutputUtils.escapeYaml("héllo"));
    }

    // ===================== Mutation-killing tests =====================

    @Test
    public void testSanitizeId_NullVsEmpty_Null() {
        // Null should return "unknown"
        String result = OutputUtils.sanitizeId(null);
        assertEquals("unknown", result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSanitizeId_NullVsEmpty_Empty() {
        // Empty should also return "unknown"
        String result = OutputUtils.sanitizeId("");
        assertEquals("unknown", result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSanitizeId_NullVsEmpty_SingleChar() {
        // Single char should NOT return "unknown"
        String result = OutputUtils.sanitizeId("x");
        assertEquals("x", result);
        assertNotEquals("unknown", result);
    }

    @Test
    public void testSanitizeRdfId_NullVsEmpty_Null() {
        // Null should return "unknown"
        String result = OutputUtils.sanitizeRdfId(null);
        assertEquals("unknown", result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSanitizeRdfId_NullVsEmpty_Empty() {
        // Empty should also return "unknown"
        String result = OutputUtils.sanitizeRdfId("");
        assertEquals("unknown", result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testSanitizeRdfId_NullVsEmpty_SingleChar() {
        // Single char should NOT return "unknown"
        String result = OutputUtils.sanitizeRdfId("x");
        assertEquals("x", result);
        assertNotEquals("unknown", result);
    }

    @Test
    public void testSanitizeId_EmptyAfterTrimNotSameAsInputEmpty() {
        // "   " after trim becomes "" which should return "unknown"
        assertEquals("unknown", OutputUtils.sanitizeId("   "));
        // But "x" after trim is "x" which should return "x"
        assertEquals("x", OutputUtils.sanitizeId("x"));
    }

    @Test
    public void testSanitizeRdfId_EmptyResult() {
        // Input that becomes empty after replacements
        String result = OutputUtils.sanitizeRdfId("\"'<>");
        assertEquals("unknown", result);
    }

    // ===================== escapeJson tests =====================

    @Test
    public void testEscapeJson_NullInput() {
        assertEquals("", OutputUtils.escapeJson(null));
    }

    @Test
    public void testEscapeJson_EmptyString() {
        assertEquals("", OutputUtils.escapeJson(""));
    }

    @Test
    public void testEscapeJson_NormalString() {
        assertEquals("hello", OutputUtils.escapeJson("hello"));
    }

    @Test
    public void testEscapeJson_Backslash() {
        assertEquals("a\\\\b", OutputUtils.escapeJson("a\\b"));
    }

    @Test
    public void testEscapeJson_DoubleQuotes() {
        assertEquals("say \\\"hello\\\"", OutputUtils.escapeJson("say \"hello\""));
    }

    @Test
    public void testEscapeJson_Newline() {
        assertEquals("line1\\nline2", OutputUtils.escapeJson("line1\nline2"));
    }

    @Test
    public void testEscapeJson_CarriageReturn() {
        assertEquals("line1\\rline2", OutputUtils.escapeJson("line1\rline2"));
    }

    @Test
    public void testEscapeJson_Tab() {
        assertEquals("a\\tb", OutputUtils.escapeJson("a\tb"));
    }

    @Test
    public void testEscapeJson_Backspace() {
        assertEquals("a\\bb", OutputUtils.escapeJson("a\bb"));
    }

    @Test
    public void testEscapeJson_FormFeed() {
        assertEquals("a\\fb", OutputUtils.escapeJson("a\fb"));
    }

    @Test
    public void testEscapeJson_AllEscapesInOrder() {
        assertEquals("\\\\\\\"\\n\\r\\t\\b\\f", OutputUtils.escapeJson("\\\"\n\r\t\b\f"));
    }

    @Test
    public void testEscapeJson_NotNull() {
        assertNotNull(OutputUtils.escapeJson("test"));
    }

    @Test
    public void testEscapeJson_UnicodeCharacters() {
        assertEquals("héllo", OutputUtils.escapeJson("héllo"));
    }
}
