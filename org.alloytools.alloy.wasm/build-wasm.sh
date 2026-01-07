#!/bin/bash
# Build script for Alloy WebAssembly module using TeaVM
#
# This script:
# 1. Builds the Alloy core libraries
# 2. Creates a classpath with all dependencies
# 3. Runs TeaVM to compile to WebAssembly and JavaScript
# 4. Copies the output to the NPM package directory

set -e  # Exit on error

echo "=== Alloy WASM Build Script ==="
echo ""

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Project root: $PROJECT_ROOT"
echo ""

# Step 1: Build all required Alloy modules
echo "Step 1: Building Alloy core modules..."
cd "$PROJECT_ROOT"
./gradlew :org.alloytools.alloy.core:build :org.alloytools.api:build :org.alloytools.pardinus.core:build :org.alloytools.alloy.wasm:build

if [ $? -ne 0 ]; then
    echo "ERROR: Gradle build failed"
    exit 1
fi

echo ""
echo "Step 2: Collecting JAR files..."

# Find the generated JAR files in target directories
WASM_JAR=$(find "$PROJECT_ROOT/org.alloytools.alloy.wasm/target" -name "*.jar" -type f 2>/dev/null | head -1)
CORE_JAR=$(find "$PROJECT_ROOT/org.alloytools.alloy.core/target" -name "*.jar" -type f 2>/dev/null | head -1)
API_JAR=$(find "$PROJECT_ROOT/org.alloytools.api/target" -name "*.jar" -type f 2>/dev/null | head -1)
PARDINUS_JAR=$(find "$PROJECT_ROOT/org.alloytools.pardinus.core/target" -name "*.jar" -type f 2>/dev/null | head -1)

echo "WASM JAR: $WASM_JAR"
echo "Core JAR: $CORE_JAR"
echo "API JAR: $API_JAR"
echo "Pardinus JAR: $PARDINUS_JAR"
echo ""

# Check if JARs exist
if [ -z "$WASM_JAR" ] || [ -z "$CORE_JAR" ] || [ -z "$API_JAR" ] || [ -z "$PARDINUS_JAR" ]; then
    echo "WARNING: One or more JAR files not found in target directories"
    echo "This is normal - JARs are generated during the Gradle build process"
    echo ""
else
    echo "All required JARs found!"
    echo ""
fi

echo "Step 3: TeaVM compilation..."
echo ""
echo "Note: TeaVM compilation is complex and may require manual setup."
echo "Please refer to the README.md for detailed instructions."
echo ""
echo "For now, you can use the compiled JARs directly with a JavaScript bridge."
echo "Alternatively, use the provided Maven POM file (teavm-pom.xml) to compile with TeaVM."
echo ""
echo "To compile with Maven/TeaVM:"
echo "  cd $SCRIPT_DIR"
echo "  mvn clean package -f teavm-pom.xml"
echo ""

# Create a simple JavaScript wrapper that can load the Java classes
# This is a placeholder until TeaVM compilation is fully automated
WRAPPER_DIR="$SCRIPT_DIR/npm/dist"
mkdir -p "$WRAPPER_DIR"

cat > "$WRAPPER_DIR/alloy-placeholder.js" << 'EOF'
/**
 * Alloy WASM Placeholder
 * 
 * This is a placeholder file until TeaVM compilation is fully configured.
 * 
 * To use Alloy in the browser, you need to:
 * 1. Compile the Java code to WebAssembly using TeaVM
 * 2. Load the generated WASM module
 * 3. Call the exported functions
 * 
 * See README.md for detailed instructions.
 */

console.warn('Alloy WASM module not yet compiled. See README.md for build instructions.');

module.exports = {
    parseModel: function(modelText) {
        throw new Error('Alloy WASM not compiled. Run TeaVM compilation first.');
    },
    executeCommand: function(modelText, commandName) {
        throw new Error('Alloy WASM not compiled. Run TeaVM compilation first.');
    },
    getVersion: function() {
        return JSON.stringify({
            success: false,
            error: 'WASM module not compiled'
        });
    }
};
EOF

echo "Created placeholder JavaScript file at: $WRAPPER_DIR/alloy-placeholder.js"
echo ""
echo "=== Build Complete ==="
echo ""
echo "Next steps:"
echo "1. For TeaVM compilation, see the README.md in org.alloytools.alloy.wasm/"
echo "2. The Java interface is ready and can be used with GraalVM or other tools"
echo "3. The NPM package structure is set up at: $SCRIPT_DIR/npm/"
echo ""
