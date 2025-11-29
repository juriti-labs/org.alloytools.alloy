# Mutation Testing Guide

This document describes how to run mutation testing for the Alloy project.

## Overview

Mutation testing is a technique to evaluate the quality of test suites by introducing small changes (mutations) to the code and checking if the tests can detect them. We use [PITest](https://pitest.org/) for mutation testing.

## Prerequisites

- Java 17 or later
- Gradle (wrapper included)

## Running Mutation Tests

### Run mutation tests on all subprojects

```bash
./gradlew mutationTest
```

### Run mutation tests on a specific subproject

```bash
./gradlew :org.alloytools.alloy.cli:pitest
```

### Run mutation tests on the core module

```bash
./gradlew :org.alloytools.alloy.core:pitest
```

## Viewing Reports

After running mutation tests, HTML reports are generated in:

```
<module>/build/reports/pitest/index.html
```

For example:
- `org.alloytools.alloy.cli/build/reports/pitest/index.html`
- `org.alloytools.alloy.core/build/reports/pitest/index.html`

## Understanding the Reports

### Mutation Score

The mutation score indicates the percentage of mutations that were detected (killed) by your tests:
- **100%** - All mutations were killed by tests (excellent test coverage)
- **80%+** - Good mutation coverage
- **60-80%** - Moderate coverage, consider adding more tests
- **<60%** - Poor coverage, significant improvements needed

### Mutation Types

PITest uses various mutation operators:
- **Conditionals Boundary**: Changes `<` to `<=`, `>` to `>=`, etc.
- **Increments**: Changes `++` to `--` and vice versa
- **Invert Negatives**: Removes negation operators
- **Math**: Changes arithmetic operators (`+` to `-`, etc.)
- **Negate Conditionals**: Changes `==` to `!=`, etc.
- **Return Values**: Changes return values
- **Void Method Calls**: Removes void method calls

## Configuration

The PITest configuration is in `build.gradle`:

```groovy
pitest {
    targetClasses = ['org.alloytools.**', 'edu.mit.csail.sdg.**']
    targetTests = ['org.alloytools.**', 'edu.mit.csail.sdg.**']
    mutators = ['DEFAULTS']
    outputFormats = ['HTML', 'XML']
    threads = Runtime.runtime.availableProcessors()
}
```

### Configuration Options

- `targetClasses`: Classes to mutate
- `targetTests`: Test classes to run
- `mutators`: Mutation operators to apply
- `outputFormats`: Report formats (HTML, XML)
- `threads`: Number of parallel threads

## Best Practices

1. **Start Small**: Run mutation tests on a single module first
2. **Focus on Critical Code**: Prioritize testing business-critical code
3. **Incremental Improvement**: Address surviving mutations gradually
4. **Exclude Generated Code**: Don't mutate auto-generated code
5. **Balance Speed**: Use fewer mutators for faster feedback

## Improving Test Coverage

When mutations survive (are not killed), consider:

1. **Add edge case tests**: Test boundary conditions
2. **Test error handling**: Verify exception behavior
3. **Verify return values**: Assert on actual return values
4. **Test state changes**: Verify side effects

## Continuous Integration

For CI, you can set a minimum mutation score threshold:

```groovy
pitest {
    mutationThreshold = 60  // Fail if mutation score < 60%
}
```

## Troubleshooting

### Out of Memory

Increase JVM heap size:
```groovy
pitest {
    jvmArgs = ['-Xmx2g']
}
```

### Slow Execution

Reduce scope or use incremental mutation testing:
```bash
./gradlew :org.alloytools.alloy.cli:pitest --info
```

### No Tests Found

Ensure tests are in the correct location:
- `src/test/java/` for test sources
- Test classes must end with `Test.java`
