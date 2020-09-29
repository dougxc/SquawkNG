package com.sun.squawk.translator.ir;

import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;
import com.sun.squawk.translator.loader.LinkageException;
import com.sun.squawk.translator.loader.StackMap;
import com.sun.squawk.translator.loader.TypeProxy;

/**
 * Local variables in Squawk are not reused across different types.
 */
public class Local {

    private static final String WORD2_SUFFIX = "<2>";

    /** The type of this local variable. */
    private Type type;
    /** The parameter this local variable corresponds to (if any). */
    private int parameterNumber = -1;
    /** The LocalVariableInstructions that define and use this variable. */
    private SortableVector useDefs;
    /** Unique ID (within a method). */
    private final int id;
    /** Offset of variable in local variable table. */
    private int squawkIndex = -1;
    /** The local storing the second word of a double word local variable. */
    private final Local secondWordLocal;
    /** The original index assigned to the local by javac. */
    private final int javacIndex;
    /** The name of the local variable specified by a LocalVariableTable attribute (if any). */
    private String name;
    /** The variable this was merged into (if it was merged). */
    private Local mergeParent;
    /** Flags whether or not this variable was merged into (i.e. some other Local has this
        variable as its 'mergeParent'. */
    private boolean isMergeParent;
    /** Flags whether or not this variable is initialized to its default value upon entry
        to the method. This will only be set if the first store to this instruction was an
        initialization to the default value and that store was optimized away by the GraphTransformer. */
    private boolean autoInitialized;

    private Range liveness;

    /**
     * This should only be called from the TemporaryLocal constructor.
     * @param type
     */
    protected Local(Type type) {
        Assert.that(type == type.localType());
        this.type            = type;
        this.id              = type.vm().nextLocalVariableID();
        this.javacIndex      = -1;
        this.secondWordLocal = createSecondWordLocal();
    }

    /**
     * Construct a Local variable corresponding to a local allocated by javac.
     * @param type The inferred type of the variable (which may change).
     * @param javacIndex The index assigned to this variable by javac.
     * @param name The source level name of the variable as determined by a LocalVariableTable
     * attribute or null if no such attribute was present.
     */
    public Local(Type type, int javacIndex, String name) {
        Assert.that(!(this instanceof TemporaryLocal));
        Assert.that(type == type.localType());
        Assert.that(javacIndex != -1);

        this.type            = type;
        this.javacIndex      = javacIndex;
        this.name            = name;
        this.id              = type.vm().nextLocalVariableID();
        this.secondWordLocal = createSecondWordLocal();
    }


    public Type type()                    { return type;                  }
    public void setParameterNumber(int n) { parameterNumber = n;          }
    public int getParameterNumber()       { return parameterNumber;       }
    public boolean isParameter()          { return parameterNumber != -1; }
    public String name()                  { return name;                  }
    public int javacIndex()               { return javacIndex;            }
    public void setSquawkIndex(int n)     { squawkIndex = n;              }
    public int squawkIndex()              { return squawkIndex;           }
    public int id()                       { return id;                    }
    public void setAutoInitialized()      { autoInitialized = true;       }
    public boolean isAutoInitialized()    { return autoInitialized;       }

    private Local createSecondWordLocal() {
        if (type.isTwoWords()) {
            String name = this.name;
            if (name != null) {
                name += WORD2_SUFFIX;
            }
            return new Local(type.secondWordType(), javacIndex+1, name);
        }
        return null;
    }

    /**
     * Change the type of this local variable.
     * @param type the new type of this local variable.
     */
    public final void changeType(Type type) {
        Assert.that(mergeParent == null);
        Assert.that(type != null);
        Assert.that(type.isTwoWords() == this.type.isTwoWords());
        Assert.that(!type.isTwoWords() || secondWordLocal != null);
        this.type = type;
    }

    /**
     * Narrow the inferred type of a local variable. If the given type is
     * narrower that the existing type of the local variable, the local
     * variable's type is updated to be the inferred type.
     * @param parm An instruction that may load the value of a local variable onto the stack.
     * @param inferred The inferred type.
     * @param interfaceTypeNarrowsConcreteType If true, then allow an interface type to
     * narrow a concrete type where the concrete type does not implement the given
     * interface type. In theory, this should always be allowable as narrowing is
     * done after verification. However, narrowing to an interface type in this
     * manner may conflict with subsequent stack maps that have a more broad
     * type for a local variable even though the local variable is really dead
     * at the point of the stack map. As such, this type of narrowing can only
     * be done once the stack maps that have been fixed up after
     * liveness analysis.
     * @throws LinkageException
     */
    public static void narrowLocalType(Instruction parm, Type inferred, boolean interfaceTypeNarrowsConcreteType) throws LinkageException {
        inferred = inferred.localType();
        Local local;
        if (parm instanceof LoadLocal) {
            LoadLocal ld = (LoadLocal)parm;
            local = ld.local();
        }
        else if (parm instanceof LoadConstantNull) {
            local = parm.getTemporaryLocal();

            LoadConstantNull ldnull = (LoadConstantNull)parm;
            Type castType = ldnull.castType();
            if (castType != null && inferred != inferred.vm().NULLOBJECT && inferred.isNarrowerThan(castType, interfaceTypeNarrowsConcreteType)) {
                ldnull.setCastType(inferred);
            }

            if (local.type() != inferred.vm().NULLOBJECT) {
                return;
            }
        }
        else {
            return;
        }
        Type type = local.type();

        Assert.that(type == inferred || type.vIsAssignableTo(inferred) || inferred.vIsAssignableTo(type));

        if (inferred.isNarrowerThan(type, interfaceTypeNarrowsConcreteType)) {
            local.changeType(inferred);
            parm.changeType(inferred);
        }
    }

    /**
     * Get the local corresponding to the second word of this double-word local
     * variable. This must be the first word of a double word variable.
     * @return
     */
    public Local secondWordLocal() {
        Assert.that(type.isTwoWords() && secondWordLocal != null);
        return secondWordLocal;
    }

    public boolean isSameParameter(Local local) {
        return isParameter() && local.isParameter() && parameterNumber == local.parameterNumber;
    }

   /* ------------------------------------------------------------------------ *\
    *                           Use/def accessor methods                       *
   \* ------------------------------------------------------------------------ */

    /**
     * Add an instruction that defines this local variable to the use/defs of the variable.
     * @param definition an instruction that defines this local variable.
     */
    public void addDefinition(LocalVariableInstruction definition) {
        Assert.that(mergeParent == null);
        Assert.that(definition.isDefinition());
        Assert.that(useDefs == null || !useDefs.contains(definition));
        Assert.that(definition.local() == this);
        if (useDefs == null) {
            useDefs = new SortableVector();
        }
        useDefs.addElement(definition);
    }

    /**
     * Add an instruction that uses this local variable to the use/defs of the variable.
     * @param use an instruction that uses this local variable.
     */
    public void addUse(LocalVariableInstruction use) {
        Assert.that(mergeParent == null);
        Assert.that(use.isUse());
        Assert.that(use.local() == this);
        if (useDefs == null) {
            useDefs = new SortableVector();
            useDefs.addElement(use);
        }
        else if (!useDefs.contains(use)) {
            useDefs.addElement(use);
        }
    }

    public void removeDefinition(LocalVariableInstruction def) {
        Assert.that(mergeParent == null);
        Assert.that(def.isDefinition());
        Assert.that(def.local() == this);
        Assert.that(useDefs != null);
        boolean existed = useDefs.removeElement(def);
        Assert.that(existed);
    }

    public void removeUse(LocalVariableInstruction use) {
        Assert.that(mergeParent == null);
        Assert.that(use.isUse());
        Assert.that(use.local() == this);
        Assert.that(useDefs != null);
        boolean existed = useDefs.removeElement(use);
        Assert.that(existed);
    }

    public final int useDefsSize() {
        return useDefs == null ? 0 : useDefs.size();
    }

    public final LocalVariableInstruction getLastUse() {
        if (useDefs != null) {
            for (int i = useDefs.size() - 1; i >= 0; i--) {
                LocalVariableInstruction lvi = (LocalVariableInstruction)useDefs.elementAt(i);
                if (lvi.isUse()) {
                    return lvi;
                }
            }
        }
        return null;
    }

   /* ------------------------------------------------------------------------ *\
    *                       Identity merging methods                           *
   \* ------------------------------------------------------------------------ */

    /**
     * If this variable was merged into another variable, then return that variable.
     * @return the varibale into which this was merged or null if this variable was
     * not merged into another.
     */
    public Local mergeParent() {
        return mergeParent;
    }

    /**
     * Determine whether or not this variable was merged into (i.e. some other
     * Local has this variable as its 'mergeParent'.
     * @return
     */
    public boolean isMergeParent() {
        return isMergeParent;
    }

    /**
     * Get the end merge result of this variable which may be itself if it was
     * not merged into another variable.
     * @return the end merge result of this variable
     */
    public Local mergeRoot() {
        Local l = this;
        while (l.mergeParent != null) {
            l = l.mergeParent;
        }
        return l;
    }

    private void mergeName(Local other) {
        if (name == null) {
            name = other.name;
        }
        else {
            if (other.name != null) {
               StringTokenizer st = new StringTokenizer(name,"+");
               boolean found = false;
               while (st.hasMoreTokens()) {
                   if (st.nextToken().equals(other.name)) {
                       found = true;
                       break;
                   }
               }
               if (!found) {
                   name += "+"+other.name;
               }
            }
        }
    }

    /**
     * Merge a given local variable into this one.
     * @param other
     * @param mergingAtBranchPoint
     * @return
     */
    private Local mergeWith(Local other, boolean mergingAtBranchPoint) {
//System.err.println("merging "+other+" into "+this);
        Assert.that(this != other);
        Assert.that(mergeParent == null);
        Assert.that(other.mergeParent == null);
        Assert.that(type == other.type);

        // Merge the use/defs
        if (useDefs == null) {
            if (other.useDefs != null) {
                useDefs = other.useDefs;
                for (Enumeration e = useDefs.elements(); e.hasMoreElements();) {
                    LocalVariableInstruction lvi = (LocalVariableInstruction)e.nextElement();
                    lvi.updateLocal(this, false);
                    Assert.that(lvi.local() == this);
                }
            }
        }
        else if (other.useDefs != null) {
            for (Enumeration e = other.useDefs.elements(); e.hasMoreElements();) {
                LocalVariableInstruction lvi = (LocalVariableInstruction)e.nextElement();
                lvi.updateLocal(this, false);
                useDefs.addElement(lvi);
                Assert.that(lvi.local() == this);
            }
        }

        // Merge parameter number.
        if (mergingAtBranchPoint) {
            if (parameterNumber == -1) {
                parameterNumber = other.parameterNumber;
            }
            else {
                Assert.that(other.parameterNumber == -1 || other.parameterNumber == parameterNumber);
            }
        }

        // Merge name
        mergeName(other);

        // Update the mergee to reflect the result of the merge
        other.mergeParent = this;
        other.useDefs = null;

        if (!mergingAtBranchPoint) {
            this.isMergeParent = true;
        }

//System.err.println("merge result: "+this);
        return this;
    }

    /**
     * Merge two given variables.
     * @param local1
     * @param local2
     * @param mergePoint
     * @return
     */
    public static Local merge(Local local1, Local local2) {
        if (local1 == local2) {
            return local1;
        }

        Assert.that(local1.javacIndex == local2.javacIndex);

        if (local2.mergeParent != null) {
            local2 = local2.mergeRoot();
            return (local2 == local1 ? local1 : local1.mergeWith(local2, true));
        }
        else if (local1.mergeParent != null) {
            local1 = local1.mergeRoot();
            return (local1 == local2 ? local2 : local2.mergeWith(local1, true));
        }

        if (local1.useDefsSize() > local2.useDefsSize()) {
            return local1.mergeWith(local2, true);
        }
        else {
            return local2.mergeWith(local1, true);
        }
    }

    /**
     * Return a string representation of this local variable.
     */
    public String toString() {
        int id = (squawkIndex == -1 ? this.id : squawkIndex);
        String asString;
        if (name == null) {
            if (type.isSecondWordType()) {
                asString = toString("loc"+id+WORD2_SUFFIX);
            }
            else {
                asString = toString("loc"+id);
            }
        }
        else {
            asString = toString(name)/* + " id="+id*/;
        }
        return asString;
    }

    /**
     * Return a string representation of this local variable.
     * @param name The name of the local variable.
     */
    protected String toString(String name) {
        return name+":"+type.toSignature(false, true)+"{javac:"+javacIndex+"}";
    }

   /* ------------------------------------------------------------------------ *\
    *                     Liveness based merging                               *
   \* ------------------------------------------------------------------------ */

    public void mergeWith(Local other, Range liveness) {
        Assert.that(!areTwoDifferentParameters(this, other));
        mergeWith(other, false);
        this.liveness = liveness;
        other.liveness = null;
    }

    /**
     * Test whether two given locals correspond to two unique parameters. This will
     * return false if either local does not correspond to a parameter.
     * @return
     */
    public static boolean areTwoDifferentParameters(Local l1, Local l2) {
        return l1.isParameter() && l2.isParameter() && l1.parameterNumber != l2.parameterNumber;
    }

    private Instruction findFirstDef(SortableVector useDefs) {
        Instruction first = (Instruction)useDefs.firstElement();
        if (first instanceof StoreLocal) {
            return first;
        }
        Assert.that(first instanceof LoadLocal || first instanceof IncDecLocal);
        Instruction inst = first.getPrev();
        while (!(inst instanceof Phi)) {
            inst = inst.getPrev();
        }
        Type[] locals = ((Phi)inst).target().getStackMapEntry().locals;
        Assert.that(this instanceof TemporaryLocal || (locals.length > javacIndex && locals[javacIndex] != locals[javacIndex].vm().BOGUS));
        return inst;
    }

    /**
     * Create the liveness ranges for this temporary local. The method assumes that
     * useDefs is sorted by address.
     * @param useDefs The use/defs of the local.
     * @param head The first instruction of the method
     * @return the head element of the range list
     */
    private Range createLiveness(SortableVector useDefs, Instruction head) {
        Assert.that(!useDefs.isEmpty());
        Assert.that(squawkIndex >= 0);

        Range range;
        Range liveness;
        Instruction lastDef = null; // Last definition without a corresponding use
        Instruction tail = (Instruction)useDefs.lastElement();
        if (isParameter() || isAutoInitialized()) {
            liveness = range = Range.create(0, null);
            lastDef = head;
        } else {
            head = lastDef = findFirstDef(useDefs);
            if (head != tail) {
                 head = head.getNext();
            }
            liveness = range = Range.create(lastDef.getRelocIP(), null);
        }
        if (head == tail) {
            range.setEnd(tail.getRelocIP());
            return liveness;
        }

        // Iterate through the instructions to determine liveness
        Instruction inst = head;
        Assert.that(inst != null);
        while (inst != null) {
            if (inst instanceof LocalVariableInstruction) {
                LocalVariableInstruction lvi = (LocalVariableInstruction)inst;
                if (lvi.local() == this) {
                    int ip = inst.getRelocIP();
                    if (lvi.isUse()) {
                        Assert.that(range != null);
                        range.setEnd(ip);
                        lastDef = null;
                    }

                    if (lvi.isDefinition()) {
                        Assert.that(lvi instanceof StoreLocal || lvi instanceof IncDecLocal);
                        if (lastDef != null) {
                            Assert.that(range != null);
                            if (lastDef instanceof Phi) {
                                range.setEnd(ip-1);
                            }
                            else {
                                range.setEnd(lastDef.getRelocIP());
                            }
                        }
                        range = Range.create(ip, range);
                        lastDef = lvi;
                    }
                }
            } else if (inst instanceof BasicBlockExitDelimiter || inst instanceof Trappable) {
                int ip = inst.getRelocIP();
                BitSet liveOut;
                BitSet successorliveIn;
                boolean fallsThrough;
                if (inst instanceof BasicBlockExitDelimiter) {
                    BasicBlockExitDelimiter delim = (BasicBlockExitDelimiter)inst;
                    liveOut      = delim.getBBLiveOut();
                    fallsThrough = delim.fallsThrough();
                    if (fallsThrough) {
                        successorliveIn = delim.getFallThroughBBLiveIn();
                    } else {
                        successorliveIn = null;
                    }
                } else {
                    Trappable trap = (Trappable)inst;
                    liveOut        = trap.getBBLiveOut();
                    fallsThrough   = true;
                    if (liveOut == null) {
                        // The fact that there is no liveness set for this trappable
                        // instruction implies that it is not inside an active
                        // handler and therefore is not a basic block boundary
                        inst = inst.getNext();
                        continue;
                    }
                    successorliveIn = trap.getSuccessorBBLiveIn();
                }

                boolean alive = (liveOut != null && liveOut.get(squawkIndex));
                if (alive) {
                    // Handle fall through control flow
                    boolean kill = true;
                    if (fallsThrough) {
                        // Don't kill the range if the variable is live on entry to the
                        // fall through instruction
                        Instruction next = inst.getNext();
                        Assert.that(successorliveIn != null);
                        if (!(next instanceof Phi) && successorliveIn.get(squawkIndex)) {
                            kill = false;
                        }
                    }
                    if (kill) {
                        range.setEnd(ip);
                        lastDef = null;
                    }
                }
                else {
                    if (lastDef != null) {
                        range.setEnd(lastDef.getRelocIP());
                        lastDef = null;
                    }
                }
            } else if (inst instanceof BasicBlockEntryDelimiter) {
                int ip = inst.getRelocIP();
                BasicBlockEntryDelimiter delim = (BasicBlockEntryDelimiter)inst;
                BitSet liveIn = delim.getBBLiveIn();
                boolean alive = (liveIn != null && delim.getBBLiveIn().get(squawkIndex));

                // Handle fall through from preceeding non-control transfer instruction
                Instruction prev = inst.getPrev();
                if (delim instanceof Phi && !(prev instanceof BasicBlockExitDelimiter)) {
                    int prevIp = (prev == null ? 0 : ip - 1);
                    if (alive) {
                        range.setEnd(prevIp);
                        lastDef = null;
                    }
                    if (lastDef != null) {
                        if (lastDef instanceof Phi) {
                            range.setEnd(prevIp);
                        }
                        else {
                            range.setEnd(lastDef.getRelocIP());
                        }
                        lastDef = null;
                    }
                }
                Assert.that(lastDef == null);

                if (alive) {
                    range = Range.create(ip, range);
                    lastDef = inst;
                }
            }

            inst = inst.getNext();
        }

        // Close any pending open definition
        if (lastDef != null) {
            Assert.that(range != null);
            range.setEnd(lastDef.getRelocIP());
            lastDef = null;
        }
        return liveness;
    }

    /**
     * Get the liveness range for this local. This should only be called after the
     * transformations for the enclosing method having been completed and this
     * variable has been determined to be a used local with in the method.
     * @param head The first instruction in the method. This will be used for
     * creating the liveness range if it hasn't already been created. If this
     * value is null, no attempt is made to create the liveness range.
     * @return the liveness range for the local.
     */
    public Range getLivenessRange(Instruction head) {
        if (mergeParent != null) {
            return null;
        }
        if (useDefs == null || useDefs.isEmpty()) {
            Assert.that(isParameter());
            return null;
        }
        if (liveness == null && head != null) {
            useDefs.sort(new Comparer() {
                public int compare(Object o1, Object o2) {
                    if (o1 == o2) {
                        return 0;
                    }
                    int ip1 = ((LocalVariableInstruction)o1).getRelocIP();
                    int ip2 = ((LocalVariableInstruction)o2).getRelocIP();
                    if (ip1 < ip2) {
                        return -1;
                    }
                    else {
                        Assert.that(ip1 > ip2);
                return 1;
                    }
                }
            });
            liveness = createLiveness(useDefs, head);
        }
        Assert.that(liveness != null);
        return liveness;
    }

    /**
     * This is a comparer that will sort an array of locals first according to
     * parameter number (lower parameter number first) and then by frequency of use/defs.
     */
    public static final Comparer OFFSET_ALLOCATION_COMPARER = new Comparer() {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) {
                return 0;
            }
            Local l1 = (Local)o1;
            Local l2 = (Local)o2;

            if (l1.isParameter()) {
                if (!l2.isParameter()) {
                    return -1;
                }
                return l1.parameterNumber - l2.parameterNumber;
            }
            else {
                if (l2.isParameter()) {
                    return 1;
                }
            }
            int result = l2.useDefsSize() - l1.useDefsSize();
            if (result == 0) {
                return l1.toString().compareTo(l2.toString());
            } else {
                return result;
            }
        }
    };


}
