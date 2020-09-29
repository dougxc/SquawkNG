package com.sun.squawk.translator.loader;

import java.util.Vector;
import java.util.Stack;
import java.util.Enumeration;
import com.sun.squawk.util.IntHashtable;
import com.sun.squawk.translator.util.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.Type;
import com.sun.squawk.translator.Translator;
import com.sun.squawk.translator.Method;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.AssertionFailed;

/**
 * This class performs liveness analysis for all the local variables (as allocated
 * by javac) for a method and uses the results to refine the stack maps of the
 * method or to verify that a provided Liveness attribute is correct.
 * If not verifying and the stack maps are actually modified as a result, then this class
 * also re-allocates all the local variables so that they are given correctly
 * inferred type, something that is not guaranteed by the stack maps if they
 * don't reflect the true liveness of the variables.
 */
public final class LivenessDFA {

    /** The method being analysed. */
    private final Method method;
    /** The exception handlers. */
    private final ExceptionHandlerTable handlerTable;
    /** Translator context. */
    private final Translator vm;
    /** Tracing object. */
    private final Tracer tracer;
    /** The Liveness attribute encapsulating the result of the analysis. */
    private final Liveness livenessAttr;
    /** The execution frame used to build the method. */
    private final ExecutionFrame frame;
    /** Denotes whether or not the liveness sets are to be in terms of javac
        or squawk local indices. */
    private final boolean javac;


    /**
     * Do liveness analysis on the locals of a method to see if the stack maps
     * are correct with respect to liveness. That is they do not have non-bogus
     * entries for locals that are not live. If the stackmaps do not reflect the
     * correct liveness properties, correct the stackmap and re-allocate the
     * local variables for the method.
     * @param method
     * @param ilist
     * @param numLocals
     * @param maxStack
     * @param livenessAttr
     */
    public static void analyse(Method method,
                               InstructionList ilist,
                               int numLocals,
                               int maxStack,
                               Liveness livenessAttr,
                               boolean javac,
                               boolean reallocate,
                               LocalVariableTable localVariableTable) throws LinkageException
    {
        new LivenessDFA(method, ilist, numLocals, maxStack, livenessAttr, javac, reallocate, localVariableTable);
    }

    /**
     * Get the next non-pseudo instruction starting from a given instruction.
     * @param inst The instruction to start searching from.
     * @return the next non-pseudo instruction starting from inst onwards.
     */
    static Instruction real(Instruction inst) {
        while (inst instanceof PseudoInstruction) {
            inst = inst.getNext();
        }
        return inst;
    }

    /**
     * Get the unique address of a given instruction. This is in terms of logical
     * instruction adresses and will not necessarily correspond with the instruction's
     * original IP address. This is necessary to ensure each instruction has
     * a unique address, something not guaranteed by original IP addresses given
     * that the GraphBuilder may insert additional instructions.
     * @param inst the instruction for which a unique address is requested.
     * @return a unique address for inst.
     */
    static int ip(Instruction inst) {
        int ip = real(inst).getRelocIP();
        Assert.that(ip >= 0);
        return ip;
    }

    /**
     * Get the unique address of a given target. See above.
     * @param target
     * @return
     */
    static int ip(Target target) {
        return ip(target.getInstruction());
    }

    /**
     * Construct a LivenessDFA object and do the DFA analysis and variable
     * re-allocation (if necessary).
     * @param method
     * @param ilist
     * @param numLocals
     * @param maxStack
     * @param livenessAttr
     */
    private LivenessDFA(Method method,
                        InstructionList ilist,
                        int numLocals,
                        int maxStack,
                        Liveness livenessAttr,
                        boolean javac,
                        boolean reallocate,
                        LocalVariableTable localVariableTable) throws LinkageException
    {
        this.vm            = method.vm();
        this.method        = method;
        this.tracer        = new Tracer(System.err, vm.tracelivenessDFA(method.toString()));
        this.handlerTable  = ilist.getHandlerTable();
        this.livenessAttr  = livenessAttr;
        this.frame         = ilist.frame();
        this.javac         = javac;

        try {
            tracer.traceln("Doing liveness DFA on: " + method);

            // The original IP addresses cannot be relied upon to uniquely idenitfy
            // basic blocks as the GraphBuilder may have inserted instructions
            // that were not in the original code (e.g. the exception handler
            // and monitorenter/monitorexit wrapping a synchronized method).
            ilist.logicallyRelocate();

            // Build the control flow graph
            Vector cfg = buildCFG(ilist, numLocals);

            // Sort the CFG into depth first search order
            Vector dfsCfg = buildDFS(cfg);

            // Initialize or verify 'live in' set for first basic block from parameters
            Assert.that(ilist.frame() != null);
            Local[] parmLocals = ilist.frame().getParmLocals();
            BasicBlock bb = (BasicBlock)dfsCfg.firstElement();
            for (int i = 0; i != parmLocals.length; i++) {
                Local parm = parmLocals[i];
                bb.liveIn(parm);
                if (parm.type().isTwoWords()) {
                    bb.liveIn(parm.secondWordLocal());
                    i++;
                }
            }

            // Do the data flow analysis
            doDFA(dfsCfg);

            // Do local variable re-allocation
            try {
                // Trace basic blocks with liveness info
                if (tracer.switchedOn()) {
                    tracer.traceln("Basic blocks:");
                    for (Enumeration e = cfg.elements(); e.hasMoreElements(); ) {
                        bb = (BasicBlock)e.nextElement();
                        tracer.traceln("  "+bb);
                    }
                }
                if (reallocate) {
                    ilist.changeFrame(new VariableReallocator(method, cfg, numLocals, (maxStack+4), tracer, localVariableTable));
                }
            } catch (LinkageException le) {
                le.printStackTrace();
                Assert.shouldNotReachHere();
            }

        } catch (AssertionFailed ae) {
            ae.addContext("While doing liveness DFA on: " + method);
            throw ae;
        }

    }

    /**
     * Do a depth first search of the CFG from a given BB, building successor
     * BBs and adding them to the DFS ordering if they haven't already been added.
     * @param bb
     * @param numLocals
     * @param ipToBB
     */
    private Vector buildCFG(InstructionList ilist, int numLocals) throws LinkageException {
        Vector cfg           = new Vector();
        Stack activeHandlers = new Stack();
        IntHashtable ipToBB  = new IntHashtable();
        BasicBlock bb                = null;

        // Build the basic blocks
        for (Instruction inst = ilist.head(); inst != null; inst = inst.getNext()) {

            // Handle the place holder instructions first.
            if (inst instanceof PseudoInstruction) {
                if (inst instanceof HandlerEnter) {
                    ExceptionHandlerTable.Entry handler = handlerTable.lookup(((HandlerEnter)inst).target());
                    activeHandlers.push(handler.handlerEntryTarget());
                    if (bb != null) {
                        Instruction prev = inst.getPrev();
                        bb.setEnd(prev, cfg, activeHandlers, ipToBB);
                        bb.initFromLivenessAttribute(livenessAttr, frame);
                        bb = null;
                    }
                }
                else if (inst instanceof HandlerExit) {
                    activeHandlers.pop();
                }
                else if (inst instanceof LoadException) {
                    Assert.that(bb == null);
                    LoadException lde = (LoadException)inst;
                    bb = new BasicBlock(inst, javac);
                }
                else if (inst instanceof Phi) {
                    if (bb != null) {
                        bb.setEnd(inst.getPrev(), cfg, activeHandlers, ipToBB);
                        bb.initFromLivenessAttribute(livenessAttr, frame);
                    }
                    bb = new BasicBlock(inst, javac);
                }
                else {
                    Assert.shouldNotReachHere();
                }
                continue;
            }
            else {
                if (bb == null) {
                    bb = new BasicBlock(inst, javac);
                }
            }

            // Now handle real instructions
            if (inst instanceof LocalVariableInstruction) {
                LocalVariableInstruction lvi = (LocalVariableInstruction)inst;
                Local local = lvi.local();
                if (lvi.isUse()) {
                    if (!bb.isDefined(local)) {
                        bb.uses(local);
                    }
                }
                if (lvi.isDefinition()) {
                    bb.defines(local);
                }
            }
            else if (inst instanceof BasicBlockExitDelimiter) {
                bb.setEnd(inst, cfg, activeHandlers, ipToBB);
                bb.initFromLivenessAttribute(livenessAttr, frame);
                bb = null;
            }
            else if (!activeHandlers.empty()) {
                if (inst instanceof Throw || (inst instanceof Trappable && ((Trappable)inst).canTrap())) {
                    bb.setEnd(inst, cfg, activeHandlers, ipToBB);
                    bb.initFromLivenessAttribute(livenessAttr, frame);
                    bb = null;
                }
            }
        }

        Assert.that(bb == null);

        // Link the basic blocks together by setting their predeccessors and successors
        for (Enumeration e = cfg.elements(); e.hasMoreElements(); ) {
            bb = (BasicBlock)e.nextElement();
            bb.setSuccessors(ipToBB);
        }
        return cfg;
    }

    /**
     * Build a depth first search ordering of a given control flow graph.
     * @param cfg
     */
    private Vector buildDFS(Vector cfg) {
        Vector dfs = new Vector(cfg.size());
        Assert.that(cfg.size() > 0);
        dfs(dfs, (BasicBlock)cfg.firstElement());
        Assert.that(dfs.size() <= cfg.size()); // There may have been unreachable blocks
        return dfs;
    }

    private void dfs(Vector dfs, BasicBlock bb) {
        if (dfs.contains(bb)) {
            return;
        }
        dfs.addElement(bb);
        for (Enumeration e = bb.successors(); e.hasMoreElements();) {
            dfs(dfs, (BasicBlock)e.nextElement());
        }
    }

    /**
     * Do the liveness analysis for a given control flow graph.
     * @param cfg The control flow graph to analyze.
     * @return true if the analysis resulted in modifications to the stack maps in the method.
     */
    private boolean doDFA(Vector cfg) throws LinkageException {
        // Do DFA
        boolean changed;
        do {
            changed = false;
            // Iterate through the BBs in reverse DFS order
            for (int i = cfg.size() - 1; i >= 0; --i) {
                BasicBlock bb = (BasicBlock)cfg.elementAt(i);
                changed = changed || bb.flow();
                if (livenessAttr != null && changed) {
                    frame.verify(false, "Liveness attribute differs from derived liveness for basic block: "+bb);
                }
            }
        } while (changed);

        // Copy live in/out sets into BB entry and exit instructions and
        // fix stack maps based on liveness info
        changed = false;
        for (Enumeration e = cfg.elements(); e.hasMoreElements(); ) {
            BasicBlock bb = (BasicBlock)e.nextElement();
            bb.setStartEndLiveness();
            changed = bb.fixStackMapEntry(vm) || changed;
        }
        return changed;
    }
}

/**
 * This class represent basic blocks for the purpose of liveness analysis. These
 * basic blocks are delimited by one of the following:
 *
 *   1, A stand control flow instruction (goto, conditional branch, return or throw)
 *   2, An instruction that can raise an exception when such that instruction is
 *      enclosed in a code range protected by one or more exception handlers.
 *   3, An non-control flow instruction immediately preceeding a merge point (Phi instruction).
 */
final class BasicBlock {
    /** A unique ID for the block (for debugging only). */
    private int id;
    /** The set of local variables that are determined to be live upon entry to the block. */
    private BitSet in;
    /** The set of local variables that are determined to be live upon exit from the block. */
    private BitSet out;
    /** The set of local variables that are defined within the block. */
    private BitSet def;
    /** The set of local variables that are used within the block prior to definition within the block. */
    private BitSet use;
    /** The first instruction in the block. */
    private final Instruction start;
    /** The last instruction within the block. */
    private Instruction end;
    /** The entry point(s) of the exception handler(s) enclosing the BB (if any). */
    private Target[] handlers;
    /** The control flow predecessors. */
    private final Vector predecessors;
    /** The control flow successors. */
    private final Vector successors;
    /** Denotes whether or not the liveness sets are to be in terms of javac
        or squawk local indices. */
    private final boolean javac;

    BasicBlock(Instruction start, boolean javac) {
        this.in  = new BitSet();
        this.out = new BitSet();
        this.def = new BitSet();
        this.use = new BitSet();
        Assert.that((!(start instanceof PseudoInstruction)) || start instanceof StackMapEntryInstruction);
        this.start = start;
        this.predecessors = new Vector();
        this.successors   = new Vector();
        this.javac        = javac;
    }

   /* ------------------------------------------------------------------------ *\
    *                    BitSet accessors                                      *
   \* ------------------------------------------------------------------------ */

    private static void set(BitSet bs, Local local, boolean javac) {
        int index = (javac ? local.javacIndex() : local.squawkIndex());
        Assert.that(index >= 0);
        bs.set(index);
        if (local.type().isTwoWords()) {
            bs.set(index+1);
        }
    }
    private static boolean isSet(BitSet bs, Local local, boolean javac) {
        int index = (javac ? local.javacIndex() : local.squawkIndex());
        return bs.get(index);
    }


    /** Update the uses set to include 'local'. */
    public void uses(Local local)         { set(use, local, javac); }
    /** Update the defines set to include 'local'. */
    public void defines(Local local)      { set(def, local, javac); }
    /** Update the live in set to include 'local'. */
    public void liveIn(Local local)       { set(in, local, javac);  }
    /** Update the live out set to include 'local'. */
    public void liveOut(Local local)      { set(out, local, javac); }
    /** Query whether or not 'local' is defined by this block. */
    public boolean isDefined(Local local) { return isSet(def, local, javac); }

    /** Verify that a given set matches the live ins for this basic block. */
//    public void verifyLiveIn(BitSet set, ExecutionFrame frame) throws LinkageException {
//        frame.verify(set.equals(in), "Live in set ("+set+") in Liveness attribute does not match derived live in set ("+in+") for basic block: "+this);
//    }
    /** Verify that a given set matches the live outs for this basic block. */
//    public void verifyLiveOut(BitSet set, ExecutionFrame frame) throws LinkageException {
//        frame.verify(set.equals(out), "Live out set ("+set+") in Liveness attribute does not match derived live out set ("+out+") for basic block: "+this);
//    }

    /**
     * Initialize the in/out sets of this basic block from a Liveness attribute.
     * @param attr
     * @param frame
     * @throws LinkageException
     */
    public void initFromLivenessAttribute(Liveness attr, ExecutionFrame frame) throws LinkageException {
        if (attr != null && (start instanceof StackMapEntryInstruction)) {
            Liveness.Entry entry = attr.lookupBBEntry(start.getOriginalIP());
            if (entry == null) {
                frame.verify(false, "Missing live in for basic block: "+this);
            }
            in.or(entry.livenessSet);
            Liveness.Entry exit = attr.lookupBBExit(end.getOriginalIP());
            if (exit == null) {
                frame.verify(false, "Missing live out for basic block: "+this);
            }
            out.or(exit.livenessSet);
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                Start/end/successor/predecessor accessors                 *
   \* ------------------------------------------------------------------------ */

    public void setEnd(Instruction end, Vector bbList, Stack activeHandlers, IntHashtable ipToBB) {
        Assert.that(this.end == null && end != null);
        while (end instanceof HandlerExit) {
            end = end.getPrev();
        }

        /*
         * This is a test that prevents a basic block being constructed for the
         * following sequence:
         *
         *    phi
         *    nop
         *    phi
         *
         * which occurs in at least one TCK test: javasoft.sqe.tests.vm.classfmt.vrf.vrfpsf202.vrfpsf20201m1.vrfpsf20201m1p
         */
        if (end instanceof PseudoInstruction && this.start == end) {
            return;
        }

        Assert.that(!(end instanceof PseudoInstruction));
        this.end = end;
        Assert.that(start != null);
        Object o = ipToBB.put(LivenessDFA.ip(start), this);
        Assert.that(o == null);
        if (!activeHandlers.empty()) {
            if (end instanceof Throw || (end instanceof Trappable && ((Trappable)end).canTrap())) {
                Assert.that(!activeHandlers.empty());
                handlers = new Target[activeHandlers.size()];
                activeHandlers.copyInto(handlers);
            }
        }
        Assert.that(!bbList.contains(this));
        bbList.addElement(this);
        id = bbList.size();
    }

    /**
     * Initialize the links between this basic block and it's successors. This also
     * updates the predecessors list of each successor to include this block.
     * @param ipToBB
     */
    public void setSuccessors(IntHashtable ipToBB) {
        if (end instanceof BasicBlockExitDelimiter) {
            // Add all the control flow successors determined by a control flow instruction
            Instruction[] successors = ((BasicBlockExitDelimiter)end).getSuccessors();
            for (int i = 0; i != successors.length; i++) {
                Instruction successor = successors[i];
                Assert.that(successor != null);
                BasicBlock targetBB = (BasicBlock)ipToBB.get(LivenessDFA.ip(successor));
                addSuccessor(targetBB);
            }
        }
        else {
            // Link to the lexically succeeding basic block as this
            // basic block is not delimited by a control flow instruction
            BasicBlock targetBB = (BasicBlock)ipToBB.get(LivenessDFA.ip(end.getNext()));
            addSuccessor(targetBB);
        }

        if (handlers != null) {
            // Add all exception handler entry blocks as successor to this block
            // which is delimited by an instruction that may throw an exception
            for (int i = 0; i != handlers.length; i++) {
                BasicBlock targetBB = (BasicBlock)ipToBB.get(LivenessDFA.ip(handlers[i]));
                addSuccessor(targetBB);
            }
        }
    }

    /**
     * Get the successors of this basic block in an Enumeration.
     * @return the successors of this basic block in an Enumeration.
     */
    public Enumeration successors() {
        return successors.elements();
    }

    private void addSuccessor(BasicBlock successor) {
        Assert.that(successor != null);
        if (!successors.contains(successor)) {
            successors.addElement(successor);
        }
        if (!successor.predecessors.contains(this)) {
            successor.predecessors.addElement(this);
        }
    }

   /* ------------------------------------------------------------------------ *\
    *                         Data flow analysis                               *
   \* ------------------------------------------------------------------------ */

    /**
     * Update the liveness of a given basic block.
     * @return true if the liveness info for the basic block changed.
     */
    public boolean flow() {
        // out = U (succcessors s) (in(s))
        if (successors.size() != 0) {
            out = new BitSet();
            for (Enumeration e = successors.elements(); e.hasMoreElements(); ) {
                BasicBlock succ = (BasicBlock)e.nextElement();
                out.or(succ.in);
            }
        }

        // in = use U (out - def)
        BitSet newIn = new BitSet();
        newIn.or(out);
        newIn.andNot(def); // out - def
        newIn.or(use);     // use U (out - def)

        // did anything change?
        boolean changed = ! in.equals(newIn);
        in = newIn;

        return changed;
    }

    /**
     * Ensure that the stack map (if any) at the start of this basic block
     * reflects the correct liveness of locals upon entry to the basic block.
     * @param vm
     * @return
     */
    public boolean fixStackMapEntry(Translator vm) {
        if (!javac || !(start instanceof StackMapEntryInstruction)) {
            return false;
        }
        StackMap.Entry stackMapEntry = ((StackMapEntryInstruction)start).stackMapEntry();
        boolean changed = false;
        Type[] locals = stackMapEntry.locals;
        for (int javacIndex = 0; javacIndex != locals.length; javacIndex++) {
            Type ltype = locals[javacIndex];
            if (ltype != vm.BOGUS && !in.get(javacIndex)) {
                // The stack map was a little too eager...
                changed = true;
                locals[javacIndex] = vm.BOGUS;
                if (ltype.isTwoWords()) {
                    Assert.that(javacIndex + 1 < locals.length);
                    Assert.that(locals[javacIndex + 1] == ltype.secondWordType());
                    locals[javacIndex++] = vm.BOGUS;
                }
            }
        }
        return changed;
    }

    /**
     * Copy the live in/out sets of the basic block into it's start and end instructions.
     */
    public void setStartEndLiveness() {
        if (start instanceof BasicBlockEntryDelimiter) {
            Assert.that(in != null);
            ((BasicBlockEntryDelimiter)start).setBBLiveIn(in);
        }
        if (end instanceof BasicBlockExitDelimiter) {
            BasicBlockExitDelimiter delim = (BasicBlockExitDelimiter)end;
            delim.setBBLiveOut(out);
            if (delim.fallsThrough()) {
                Assert.that(delim instanceof IfOp);
                Assert.that(successors.size() <= 2);
                for (Enumeration e = successors.elements(); e.hasMoreElements();) {
                    BasicBlock successor = (BasicBlock)e.nextElement();
                    if (successor.id == id+1) {
                        delim.setFallThroughBBLiveIn(successor.in);
                        return;
                    }
                }
                Assert.shouldNotReachHere();
            }
        } else if (end instanceof Trappable && handlers != null) {
            Trappable trap = (Trappable)end;
            trap.setBBLiveOut(out);
            trap.setSuccessorBBLiveIn(((BasicBlock)successors.firstElement()).in);
        }

    }

    /**
     * Use a VariableReallocator instruction visitor to re-allocate the local variables
     * defined/used by the instructions of this BB.
     * @param visitor
     */
    public boolean visit(VariableReallocator visitor, boolean fellThrough) throws LinkageException {

        // Do any required merges
        if (start instanceof StackMapEntryInstruction) {
            Target target = ((StackMapEntryInstruction)start).target();
            if (!fellThrough) {

                if (target.isExceptionTarget()) {
                    // Occasionally a TCK test or optimized code (e.g. BCO) will contain
                    // a sequence of code such that there is something on the stack at the
                    // 'goto' that delimits a try-catch block. This value will be correctly
                    // spilled and filled as necessary. However, to correctly handle the
                    // code in the exception handler, the stack must be cleared on entry
                    // to the handler, just as the stack is conceptually cleared during
                    // execution.
                    while (visitor.stackSize(true) != 0) {
                        visitor.pop(visitor.vm.UNIVERSE);
                    }
                }

                // There is no flow from the lexical predecessor so ignore the
                // derived stack and locals and re-initialize them from the target.
                visitor.reinitialize(target);
            }

            // Merge frame into target
            visitor.mergeWithTarget(target);
        }

        // Iterate through the instructions of the basic block
        Instruction inst = start;
        while (true) {
            if (visitor.tracer.switchedOn()) {
                int size = visitor.copyStackState(visitor.stackState, false);
                visitor.tracer.traceln("\t\tstack={"+Type.toSignature(visitor.stackState, 0, size, ", ")+"}");
                visitor.tracer.traceln("\t\tlocals={"+visitor.localsStateToString(true)+"}");
                visitor.tracer.traceln(inst.getRelocIP()+":\t"+inst.toString(false, true, true));
            }
            inst.visit(visitor);

            // Model the effect of the original dup/swap following this instruction (if any)
            byte[] dupSwapOpcodes = inst.getDupSwapSuccessors();
            if (dupSwapOpcodes != null) {
                for (int i = 0; i != dupSwapOpcodes.length; i++) {
                    int opcode = dupSwapOpcodes[i] & 0xFF;
                    if (visitor.tracer.switchedOn()) {
                        visitor.tracer.traceln("\t"+JVMConst.mnemonics[opcode]);
                    }
                    visitor.dupSwap(opcode);
                }
            }

            if (inst == end) {
                break;
            }
            inst = inst.getNext();
        }

        boolean fallsThrough = end.fallsThrough();

        // Propogate the current stack state to all non-fall through successors.
        for (Enumeration e = successors.elements(); e.hasMoreElements();) {
            BasicBlock successor = (BasicBlock)e.nextElement();
            if ((!fallsThrough || successor.id != id+1) && (successor.start instanceof StackMapEntryInstruction)) {
                visitor.mergeWithTarget(((StackMapEntryInstruction)successor.start).target());
            }
        }

        return fallsThrough;
    }

   /* ------------------------------------------------------------------------ *\
    *                         Debug support methods                            *
   \* ------------------------------------------------------------------------ */

    public boolean equals(Object o) {
        if (o instanceof BasicBlock) {
            return ((BasicBlock)o).start == start;
        }
        return false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(20);
        buf.append("bb").append(id).
                append(" [").
                append(start == null ? "?" : ""+(javac ? start.getOriginalIP() : start.getRelocIP())).
                append(" - ").
                append(end == null ? "?" : ""+(javac ? end.getOriginalIP() : end.getRelocIP())).
                append("]:");
        append(buf, " pred", predecessors);
        append(buf, " succ", successors);
        append(buf, " in", in);
        append(buf, " out", out);
        append(buf, " def", def);
        append(buf, " use", use);
        return buf.toString();
    }
    private void append(StringBuffer buf, String name, Vector bbs) {
        if (!bbs.isEmpty()) {
            buf.append(name).append("={ ");
            for (Enumeration e = bbs.elements(); e.hasMoreElements();) {
                buf.append("bb").append(((BasicBlock)e.nextElement()).id).append(" ");
            }
            buf.append("}");
        }
    }
    private void append(StringBuffer buf, String name, BitSet bs) {
        if (bs.cardinality() != 0) {
            buf.append(name).append("=").append(bs);
        }
    }
}

/**
 * This class implements a graph walker that re-allocates the local variables for a method
 * based on the results of liveness analysis. This operation must be performed on a method
 * before it is passed to the GraphTransformer as this class does not know how to handle
 * TemporaryLocals.
 */
class VariableReallocator extends ExecutionFrame implements InstructionVisitor {

    final Tracer tracer;
    final Type[] stackState;

    /**
     *
     * @param method
     * @param cfg
     * @param numLocals
     * @param maxStack
     * @param localVariableTable
     * @param tracer
     * @throws LinkageException
     */
    VariableReallocator(Method method,
                        Vector cfg,
                        int numLocals,
                        int maxStack,
                        Tracer tracer,
                        LocalVariableTable localVariableTable)  throws LinkageException
    {
        super(method.vm(),
              method,
              false,
              null,
              new InstructionFactory(method.vm(), method),
              maxStack,
              new Local[numLocals],
              FLAG_MERGE_LOCAL_VARIABLES |
              FLAG_REUSABLE_LOCAL_VARIABLES,
              localVariableTable);
        this.tracer     = tracer;
        this.stackState = new Type[maxStack];
        tracer.traceln("Re-allocating variables for: " + method);
        boolean fellThrough = true;
        for (Enumeration e = cfg.elements(); e.hasMoreElements();) {
            fellThrough = ((BasicBlock)e.nextElement()).visit(this, fellThrough);
        }

        // Complete the emulation
        finish();
    }

    public void doArithmeticOp      (ArithmeticOp       inst) throws LinkageException { pop(inst.right()); pop(inst.left()); push(inst);}
    public void doArrayLength       (ArrayLength        inst) throws LinkageException { pop(inst.array()); push(inst); }
    public void doCheckCast         (CheckCast          inst) throws LinkageException { pop(inst.value()); push(inst); }
    public void doConvertOp         (ConvertOp          inst) throws LinkageException { pop(inst.value()); push(inst); }
    public void doGoto              (Goto               inst) throws LinkageException { }
    public void doHandlerEnter      (HandlerEnter       inst) throws LinkageException { }
    public void doHandlerExit       (HandlerExit        inst) throws LinkageException { }
    public void doIfOp              (IfOp               inst) throws LinkageException { pop(inst.right()); if (inst.left() != null) pop(inst.left()); }
    public void doInstanceOf        (InstanceOf         inst) throws LinkageException { pop(inst.value()); push(inst); }
    public void doLoadConstant      (LoadConstant       inst) throws LinkageException { push(inst); }
    public void doLoadConstantObject(LoadConstantObject inst) throws LinkageException { push(inst); }
    public void doLoadException     (LoadException      inst) throws LinkageException { push(inst); }
    public void doLoadField         (LoadField          inst) throws LinkageException { if (inst.ref() != null) pop(inst.ref()); push(inst); }
    public void doLoadIndexed       (LoadIndexed        inst) throws LinkageException { pop(inst.index()); pop(inst.array().type(), inst.type()); push(inst);    }
    public void doLookupSwitch      (LookupSwitch       inst) throws LinkageException { pop(inst.key()); }
    public void doLookup            (Lookup             inst) throws LinkageException { pop(inst.key()); push(inst); }
    public void doMonitorEnter      (MonitorEnter       inst) throws LinkageException { if (inst.value() != null) pop (inst.value()); }
    public void doMonitorExit       (MonitorExit        inst) throws LinkageException { if (inst.value() != null) pop (inst.value()); }
    public void doNegateOp          (NegateOp           inst) throws LinkageException { pop(inst.value()); push(inst); }
    public void doNewArray          (NewArray           inst) throws LinkageException { pop(inst.size()); push(inst); }
    public void doNewDimension      (NewDimension       inst) throws LinkageException { pop(inst.array()); pop(inst.dimension()); push(inst); }
    public void doNewMultiArray     (NewMultiArray      inst) throws LinkageException { Assert.shouldNotReachHere(); }
    public void doNewObject         (NewObject          inst) throws LinkageException { push(inst); }
    public void doPhi               (Phi                inst) throws LinkageException { }
    public void doPop               (Pop                inst) throws LinkageException { pop(vm.UNIVERSE); }
    public void doReturn            (Return             inst) throws LinkageException { if (inst.value() != null) pop(inst.value()); }
    public void doStoreField        (StoreField         inst) throws LinkageException { pop(inst.value()); if (inst.ref() != null) pop(inst.ref()); }
    public void doStoreIndexed      (StoreIndexed       inst) throws LinkageException { pop(inst.value()); pop(inst.index()); pop(inst.array().type(), inst.value().type()); }
    public void doTableSwitch       (TableSwitch        inst) throws LinkageException { pop(inst.key()); }
    public void doThrow             (Throw              inst) throws LinkageException { pop(inst.value()); }
    public void doInvoke            (Invoke             inst) throws LinkageException {

        if (inst.form() == Invoke.Form.INIT) {
            // Change the receiver type from INITOBJECT if this is an <init> method
            renameInitTo(method.parent());
        }

        // Pop the parameters
        Type[] parmTypes = inst.method().getParms();
        for (int i = parmTypes.length - 1; i >= 0; i--) {
            pop(parmTypes[i]);
        }

        if (inst.form() != Invoke.Form.STATIC) {
            Instruction receiver = pop(vm.NULLOBJECT);

            if (inst.form() == Invoke.Form.INIT && receiver instanceof LoadLocal) {
                LoadLocal ld = (LoadLocal)receiver;
                if (ld.local().type() instanceof TypeProxy) {
                    ld.local().changeType(inst.method().parent());
                }
            }

            Local.narrowLocalType(receiver, inst.method().parent(), true);

            if (inst.form() == Invoke.Form.INIT) {
                byte[] dupSwaps = receiver.getDupSwapSuccessors();
                Assert.that(dupSwaps == null || receiver.wasDuped());
                if (dupSwaps != null) {
                    Assert.that(dupSwaps[0] == JVMConst.opc_dup, "Translator cannot handle stack manipulation other than 'dup' of an uninitialised instance");

                    // 'undo' the duping of the new
                    receiver.setDuped(false);

                    // Pop the receiver as it will be replaced with the result of this invoke
                    Assert.that(peek() == receiver);
                    pop(receiver.type());
                }
            }
        }

        if (inst.type() != vm.VOID) {
            push(inst);
        }

    }

    public void doIncDecLocal(IncDecLocal inst) throws LinkageException {
        Local oldLocal = inst.local();
        inst.updateLocal(null, false);
        int javacIndex = oldLocal.javacIndex();
        Assert.that(javacIndex != -1);
        Local local = getLocal(vm.INT, javacIndex);
        inst.updateLocal(local, true);
    }

    public void doStoreLocal(StoreLocal inst) throws LinkageException {
        Local oldLocal = inst.local();
        inst.updateLocal(null, false);
        int javacIndex = oldLocal.javacIndex();
        Assert.that(javacIndex != -1);
        Instruction parm = pop(inst.value());
        Type actualType = parm.type();
        verifyLocalIndex(actualType, javacIndex);
        Local local = allocLocal(actualType.localType(), javacIndex, -1, oldLocal.name());
        setLocal(local, javacIndex);
        inst.updateLocal(local, true);
    }

    public void doLoadLocal (LoadLocal inst) throws LinkageException {
        Local oldLocal = inst.local();
        inst.updateLocal(null, false);
        int javacIndex = oldLocal.javacIndex();
        Assert.that(javacIndex != -1);
        Type basicType = oldLocal.type();
        if (!basicType.isPrimitive()) {
            basicType = vm.OBJECT;
        }
        Local local = getLocal(basicType, javacIndex);
        if (local.type() == vm.INITOBJECT) {
            inst.changeType(vm.INITOBJECT);
        }
        inst.updateLocal(local, true);
        push(inst);
    }

    /*------------------------------------------------------------------------*\
     *              Stack modeling/verification methods                       *
    \*------------------------------------------------------------------------*/

    public void push(Instruction inst) throws LinkageException {
            super.push(inst, false);
    }

    /**
     * Pop an instruction from the stack, verifying that it is assignable
     * to a specified type.
     * @param expectedType The type to which the instruction on the top of the
     * stack should be assignable.
     * @return the instruction that was popped.
     */
    public Instruction pop(Type expectedType) throws LinkageException {
        Instruction inst = super.pop(expectedType);
        Local.narrowLocalType(inst, expectedType, true);
        return inst;
    }

    /**
     * Pop the receiver (i.e. the instruction pushing the array reference) for
     * a array load or store instruction.
     * @param expectedArrayType The expected type of the array.
     * @param elementType The type of the array's elements.
     * @return the instruction that pushed the array reference.
     */
    public Instruction pop(Type expectedArrayType, Type elementType) throws LinkageException {
        if (expectedArrayType == vm.NULLOBJECT) {
            return (pop(elementType.asArray()));
        }
        return pop(expectedArrayType);
    }

    /**
     * A helper method that simply extracts the type from an instruction and
     * calls the pop() method that takes a Type parameter.
     * @param parm the instruction denoting the type that should be on top of
     * the stack.
     * @return the instruction popped off the stack.
     */
    public Instruction pop(Instruction parm) throws LinkageException {
        Assert.that(parm.type() != null);
        return pop(parm.type());
    }
}
