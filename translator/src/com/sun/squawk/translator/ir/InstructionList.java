package com.sun.squawk.translator.ir;

import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;
import com.sun.squawk.translator.loader.*;
import com.sun.squawk.translator.util.BitSet;
import com.sun.squawk.translator.Assert;
import com.sun.squawk.translator.util.Tracer;
import com.sun.squawk.translator.util.BufferedReader;
import com.sun.squawk.translator.util.JVMConst;

/**
 * This represents the instructions and local variables used by those instructions
 * in the intermediate form of a method.
 */
public class InstructionList {

    /** Local variables used by the instructions. */
    private Local[] locals;
    /** Pointer to the first instruction. */
    private Instruction head;
    /** Pointer to the last instruction. */
    private Instruction tail;
    /** Optional frame used to model the effect of the instructions. */
    private ExecutionFrame frame;
    /** The exception handlers of the method in terms of the IR instructions. */
    private ExceptionHandlerTable handlerTable;
    /** Used to assign logical IPs to instructions when synthesizing a method. */
    private int logicalIp;

    public InstructionList(ExecutionFrame frame, boolean synthesizing) {
        this.frame = frame;
        if (synthesizing) {
            Assert.that(frame != null);
            logicalIp = 0;
        }
        else {
            logicalIp = -1;
        }
    }

/*---------------------------------------------------------------------------*\
 *                 List manipulation methods                                 *
\*---------------------------------------------------------------------------*/

    /**
     * Get the logical IP the next instruction to be appended will receive.
     * @return
     */
    public int getLogicalIP() {
        Assert.that(logicalIp != -1);
        return logicalIp;
    }

    /**
     * Append an instruction to the instruction list.
     * @param inst The instruction to append.
     */
    public void append(Instruction inst) throws LinkageException {
        // Mark instructions for spilling if necessary
        if (frame != null) {
            if (inst.constrainsStack() && !(inst instanceof BasicBlockExitDelimiter)) {
                if (frame.stackSize(false) > 0) {
                    frame.spillStack(false, inst);
                }

            }
        }

        // Give the instruction a logical address if it doesn't already have a physical address
        if (logicalIp != -1) {
            Assert.that(inst.getOriginalIP() == -1);
            inst.setContext(logicalIp++, -1); // line number etc.
        }

        if (head == null) {
            head = inst;
        }
        else {
            tail.insertAfter(inst);
        }
        tail = inst;
    }

    /**
     * Append an instruction and then push it to the frame if we have one.
     * @param inst
     */
    public void appendPush(Instruction inst) throws LinkageException {
        append(inst);
        if (frame != null) {
            frame.push(inst, true);
        }
    }

    /**
     * Insert 'before' into this list of instructions before 'after'.
     * @param after
     * @param before
     */
    public void insertBefore(Instruction before, Instruction after) {
        if (after.getPrev() == null) {
            Assert.that(head == after);
            head = before;
        }
        after.insertBefore(before);
        Assert.that(head != before || before.getPrev() == null);
    }

    /**
     * Insert 'after' into this list of instructions after 'before'.
     * @param after
     * @param before
     */
    public void insertAfter(Instruction after, Instruction before) {
        if (before.getNext() == null) {
            Assert.that(before == tail);
            tail = after;
        }
        before.insertAfter(after);
        Assert.that(tail != after || after.getNext() == null);
    }

    /**
     * Remove 'inst' from the list.
     * @param inst
     */
    public void remove(Instruction inst) {
        if (inst.getPrev() == null) {
            Assert.that(inst == head);
            head = inst.getNext();
        }
        if (inst.getNext() == null) {
            Assert.that(tail == inst);
            tail = inst.getPrev();
        }
        inst.removeFromList();
    }

    /**
     * Return the first instruction.
     * @return the first instruction.
     */
    public Instruction head() {
        return head;
    }

    /**
     * Return the last instruction.
     * @return the last instruction.
     */
    public Instruction tail() {
        return tail;
    }

/*---------------------------------------------------------------------------*\
 *                  Local variables manipulation methods                     *
\*---------------------------------------------------------------------------*/

    /**
     * Set the locals allocated for the instructions in this list.
     * @param locals
     */
    public void setLocals(Local[] locals) {
        Assert.that(this.locals == null);
        this.locals = locals;
    }

    public Local[] getLocals() {
        Assert.that(locals != null);
        return locals;
    }

    public boolean includes(Local local) {
        Assert.that(locals != null);
        for (int i = 0; i != locals.length; i++) {
            if (locals[i] == local) {
                return true;
            }
        }
        return false;
    }
/*---------------------------------------------------------------------------*\
 *                  Exception handler table methods                          *
\*---------------------------------------------------------------------------*/

    public void setHandlerTable(ExceptionHandlerTable handlerTable) {
        Assert.that(this.handlerTable == null);
        this.handlerTable = handlerTable;
    }

    public ExceptionHandlerTable getHandlerTable() {
        return handlerTable;
    }

    public HandlerEnter getSyntheticHandlerEntryForSynchronizedMethod(boolean isStatic) {
        Instruction inst = head;
        if (!isStatic) {
            Assert.that(inst instanceof LoadLocal);
            inst = inst.getNext();
        }
        Assert.that(inst instanceof MonitorEnter);
        inst = inst.getNext();
        Assert.that(inst instanceof HandlerEnter);
        return (HandlerEnter)inst;
    }

/*---------------------------------------------------------------------------*\
 *                  Other methods                                            *
\*---------------------------------------------------------------------------*/

    /**
     * Change the frame used to allocate the variables of this method.
     * @param frame
     */
    public void changeFrame(ExecutionFrame frame) {
        this.frame = frame;
    }

    /**
     * Return the execution frame model associated with this list (if any).
     * @return
     */
    public ExecutionFrame frame() {
        return frame;
    }

    /**
     * Do a linear traverse (i.e. linked list traversal) of the IR
     * setting the relocation IP of each instruction according to
     * the order in which they are traversed (starting with 0).
     */
    public void logicallyRelocate() {
        int ip = 0;
        Instruction inst = head;
        while (inst != null) {
            inst.setRelocIP(ip++);
            inst = inst.getNext();
        }
    }

    /**
     * Trace the instruction in the list.
     * @param tracer
     * @param loader
     * @param path
     * @param traceIp
     */
    public void trace(Tracer tracer, ClassFileLoader loader, String path, boolean traceIp, boolean showDups) {

        // Read in the source file and break it into lines
        String[] lines = null;
        if (loader != null && path != null) {
            InputStream is = loader.openSourceFile(path);
            if (is != null) {
                try {
                    Vector v = (new BufferedReader(new InputStreamReader(is))).readLines(new Vector(1000));
                    lines = new String[v.size()];
                    v.copyInto(lines);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }

        int ip = 0;
        int lastLineNo = -1;
        logicallyRelocate();
        for (Instruction i = head; i != null; i = i.getNext()) {
            if (lines != null) {
                int lineNo = i.getSourceLineNo();
                if (lastLineNo == -1 || lineNo != lastLineNo) {
                    tracer.traceln("");
                    tracer.traceln(lines[lineNo-1]);
                    tracer.traceln("");
                    lastLineNo = lineNo;
                }
            }
            if (i instanceof BasicBlockEntryDelimiter) {
                BasicBlockEntryDelimiter delim = (BasicBlockEntryDelimiter)i;
                BitSet liveIn = delim.getBBLiveIn();
                if (liveIn != null) {
                    tracer.traceln("\tlive in: "+liveIn.toString());
                }
            }
            if (traceIp) {
                tracer.trace("["+i.getOriginalIP()+"]\t");
            }
            String ipPrefix = ip+":";
            tracer.traceln(ipPrefix+"\t"+i.toString(false, false, traceIp));

            if (showDups) {
                byte[] dupSwapOpcodes = i.getDupSwapSuccessors();
                if (dupSwapOpcodes != null) {

                    int dupOriginalIp = i.getOriginalIP()+1;
                    if (traceIp) {
                        if (i.getNext() != null) {
                            Instruction next = i.getNext();
                            if (next.getOriginalIP() != -1) {
                                dupOriginalIp = next.getOriginalIP() - dupSwapOpcodes.length;
                            }
                        }
                    }

                    for (int j = 0; j != dupSwapOpcodes.length; j++) {
                        int opcode = dupSwapOpcodes[j] & 0xFF;
                        if (traceIp) {
                            tracer.trace("["+(dupOriginalIp++)+"]\t");
                        }
                        for (int k = 0; k != ipPrefix.length(); k++) {
                            tracer.trace(" ");
                        }
                        tracer.traceln("\t"+JVMConst.mnemonics[opcode]);
                    }
                }
            }


            if (i instanceof BasicBlockExitDelimiter) {
                BasicBlockExitDelimiter delim = (BasicBlockExitDelimiter)i;
                BitSet liveOut = delim.getBBLiveOut();
                if (liveOut != null) {
                    tracer.traceln("\tlive out: "+liveOut.toString());
                }
            }
            ip++;
        }
    }
}
