package org.alloytools.alloy.training;

/**
 * Represents statistics about an Alloy instance/model.
 * Used for training data that may include cost/complexity metrics.
 */
public class InstanceStats {
    
    /**
     * Total number of atoms in the instance.
     */
    public int atomCount;
    
    /**
     * Number of distinct signatures used.
     */
    public int signatureCount;
    
    /**
     * Number of distinct relations in the instance.
     */
    public int relationCount;
    
    /**
     * Total number of tuples across all relations.
     */
    public int tupleCount;
    
    /**
     * Number of skolem constants in the solution.
     */
    public int skolemCount;
    
    /**
     * Trace length for temporal models (1 for non-temporal).
     */
    public int traceLength;
    
    /**
     * Loop state for temporal models (-1 if not applicable).
     */
    public int loopState;
    
    /**
     * Default constructor.
     */
    public InstanceStats() {
        this.traceLength = 1;
        this.loopState = -1;
    }
    
    @Override
    public String toString() {
        return String.format("InstanceStats{atoms=%d, sigs=%d, rels=%d, tuples=%d, skolems=%d}", 
            atomCount, signatureCount, relationCount, tupleCount, skolemCount);
    }
}
