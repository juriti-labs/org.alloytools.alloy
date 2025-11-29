package org.alloytools.alloy.training;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the TrainingDataGenerator class.
 */
public class TrainingDataGeneratorTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private TrainingDataGenerator generator;
    
    @Before
    public void setUp() {
        generator = new TrainingDataGenerator();
    }
    
    @After
    public void tearDown() {
        if (generator != null) {
            generator.shutdown();
        }
    }
    
    @Test
    public void testAddPositiveExample() {
        String instruction = "Model a directed graph";
        String code = "sig Node { edges: set Node }\nrun { some Node } for 3";
        
        TrainingExample example = generator.addPositiveExample(
            instruction, code, TrainingTaskType.NL_TO_ALLOY);
        
        assertNotNull(example);
        assertEquals(TrainingTaskType.NL_TO_ALLOY, example.taskType);
        assertEquals(instruction, example.instruction);
        assertEquals(code, example.targetOutput);
        assertTrue(example.isPositive);
        assertNotNull(example.id);
    }
    
    @Test
    public void testProcessCandidateSuccess() {
        String instruction = "Model a simple node";
        String code = "sig Node {}\nrun { some Node } for 3";
        
        OracleResponse response = generator.processCandidate(instruction, code);
        
        assertEquals(OracleStatus.INSTANCE_FOUND, response.status);
        
        List<TrainingExample> examples = generator.getPositiveExamples();
        assertEquals(1, examples.size());
        assertTrue(examples.get(0).isPositive);
    }
    
    @Test
    public void testProcessCandidateParseError() {
        String instruction = "Model a node";
        String code = "sig Node { edges set Node }";  // Invalid syntax
        
        OracleResponse response = generator.processCandidate(instruction, code);
        
        assertEquals(OracleStatus.PARSE_ERROR, response.status);
        
        List<TrainingExample> needsCorrection = generator.getExamplesNeedingCorrection();
        assertEquals(1, needsCorrection.size());
        assertEquals(TrainingTaskType.FIX_PARSE_ERROR, needsCorrection.get(0).taskType);
    }
    
    @Test
    public void testAddFixExample() {
        String originalCode = "sig Node { edges set Node }";
        String correctedCode = "sig Node { edges: set Node }\nrun { some Node } for 3";
        String errorMessage = "Syntax error: expected ':'";
        
        TrainingExample example = generator.addFixExample(
            originalCode, correctedCode, errorMessage);
        
        assertNotNull(example);
        assertEquals(TrainingTaskType.FIX_PARSE_ERROR, example.taskType);
        assertEquals(originalCode, example.alloyContext);
        assertEquals(correctedCode, example.targetOutput);
        assertTrue(example.isPositive);
    }
    
    @Test
    public void testAddAssertionExample() {
        String moduleCode = "sig Node { edges: set Node }";
        String assertionCode = "assert NoSelfLoops { no n: Node | n in n.edges }\ncheck NoSelfLoops for 5";
        String description = "Assert that no node has an edge to itself";
        
        TrainingExample example = generator.addAssertionExample(
            moduleCode, assertionCode, description);
        
        assertNotNull(example);
        assertEquals(TrainingTaskType.GENERATE_ASSERTION, example.taskType);
        assertEquals(moduleCode, example.alloyContext);
        assertEquals(assertionCode, example.targetOutput);
    }
    
    @Test
    public void testGetPositiveExamples() {
        generator.addPositiveExample("Model A", "sig A {}\nrun {} for 3", TrainingTaskType.NL_TO_ALLOY);
        generator.addPositiveExample("Model B", "sig B {}\nrun {} for 3", TrainingTaskType.NL_TO_ALLOY);
        generator.processCandidate("Model C", "sig C { invalid }");  // Will fail
        
        List<TrainingExample> positives = generator.getPositiveExamples();
        assertEquals(2, positives.size());
        
        List<TrainingExample> all = generator.getExamples();
        assertEquals(3, all.size());
    }
    
    @Test
    public void testSetGeneration() {
        generator.setGeneration(2);
        TrainingExample example = generator.addPositiveExample(
            "Test", "sig Test {}\nrun {} for 3", TrainingTaskType.NL_TO_ALLOY);
        
        assertEquals(2, example.generation);
    }
    
    @Test
    public void testExportToJsonl() throws IOException {
        generator.addPositiveExample("Model a node", 
            "sig Node {}\nrun { some Node } for 3", 
            TrainingTaskType.NL_TO_ALLOY);
        
        File outputFile = tempFolder.newFile("training.jsonl");
        generator.exportToJsonl(outputFile);
        
        assertTrue(outputFile.exists());
        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("{"));
        assertTrue(lines.get(0).contains("\"input\""));
        assertTrue(lines.get(0).contains("\"output\""));
    }
    
    @Test
    public void testExportSimpleFormat() throws IOException {
        generator.addPositiveExample("Model a node", 
            "sig Node {}\nrun {} for 3", 
            TrainingTaskType.NL_TO_ALLOY);
        
        File outputFile = tempFolder.newFile("simple.jsonl");
        generator.exportSimpleFormat(outputFile);
        
        assertTrue(outputFile.exists());
        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"input\""));
        assertTrue(lines.get(0).contains("\"output\""));
    }
    
    @Test
    public void testClear() {
        generator.addPositiveExample("Test", "sig Test {}\nrun {} for 3", TrainingTaskType.NL_TO_ALLOY);
        assertEquals(1, generator.getExamples().size());
        
        generator.clear();
        assertEquals(0, generator.getExamples().size());
    }
    
    @Test
    public void testTrainingExampleFormatInput() {
        TrainingExample example = new TrainingExample();
        example.instruction = "Create a simple model";
        example.alloyContext = "sig Existing {}";
        example.compilerFeedback = "Error: missing field";
        
        String formatted = example.formatInput();
        
        assertTrue(formatted.contains("<INSTRUCTION>"));
        assertTrue(formatted.contains("Create a simple model"));
        assertTrue(formatted.contains("<ALLOY_CONTEXT>"));
        assertTrue(formatted.contains("sig Existing {}"));
        assertTrue(formatted.contains("<COMPILER_FEEDBACK>"));
        assertTrue(formatted.contains("Error: missing field"));
    }
    
    @Test
    public void testTrainingExampleToSimpleFormat() {
        TrainingExample example = new TrainingExample(
            TrainingTaskType.NL_TO_ALLOY,
            "Create a node",
            "sig Node {}"
        );
        
        java.util.Map<String, String> simple = example.toSimpleFormat();
        
        assertNotNull(simple.get("input"));
        assertEquals("sig Node {}", simple.get("output"));
    }
    
    @Test
    public void testToolCall() {
        TrainingExample.ToolCall toolCall = new TrainingExample.ToolCall("run_alloy");
        toolCall.arg("module", "sig Node {}")
                .arg("command", "run");
        
        assertEquals("run_alloy", toolCall.tool);
        assertEquals("sig Node {}", toolCall.args.get("module"));
        assertEquals("run", toolCall.args.get("command"));
    }
    
    @Test
    public void testTrainingTaskTypeFromId() {
        assertEquals(TrainingTaskType.NL_TO_ALLOY, TrainingTaskType.fromId("nl-to-alloy"));
        assertEquals(TrainingTaskType.FIX_PARSE_ERROR, TrainingTaskType.fromId("fix-parse-error"));
        assertEquals(TrainingTaskType.GENERATE_ASSERTION, TrainingTaskType.fromId("generate-assertion"));
        assertNull(TrainingTaskType.fromId("unknown"));
    }
    
    @Test
    public void testInstanceStatsDefaults() {
        InstanceStats stats = new InstanceStats();
        
        assertEquals(0, stats.atomCount);
        assertEquals(0, stats.signatureCount);
        assertEquals(1, stats.traceLength);
        assertEquals(-1, stats.loopState);
    }
    
    @Test
    public void testInstanceStatsToString() {
        InstanceStats stats = new InstanceStats();
        stats.atomCount = 5;
        stats.signatureCount = 2;
        
        String str = stats.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("2"));
    }
}
