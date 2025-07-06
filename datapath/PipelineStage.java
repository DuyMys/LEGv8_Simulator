package datapath;

/**
 * Represents the main stages of the classic 5-stage CPU pipeline.
 * This is used to associate a MicroStep with a specific stage for GUI highlighting.
 */
public enum PipelineStage {
    FETCH,
    DECODE,
    EXECUTE,
    MEMORY_ACCESS, // For LDUR/STUR
    WRITE_BACK,
    NONE // For states with no active stage
}