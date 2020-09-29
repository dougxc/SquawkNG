
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.*;

/**
 * This is the base class for the IR instructions representing the Java bytecode
 * LOOKUPSWITCH and TABLESWITCH instructions.
 */
public abstract class SwitchInstruction extends BasicBlockExitDelimiter {

    /** The instruction that pushed the value being switched on. */
    private Instruction key;
    /** The targets corresponding to each "case" of the "switch". */
    private final Target[] targets;
    /** The target for the "default" case. */
    private final Target defaultTarget;
    /** The padding calculated for the instruction during relocation. */
    private int padding;

    /**
     * Create a SwitchInstruction.
     * @param key The instruction that pushed the value being switched on.
     * @param numTargets The number of cases in the switch.
     * @param defaultTarget The target for the "default" case.
     */
    SwitchInstruction(Instruction key, int numTargets, Target defaultTarget) {
        super(null);
        this.key = key;
        this.targets = new Target[numTargets];
        this.defaultTarget = defaultTarget;
    }

    /**
     * Add a target for a case.
     * @param index The index in the targets table.
     * @param target The address of the target.
     */
    public void addTarget(int index, Target target) {
        targets[index] = target;
    }

    /**
     * Return the instruction that pushed the value being switched on.
     * @return the instruction that pushed the value being switched on.
     */
    public Instruction key() {
        return key;
    }

    /**
     * Return the targets corresponding to each "case" of the "switch".
     * @return the targets corresponding to each "case" of the "switch".
     */
    public Target[] targets() {
        return targets;
    }

    /**
     * Return the target for the "default" case.
     * @return the target for the "default" case.
     */
    public Target defaultTarget() {
        return defaultTarget;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int padding() {
        return padding;
    }

    /**
     * Entry point for a visit from a ParameterVisitor object.
     * @param visitor The ParameterVisitor object.
     */
    public void visit(ParameterVisitor visitor) {
        key = visitor.doParameter(this, key);
    }

    /**
     * Implementation of BasicBlockEndDelimiter.
     * @return
     */
    protected Instruction[] createSuccessors() {
        Assert.that(targets.length == 0 || targets[targets.length - 1] != null, "Can't getTargets until instruction is completely built");
        Instruction[] bbSuccessors;
        bbSuccessors = new Instruction[targets.length + 1];
        for (int i = 0; i != targets.length; i++) {
            bbSuccessors[i + 1] = targets[i].getInstruction();
        }
        bbSuccessors[0] = defaultTarget.getInstruction();
        return bbSuccessors;
    }
}
