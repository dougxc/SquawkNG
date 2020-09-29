package com.sun.squawk.translator.loader;

import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.NoSyncHashtable;
import java.util.Enumeration;

/**
 * This class implements the transformations that do the spilling and filling
 * required to make the IR conform with the restricted use of the stack.
 *
 * @invariant ilist != null
 * @invariant method != null
 * @invariant activeHandlers >= 0
 * @invariant usedLocals != null
 * @invariant ifactory != null
 * @invariant frame != null
 */
public class GraphTransformer implements ParameterVisitor {

    /** The instruction list currently being processed. */
    private final InstructionList ilist;
    /** Flags whether or not at least parameter of the current instruction needs spliing/filling. */
    private boolean oneOrMoreParmsFilled;
    /** The method whose IR is being processed. */
    private final Method method;
    /** Tracks the nesting of exception handlers. */
    private int activeHandlers = 0;
    /** The local variables that are used by this method. */
    private final Hashtable usedLocals;
    /** Instruction factory used to create new instructions. */
    private final InstructionFactory ifactory;
    /** The execution frame used to allocate new Locals. */
    private final ExecutionFrame frame;

    /**
     * Transform the IR for a given method.
     * @param vm
     * @param method
     * @param ilist The instruction list built by GraphBuilder.
     *
     * @pre method != null && ilist != null
     * @post $none
     */
    public static void transform(Method method, InstructionList ilist) {
        new GraphTransformer(method, ilist).run();
    }

    /**
     * Private constructor.
     * @param method
     *
     * @pre method != null
     * @pre ilist != null
     */
    private GraphTransformer(Method method, InstructionList ilist) {
        this.method   = method;
        this.ilist    = ilist;
        this.usedLocals   = new Hashtable(10);
        this.frame    = ilist.frame();
        if (frame.getInstructionFactory() != null) {
            this.ifactory = frame.getInstructionFactory();
        }
        else {
            this.ifactory = new InstructionFactory(method.vm(), method);
        }
    }

    /**
     * Do the transformations.
     */
    private void run() {
        String name = method.toString();
        Translator vm = method.vm();
        if (vm.traceir1(name)) {
            vm.tracer().trace("Transforming "+name);
        }
        Instruction inst = null;
        try {

            // Add all the local variables corresponding to parameters
            Local[] parmLocals = frame.getParmLocals();
            int parm = 0;
            for (int i = 0; i != parmLocals.length; i++) {
                Local parmLocal = parmLocals[i];
                Assert.that(parmLocal.getParameterNumber() == parm++);
                Assert.that(parmLocal.mergeParent() == null);
                markAsUsed(parmLocal);
                if (parmLocal.type().isTwoWords()) {
                    i++;
                }
            }

            // Pass 1 - Iterate over all the instructions to insert loads for
            // stack values that must be spilled.
            inst = ilist.head();
            while (inst != null) {
                oneOrMoreParmsFilled = false;
                inst.visit(this);
                inst = inst.getNext();
            }
            Assert.that(activeHandlers == 0);

            // Pass 2 - Iterate over all the instructions to do spill stack
            // values for which loads were inserted in the previous pass.
            // Also, one other transformation for lookupswitches that has to be
            // done in this pass.
            for (inst = ilist.head() ; inst != null ; inst = inst.getNext()) {

                // Spill the instruction's result if necessary
                if (inst.needsSpilling()) {
                    TemporaryLocal local = inst.getTemporaryLocal();
                    Assert.that(local != null);

                    // If the next instruction is the fill corresponding to the pending
                    // spill and the spill was not due to a dup or a phi merge, then
                    // cancel the spill and remove the fill
                    if (!inst.wasDuped() &&
                        !inst.wasMerged() &&
                        inst.getNext() instanceof LoadLocal)
                    {
                        LoadLocal ld = (LoadLocal)inst.getNext();
                        if (ld.local() == local) {
                            ilist.remove(ld);
                            local.removeUse(ld);
                            continue;
                        }
                    }

                    // Do some extra type refinement for local variables
                    // of instructions that produce a value that is never
                    // consumed. That is, they produces the extra
                    // redundant values of the stack at a 'throw' or 'return'.
                    if (local.useDefsSize() == 0) {
                        if (local.type() == method.vm().NULLOBJECT) {
                            local.changeType(method.vm().OBJECT);
                        } else if (local.type() instanceof TypeProxy) {
                            Type realType = ((TypeProxy)local.type()).getProxy();
                            local.changeType(realType);
                        }
                    }
                    Instruction st = ifactory.createStoreLocal(local, inst);
                    st.setOriginal(inst);
                    ilist.insertAfter(st, inst);
                }

                // Replace a lookupswitch with no branches and only a default case with a pop/goto.
                // as there is no way to express this as a tableswitch (tableswitch must have at least
                // one non-default case).
                if (inst instanceof LookupSwitch) {
                    LookupSwitch si = (LookupSwitch)inst;
                    if (si.targets().length == 0) {
                        Target t = si.defaultTarget();
                        Assert.that(t != null);
                        Instruction value = si.key();

                        // Pop key or remove the instruction that loaded it
                        if (value instanceof LoadLocal || value instanceof LoadConstant) {
                            ilist.remove(value);
                            if (value instanceof LoadLocal) {
                                LoadLocal ld = (LoadLocal)value;
                                Local local = ld.local();
                                local.removeUse(ld);
                                // Only remove the local if if has no other uses/defs and is
                                // not a parameter
                                if (!local.isParameter() && local.useDefsSize() == 0) {
                                    ((Vector)usedLocals.get(local.type())).removeElement(local);
                                }
                            }
                        }
                        else {
                            Instruction pop = ifactory.createPop(value);
                            ilist.insertBefore(pop, si);
                        }

                        // Replace switch with goto
                        BasicBlockExitDelimiter go2 = (BasicBlockExitDelimiter)ifactory.createGoto(t);
                        go2.setBBLiveOut(((BasicBlockExitDelimiter)inst).getBBLiveOut());
                        ilist.insertBefore(go2, si);
                        ilist.remove(si);

                        continue;
                    }
                }


            }

            // Pass 3 - Iterate over all the instructions, finding the locals that are used
            for (inst = ilist.head() ; inst != null ; inst = inst.getNext()) {

                // If the instruction loads from/stores to a local then that
                // local must be marked as being used.
                if (inst instanceof LocalVariableInstruction) {
                    Local local = ((LocalVariableInstruction)inst).local();
                    Assert.that(local != null);
                    markAsUsed(local);
                }
            }

            // Give each instruction a logical address
            ilist.logicallyRelocate();

            // Allocate the local variables
            Local[] locals = allocateLocals(method.vm().optimizeLocalsAllocation(), vm.tracelocals(name) ? vm.tracer() : null);

            // Pass 4 - Optimizations that must be performed after local variable allocation
            boolean change;
            do {
                change = false;
                boolean seenBackwardBranchTarget = false;
                for (inst = ilist.head() ; inst != null ; inst = inst.getNext()) {

                    if (inst instanceof Phi) {
                        if (!seenBackwardBranchTarget) {
                            seenBackwardBranchTarget = ((Phi)inst).target().isBackwardTarget();
                        }
                    } else if (inst instanceof StoreLocal) {
                        StoreLocal st = (StoreLocal)inst;
                        Local local = st.local();
                        Assert.that(local != null);

                        // If this is the initialization of a local with the default value for
                        // the relevant type, then remove the LoadConstant and StoreLocal
                        // as the Squawk VM initializes all local variables upon method entry
                        // top their default values. Note that an extra condition for performing
                        // this optimization is that the local variable being stored to must not
                        // have been merged with another variable as stores to that variable could
                        // invalidate the auto-initialization of this local variable.
                        if (vm.optimizeLocalInitializers() &&
                            !seenBackwardBranchTarget &&
                            !local.isMergeParent() &&
                            st.isInitializer() &&
                            st.value() instanceof LoadConstant &&
                            ((LoadConstant)st.value()).isDefaultValue())
                        {
                            Assert.that(st.value() == st.getPrev());
                            ilist.remove(st);
                            ilist.remove(st.value());
                            local.removeDefinition(st);
                            local.setAutoInitialized();
                            change = true;
                            continue;
                        }

                        // Remove any redundant load/store pairs where the same local is being loaded and then
                        // immediately stored to.
                        if (st.getPrev() instanceof LoadLocal && ((LoadLocal)st.getPrev()).local() == local) {
                            LoadLocal ld = (LoadLocal)st.getPrev();
                            ilist.remove(st);
                            ilist.remove(ld);
                            local.removeDefinition(st);
                            local.removeUse(ld);
                            change = true;
                            continue;
                        }

                        // Remove redundant <spill,fill> sequences.
                        if (st.getNext() instanceof LoadLocal) {
                            LoadLocal ld = (LoadLocal)st.getNext();
                            if (local == ld.local()) {
                                Instruction value = st.value();
                                if (local instanceof TemporaryLocal && !value.wasMerged() && !value.wasDuped()) {
                                    ilist.remove(st);
                                    ilist.remove(ld);
                                    local.removeDefinition(st);
                                    local.removeUse(ld);
                                    change = true;
                                    continue;
                                }
                            }
                        }
                    }

                    // Remove any redundant gotos
                    if (inst instanceof Goto && !(inst instanceof IfOp)) {
                        Goto g = (Goto)inst;
                        Instruction realNext = g.getNext();
                        while (realNext instanceof PseudoInstruction) {
                            realNext = realNext.getNext();
                        }
                        if (g.target().getInstruction().getNext() == realNext) {
                            ilist.remove(inst);
                            change = true;
                        }
                        continue;
                    }

                    // Remove load of receiver in virtual methods for the purpose of a getfield/putfield.
                    // These will be implemented with the 'this_getfield' and 'this_putfield' instructions
                    // which access the local variable holding the receiver directly. For example:
                    //
                    //    load 0
                    //    getfield 4
                    //
                    // becomes:
                    //
                    //    this_getfield 4
                    //
                    if (vm.optimizeThisFieldAccess() && inst instanceof FieldInstruction && !method.isStatic()) {
                        FieldInstruction finst = (FieldInstruction)inst;
                        Field field = finst.field();
                        if (!field.isStatic() && finst.isFieldOfReceiver()) {
                            Instruction ref = finst.ref();
                            if (ref != null) {
                                Assert.that(ref instanceof LoadLocal);
                                Local local = ((LoadLocal)ref).local();
                                Assert.that(local.getParameterNumber() == 0 && local.name().indexOf("this") != -1);
                                ilist.remove(ref);
                                local.removeUse((LoadLocal)ref);
                                finst.removeRef();
                                change = true;
                            }
                        }
                        continue;
                    }

                }
            } while (change);

            // Remove unused locals
            locals = removedUnusedLocals(locals);
            ilist.setLocals(locals);

            // traceir1
            if (vm.traceir1(name)) {
                vm.tracer().traceln("");
                vm.tracer().traceln("\n++IR1 trace for "+name);
                ilist.trace(vm.tracer(), vm.loader(), method.parent().getSourceFilePath(), vm.traceip(), false);
                vm.tracer().traceln("\n--IR1 trace for "+name+"\n");
            }

            // tracelocals
            traceLocals(name, locals, true, true);

        } catch (AssertionFailed ae) {
            ae.addContext(name + (inst == null ? "" : " @ " + inst.getOriginalIP()));
            throw ae;
        }
    }

    private void traceLocals(String name, Local[] locals, boolean showLiveness, boolean graphLiveness) {
        Translator vm = method.vm();
        if (vm.tracelocals(name)) {
            Tracer tracer = vm.tracer();
            tracer.traceln("\nLocals used in "+name);
            int index = 0;
            for (int i = 0; i != locals.length; i++) {
                Local local = locals[i];
                if (local == null) {
                    continue;
                }
                tracer.trace((index++)+":\t"+local);
                if (local.isParameter()) {
                    tracer.trace("\t param="+local.getParameterNumber());
                }

                if (!local.type().isSecondWordType() && showLiveness) {
                    Range liveness = local.getLivenessRange(null);
                    if (liveness != null) {
                        tracer.trace("  liveness: "+Range.toString(liveness));
                    }
                }
                tracer.traceln("");
            }

            if (graphLiveness) {
                index = 0;
                // Graphically show live ranges
                for (int i = 0; i != locals.length; i++) {
                    Local local = locals[i];
                    if (local != null) {
                        Range liveness = local.getLivenessRange(null);
                        if (liveness != null) {
                            tracer.trace((index++)+":\t");
                            if (local.type().isSecondWordType()) {
                                local = locals[i - 1];
                            }
                            int pos = 0;
                            while (liveness != null) {
                                if (pos < liveness.start()) {
                                    tracer.trace(fillString(liveness.start()-pos, " "));
                                }
                                tracer.trace("+");
                                int length = liveness.end() - liveness.start();
                                if (length > 0) {
                                    if (length > 1) {
                                        tracer.trace(fillString(length-1, "-"));
                                    }
                                    tracer.trace("+");
                                }
                                pos = liveness.end()+1;
                                liveness = liveness.next();
                            }
                            tracer.traceln("");
                        }
                        tracer.traceln("");
                    }
                }
            }
        }
    }

    private String fillString(int count, String c) {
        StringBuffer buf = new StringBuffer(count);
        for (int i = 0; i != count; i++) {
            buf.append(c);
        }
        return buf.toString();
    }

    /**
     * Mark a given local as used.
     * @param local The local variable.
     */
    private void markAsUsed(Local local) {
        Assert.that(local.type() != method.vm().NULLOBJECT);
/*if[FLOATS]*/
        Assert.that(local.type() != method.vm().DOUBLE2);
/*end[FLOATS]*/
        Assert.that(local.type() != method.vm().LONG2);
        Assert.that(local.useDefsSize() != 0 || local.isParameter());


        Vector localsOfType = (Vector)usedLocals.get(local.type());
        if (localsOfType == null) {
            localsOfType = new Vector(2);
            localsOfType.addElement(local);
            usedLocals.put(local.type(), localsOfType);
        }
        else {
            if (!localsOfType.contains(local)) {
                localsOfType.addElement(local);
            }
        }
    }

    /**
     * Allocate the new locals for the transformed graph based on the set of locals
     * that were marked as 'used' during the transformations.
     * @param optimize If true, this method optimizes the
     * allocation of the locals to ensure maximum reuse.
     * @return the locals allocated for the method.
     */
    private Local[] allocateLocals(boolean optimize, Tracer tracer) {
        if (usedLocals.isEmpty()) {
            return new Local[0];
        }

        int twoWordCount = 0;
        SortableVector allocatedLocals = new SortableVector(usedLocals.size() * 2);

        for (Enumeration e = usedLocals.elements(); e.hasMoreElements(); ) {
            Vector localsOfType = (Vector)e.nextElement();
            Assert.that(localsOfType.size() > 0);
            for (Enumeration l = localsOfType.elements(); l.hasMoreElements();) {
                Local local = (Local)l.nextElement();
                allocatedLocals.addElement(local);
                if (local.type().isTwoWords()) {
                    twoWordCount += 1;
                }
            }
        }

        // Optimize
        if (optimize) {
            allocatedLocals = optimizeLocals(allocatedLocals, tracer);
        }

        // Assign offsets to locals.
        int count = assignOffsets(allocatedLocals);

        // Copy locals into an array
        Local[] locals = new Local[count];
        int offset = 0;
        for (Enumeration e = allocatedLocals.elements(); e.hasMoreElements(); ) {
            Local local = (Local)e.nextElement();
            Assert.that(local.squawkIndex() == offset);
            locals[offset++] = local;
            if (local.type().isTwoWords()) {
                Assert.that(local.secondWordLocal().squawkIndex() == offset);
                locals[offset++] = local.secondWordLocal();
            }
        }
        return locals;
    }

    /**
     * Optimze the locals allocated for a method by merging locals of the
     * same type that have mutually exclusive liveness ranges.
     * @param locals
     * @return
     */
    private SortableVector optimizeLocals(SortableVector locals, Tracer tracer) {

        // Assign offsets to squawk locals.
        int count = assignOffsets(locals);

        // Do liveness analysis in terms of squawk locals.
        try {
            for (Instruction inst = ilist.head(); inst != null; inst = inst.getNext()) {
                if (inst instanceof BasicBlockEntryDelimiter) {
                    ((BasicBlockEntryDelimiter)inst).clearDataFlowData();
                } else if (inst instanceof BasicBlockExitDelimiter) {
                    ((BasicBlockExitDelimiter)inst).clearDataFlowData();
                }
            }
            LivenessDFA.analyse(method, ilist, count, 0, null, false, false, null);
        } catch (LinkageException le) {
            le.printStackTrace();
            Assert.shouldNotReachHere();
        }

        // Trace
        if (tracer != null) {
            tracer.traceln("");
            tracer.traceln("\n++IR1 trace for "+method+" prior to optimization:");
            ilist.trace(tracer, method.vm().loader(), method.parent().getSourceFilePath(), false, false);
            tracer.traceln("");

            tracer.traceln("\nLocals allocated in "+method+" prior to optimization:");
            int index = 0;
            for (Enumeration e = locals.elements(); e.hasMoreElements();) {
                Local local = (Local)e.nextElement();
                tracer.trace((index++)+":\t"+local);
                if (local.isParameter()) {
                    tracer.trace("\t param="+local.getParameterNumber());
                }
                if (!local.type().isSecondWordType()) {
                    tracer.trace("  liveness: "+Range.toString(local.getLivenessRange(ilist.head())));
                }
                tracer.traceln("");
            }
        }

        // Merge locals based on liveness ranges
        SortableVector mergedLocals  = new SortableVector(locals.size());
        for (Enumeration l = locals.elements(); l.hasMoreElements();) {
            Local local = (Local)l.nextElement();

            // Try to merge with another local of the same type
            int index = 0;
            for (Enumeration e = mergedLocals.elements(); e.hasMoreElements(); index++) {
                Local merged = (Local)e.nextElement();
                if (merged.type() == local.type()) {
                    Range r1 = merged.getLivenessRange(ilist.head());
                    Range r2 = local.getLivenessRange(ilist.head());
                    if (!Local.areTwoDifferentParameters(local, merged) && !Range.intersects(r1, r2)) {

                        // Swap mergee and merger to ensure that a non-temporary
                        // local is the result of the merge if possible
                        if (merged instanceof TemporaryLocal && !(local instanceof TemporaryLocal)) {
                            Local tmp  = merged;
                            merged     = local;
                            local      = tmp;
                            Range rtmp = r2;
                            r2         = r1;
                            r1         = rtmp;
                        }

                        if (tracer != null) {
                            tracer.traceln("merging  "+local+ "  "+Range.toString(r2));
                            tracer.traceln("   into  "+merged+"  "+Range.toString(r1));
                        }

                        // Merge the ranges and then the locals themselves
                        Range mergedRange = Range.merge(r1, r2);
                        merged.mergeWith(local, mergedRange);

                        // Override existing merged local in case it was a temporary local
                        mergedLocals.setElementAt(merged, index);

                        local = null;
                        if (tracer != null) {
                            tracer.traceln(" result  "+merged+"  "+Range.toString(mergedRange));
                        }

                        // Stop looking for other variables to merge with
                        break;
                    }
                }
            }

            // Now add the current local to the merged locals if it was not
            // merged itself.
            if (local != null) {
                mergedLocals.addElement(local);
            }
        }

        return mergedLocals;
    }

    /**
     * Assign physical offsets to a given Vector of logical locals.
     * @param locals The list of logical locals.
     * @return the number of physical local variables.
     */
    private int assignOffsets(SortableVector locals) {

        locals.sort(Local.OFFSET_ALLOCATION_COMPARER);

        int offset = 0;
        for (Enumeration e = locals.elements(); e.hasMoreElements();) {
            Local local = (Local)e.nextElement();
            local.setSquawkIndex(offset);
            offset++;
            if (local.type().isTwoWords()) {
                local.secondWordLocal().setSquawkIndex(offset);
                offset++;
            }
        }
        return offset;
    }

    /**
     * Remove the locals that are no longer used after all transformations have
     * been performed.
     * @param locals
     * @return
     */
    private Local[] removedUnusedLocals(Local[] locals) {
        int usedCount = 0;
        for (int i = 0; i != locals.length; i++) {
            Local local = locals[i];
            if (local.isParameter() || local.type().isSecondWordType() || local.useDefsSize() != 0) {
                local.setSquawkIndex(usedCount);
                locals[usedCount++] = local;
            }
        }
        if (usedCount != locals.length) {
            locals = (Local[])Arrays.copy(locals, 0, new Local[usedCount], 0, usedCount);
        }
        return locals;
    }

    /**
     * Attempt to transform a single parameter of a given instruction.
     * @param inst The instruction to which the parameter belongs.
     * @param parm The parameter to transform.
     * @param parmResult The temporary local variable holding the result (if any) of parm.
     * @return the (possibly new) parameter after optimization.
     *
     * @pre inst != null
     * @pre parm != null
     * @post $result != null
     */
    public Instruction doParameter(Instruction inst, Instruction parm) {
        /*
         * Keep track of the number of active exception handlers.
         * This information is useful to when determining if it is possible
         * to move stores to local variables before instructions that
         * can trap. Its not perfect because no check is made about the
         * handlers exception type, but it is not too bad a heuristic.
         */
        if (inst instanceof HandlerEnter) {
            activeHandlers++;
        } else if (inst instanceof HandlerExit) {
            activeHandlers--;
            Assert.that(activeHandlers >= 0);
        }

        Assert.that(parm != null);
        TemporaryLocal parmResult = parm.getTemporaryLocal();
        if (oneOrMoreParmsFilled || !connectsToWithoutSpilling(parm, inst)) {
            Assert.that(parmResult != null);
            boolean spillsResult = true;  // Flags whether or not to spill the result of this parameter to a local variable

            Assert.that(inst.getPrev() != parm || parm.wasDuped());
            oneOrMoreParmsFilled = true;
            if (parmResult.referenceCount() >= 0) {
                if (parm instanceof LoadLocal) {
                    /*
                     * Loads of local variables may be hoisted if:
                     *
                     *   1, there is only straight line code between the load
                     *      and its use and,
                     *   2, there are no stores to the local variable between
                     *      the load and its use and,
                     *   3, the loaded value was not duped or merged
                     */
                    Assert.that(parmResult != null);

                    // Get a handle to the local variable loaded by the load instruction
                    Local local = ((LoadLocal)parm).local();
                    if (!parm.wasDuped() && !parm.wasMerged() && connectsWithoutStoringTo(parm, inst, local)) {

                        // Hoist the load up to the current insertion point.
                        parm.cancelSpilling();
                        ilist.remove(parm);
                        ilist.insertBefore(parm, inst);

                        // There's no need to spill the result of this parameter any more
                        spillsResult = false;
                    }
                }
                else if (parm instanceof LoadConstant || parm instanceof LoadConstantObject) {
                    /*
                     * Loads of constants may be hoisted if:
                     *
                     *   1, there is only straight line code between the
                     *      constant load and its use and,
                     *   3, the loaded value was not duped or merged
                     */
                    if (!parm.wasDuped() && !parm.wasMerged() && isStraightLineCode(parm, inst)) {
                        // Hoist the load up to the current insertion point.
                        parm.cancelSpilling();
                        ilist.remove(parm);
                        ilist.insertBefore(parm, inst);

                        // There's no need to spill the result of this parameter any more
                        spillsResult = false;
                    }
                }
            }

            if (spillsResult) {
                // The current parameter's temporary local will become a real local
                // so refine its type now if it is currently 'null'. This refinement
                // takes into account how the current instruction uses the temporary local.
                if (parmResult.type() == method.vm().NULLOBJECT) {
                    if (inst instanceof StoreLocal) {
                        // The current instruction is a store local, so change
                        // the type of the temporary local to be the same type as
                        // the local being stored to.
                        parmResult.changeType(((StoreLocal)inst).local().type());
                    } else if (inst instanceof LoadField) {
                        // The current instruction is a load field, so change
                        // the type of the temporary local to be the same type as
                        // the object containing the field.
                        parmResult.changeType(((LoadField)inst).field().parent());
                    } else if (inst instanceof StoreField) {
                        // The current instruction is a store field, so change
                        // the type of the temporary local to be the same type as
                        // the object containing the field if this is the 'ref'
                        // parameter otherwise change it to be the type of the
                        // value being stored.
                        StoreField st = (StoreField)inst;
                        if (parm == st.ref()) {
                            parmResult.changeType(st.ref().type());
                        } else {
                            parmResult.changeType(st.value().type());
                        }
                    } else {
                        // By default, just set the temporary local type
                        // to be Object.
                        parmResult.changeType(method.vm().OBJECT);
                    }
                } else if (parmResult.type() == method.vm().INITOBJECT) {
                    parmResult.changeType(method.parent());
                }

                // Flag the parm for spilling
                parm.spills();

                // Load the spilt result
                Instruction ld = ifactory.createLoadLocal(parmResult, parm.type());
                ld.setOriginal(inst);
                ld.setTemporaryLocal(parmResult);
                ilist.insertBefore(ld, inst);

                // The load becomes the replacement parm
                parm = ld;
            }
        }

        return parm;
    }

   /**
    * Return true if a given local variable is not stored to between
    * two points of code and there is no jump into or out of the two
    * points of code.
    *
    * @param from
    * @param to
    * @param local
    * @return
    */
   private boolean connectsWithoutStoringTo(Instruction from, Instruction to, Local local) {
        Instruction inst = from;
        while (inst != to) {
            if (inst instanceof Phi) {
                Instruction source = ((Phi)inst).target().getInstruction();
                Assert.that(source != null);
                if (source.getOriginalIP() < from.getOriginalIP() ||
                    source.getOriginalIP() > to.getOriginalIP())
                {
                    // I have forgotten how this case could ever occur and so
                    // want to see an example...
                    Assert.shouldNotReachHere();
                    return false;
                }
            }
            if (inst instanceof LocalVariableInstruction) {
                LocalVariableInstruction lvi = (LocalVariableInstruction)inst;
                if (lvi.local() == local && lvi.isDefinition()) {
                    return false;
                }
            }
            Assert.that(inst.getNext() != null);
            inst = inst.getNext();
        }
        return true;
    }

    /**
     * Determine if a sequence of code between two instructions
     * has no control flow entry point apart from the start of the
     * sequence and no exit point apart from the end of the sequence.
     *
     * @param from
     * @param to
     * @return
     */
    private boolean isStraightLineCode(Instruction from, Instruction to) {
        while (from != to) {
            if (from instanceof Phi || from instanceof BasicBlockExitDelimiter) {
                return false;
            }
            Assert.that(from.getNext() != null);
            from = from.getNext();
        }
        return true;
    }

    /**
     * Test a sequence of code between two instructions, the first of which
     * pushes a value onto the stack that is consumed by the second.
     *
     * @param from The stack value producer.
     * @param to The stack value consumer.
     * @return true if there is no need for the stack value to be spilled
     * by the producer and filled by the consumer.
     */
    private boolean connectsToWithoutSpilling(Instruction from, Instruction to) {
        Assert.that(from != to);
        // A 'new' and 'invokeinit' pair separated only by one or more 'dup's
        // is special in that the duping that was originally applied to the
        // result of the 'new' is now applied to the return value of the 'invokeinit'.
        if (from.getNext() == to && from instanceof NewObject && to instanceof Invoke) {
            Invoke invoke = (Invoke)to;
            if (!from.wasDuped() && invoke.form() == Invoke.Form.INIT) {
                if (from.needsSpilling()) {
                    from.cancelSpilling();
                }
                return true;
            }
        }

        while (from != to) {
            if (from.wasDuped() ||
                from.wasMerged() ||
                from.needsSpilling() ||
                from instanceof BasicBlockExitDelimiter)
            {
                return false;
            }
            Assert.that(from.getNext() != null);
            from = from.getNext();
        }
        return true;
    }

}
