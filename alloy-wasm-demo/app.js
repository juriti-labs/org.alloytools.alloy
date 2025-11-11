// Example Alloy models
const examples = {
    simple: `// Simple example
sig Person {
    friends: set Person
}

// Friendship is symmetric
fact SymmetricFriends {
    all p1, p2: Person | p1 in p2.friends implies p2 in p1.friends
}

// Nobody is their own friend
fact NoSelfFriendship {
    no p: Person | p in p.friends
}

// Find an instance with at least 3 people
run {} for 3`,

    graph: `// Directed graph model
sig Node {
    edges: set Node
}

// No self-loops
fact NoSelfLoops {
    no n: Node | n in n.edges
}

// Graph is connected
pred connected {
    all n1, n2: Node | n1 in n2.*edges
}

// Find a connected graph
run connected for 4`,

    family: `// Family tree model
abstract sig Person {
    father: lone Man,
    mother: lone Woman
}

sig Man extends Person {}
sig Woman extends Person {}

// No one is their own ancestor
fact NoSelfAncestor {
    no p: Person | p in p.^(father + mother)
}

// Everyone has at most one father and one mother
fact UniqueParents {
    all p: Person | lone p.father and lone p.mother
}

run {} for 5`,

    filesystem: `// File system model
abstract sig Object {
    parent: lone Dir
}

sig File extends Object {}

sig Dir extends Object {
    contents: set Object
}

// Root directory has no parent
one sig Root extends Dir {} {
    no parent
}

// Contents are children
fact ContentsAreChildren {
    all d: Dir, o: Object | o in d.contents iff d = o.parent
}

// No cycles in directory structure
fact NoCycles {
    no d: Dir | d in d.^parent
}

run {} for 4`
};

class AlloyWASM {
    constructor() {
        this.initialized = false;
        this.worker = null;
    }

    async init() {
        try {
            updateStatus('loading', 'Initializing Alloy WASM engine...');
            
            // Simulate initialization - in a real implementation, this would load CheerpJ
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            this.initialized = true;
            updateStatus('success', 'Alloy engine ready! ✓');
            updateOutput('Alloy WASM engine initialized successfully.\nReady to execute models.\n\nNote: This is a proof-of-concept demo.\nIn production, this would use CheerpJ to run the actual Alloy JAR in the browser.');
            
            return true;
        } catch (error) {
            updateStatus('error', `Initialization failed: ${error.message}`);
            updateOutput(`Error: ${error.message}`);
            return false;
        }
    }

    async runModel(modelText) {
        if (!this.initialized) {
            throw new Error('Alloy engine not initialized');
        }

        try {
            updateStatus('loading', 'Executing model...');
            updateOutput('Parsing and analyzing model...\n');

            // Simulate model execution
            await new Promise(resolve => setTimeout(resolve, 1000));

            // Parse the model to extract basic info
            const sigMatches = modelText.match(/sig\s+(\w+)/g) || [];
            const signatures = sigMatches.map(m => m.replace('sig ', ''));
            
            const runMatch = modelText.match(/run\s+.*?for\s+(\d+)/);
            const scope = runMatch ? runMatch[1] : '3';

            // Generate mock output
            const output = this.generateMockOutput(signatures, scope, modelText);
            
            updateStatus('success', 'Model executed successfully! ✓');
            updateOutput(output);

            return { success: true, output };
        } catch (error) {
            updateStatus('error', `Execution failed: ${error.message}`);
            updateOutput(`Error: ${error.message}`);
            throw error;
        }
    }

    generateMockOutput(signatures, scope, modelText) {
        const timestamp = new Date().toISOString();
        let output = `═══════════════════════════════════════════════════════════\n`;
        output += `Alloy Analyzer - Model Execution Results\n`;
        output += `═══════════════════════════════════════════════════════════\n\n`;
        output += `Execution Time: ${timestamp}\n`;
        output += `Scope: ${scope}\n`;
        output += `Signatures found: ${signatures.join(', ') || 'none'}\n\n`;
        
        output += `───────────────────────────────────────────────────────────\n`;
        output += `Parsing Model...\n`;
        output += `✓ Syntax check passed\n`;
        output += `✓ Type checking passed\n`;
        output += `✓ Constraints analyzed\n\n`;
        
        output += `───────────────────────────────────────────────────────────\n`;
        output += `Searching for instances...\n`;
        output += `Using SAT solver: MiniSat\n\n`;
        
        output += `Instance found!\n\n`;
        
        if (signatures.length > 0) {
            output += `═══════════════════════════════════════════════════════════\n`;
            output += `INSTANCE 1\n`;
            output += `═══════════════════════════════════════════════════════════\n\n`;
            
            signatures.forEach((sig, idx) => {
                const count = Math.min(parseInt(scope), 3);
                output += `${sig}:\n`;
                for (let i = 0; i < count; i++) {
                    output += `  ${sig}${i}\n`;
                }
                output += `\n`;
            });
            
            // Check for relations
            const relMatches = modelText.match(/(\w+):\s*set\s+(\w+)/g) || [];
            if (relMatches.length > 0) {
                output += `Relations:\n`;
                relMatches.forEach(rel => {
                    const parts = rel.match(/(\w+):\s*set\s+(\w+)/);
                    if (parts) {
                        output += `  ${parts[1]}: {}\n`;
                    }
                });
                output += `\n`;
            }
        }
        
        output += `───────────────────────────────────────────────────────────\n`;
        output += `Satisfiable: YES\n`;
        output += `Time: ~${Math.floor(Math.random() * 500 + 100)}ms\n\n`;
        
        output += `NOTE: This is a simulated output for demonstration.\n`;
        output += `In production, CheerpJ would execute the actual Alloy JAR\n`;
        output += `and provide real analysis results.\n`;
        
        return output;
    }
}

// UI Helper functions
function updateStatus(type, message) {
    const statusEl = document.getElementById('status');
    statusEl.className = `status ${type}`;
    
    if (type === 'loading') {
        statusEl.innerHTML = `<div><span class="loader"></span>${message}</div>`;
    } else {
        statusEl.textContent = message;
    }
}

function updateOutput(text) {
    document.getElementById('output').textContent = text;
}

// Initialize the application
const alloy = new AlloyWASM();

document.addEventListener('DOMContentLoaded', async () => {
    const editor = document.getElementById('editor');
    const runBtn = document.getElementById('runBtn');
    const clearBtn = document.getElementById('clearBtn');
    const exampleSelect = document.getElementById('exampleSelect');

    // Initialize Alloy engine
    await alloy.init();

    // Run button handler
    runBtn.addEventListener('click', async () => {
        const modelText = editor.value.trim();
        
        if (!modelText) {
            updateStatus('error', 'Please enter an Alloy model');
            return;
        }

        runBtn.disabled = true;
        
        try {
            await alloy.runModel(modelText);
        } catch (error) {
            console.error('Error running model:', error);
        } finally {
            runBtn.disabled = false;
        }
    });

    // Clear button handler
    clearBtn.addEventListener('click', () => {
        editor.value = '';
        updateOutput('Output cleared. Ready for new model.');
        updateStatus('info', 'Ready to execute models');
    });

    // Example selector handler
    exampleSelect.addEventListener('change', (e) => {
        const selected = e.target.value;
        if (selected && examples[selected]) {
            editor.value = examples[selected];
            updateStatus('info', `Loaded example: ${e.target.options[e.target.selectedIndex].text}`);
            updateOutput('Example loaded. Click "Run Model" to execute.');
        }
    });

    // Allow Ctrl+Enter to run
    editor.addEventListener('keydown', (e) => {
        if (e.ctrlKey && e.key === 'Enter') {
            runBtn.click();
        }
    });
});
