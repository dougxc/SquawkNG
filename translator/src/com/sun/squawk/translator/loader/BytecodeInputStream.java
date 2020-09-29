
package com.sun.squawk.translator.loader;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.*;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * This class extends ClassFileInputStream to support decoding of higher level
 * constructs from an input stream representing a class file.
 */
public class BytecodeInputStream extends ClassFileInputStream {

    /** Method being read. */
    final private Method method;
    /** Constant pool for method. */
    final private ConstantPool pool;
    /** Stack map for method. */
    final private StackMap map;
    /** The highest valid ldc slot value. */
    final private int maxLocals;
    /** The IP of the start of the last bytecode. */
    private int lastBytecode;
    /** Limiter for the bytecodes input stream. */
    final private LimitInputStream lis;
    /** The ExecutionFrame corresponding to the bytecode being parsed. */
    private ExecutionFrame frame;
    /** The target that just produced a opc_branchtarget or opc_exceptiontarget. */
    private BytecodeAddress lastTarget;
    /** Hashtable of branch targets. */
    final private IntHashtable targets;
    /** List of sorted BytecodeAddress objects. */
    final private BytecodeAddress[] addressList;
    /** Current entry in the above list. */
    private int listEntry = 0;
    /** The ip address in the current list entry. */
    private int nextListEntry;
    /** The instruction factory. */
    private InstructionFactory ifactory;
    /** The instruction count. */
    private int instructionCount;

    /**
     * Create a BytecodeInputStream to read the bytecodes for a given method.
     */
    public static BytecodeInputStream create(
                                              InputStream is,
                                              int length,
                                              String fileName,
                                              Method method,
                                              ConstantPool pool,
                                              int maxLocals,
                                              StackMap map,
                                              BytecodeAddress[] addressList
                                             ) {
        return new BytecodeInputStream(new LimitInputStream(is, length), fileName, method, pool, maxLocals, map, addressList);
    }

    /**
     * Create a BytecodeInputStream to read the bytecodes for a given method.
     */
    private BytecodeInputStream(LimitInputStream lis,
                                String fileName,
                                Method method,
                                ConstantPool pool,
                                int maxLocals,
                                StackMap map,
                                BytecodeAddress[] addressList) {
        super(lis, fileName, method.type().vm());
        this.lis = lis;
        this.method = method;
        this.pool = pool;
        this.maxLocals = maxLocals;
        this.map = map;
        this.addressList = addressList;

        if (map != null) {
            targets = map.getTargets();
        } else {
            targets = new IntHashtable(0);
        }

       /*
        * Get the ip address of the first entry in the address list
        */
        if (addressList.length > 0) {
            nextListEntry = addressList[listEntry].getIP();
        } else {
            nextListEntry = Integer.MAX_VALUE;
        }
    }

    /**
     * Set the GraphBuilder object reading from this stream.
     * @param client The GraphBuilder object reading from this stream.
     */
    public void setFrame(ExecutionFrame frame) {
        this.frame = frame;
    }

    public void setInstructionFactory(InstructionFactory ifactory) {
        this.ifactory = ifactory;
    }

    /**
     * Return EOF state
     */
    public boolean atEof() {
        return lis.atEof() && nextListEntry == Integer.MAX_VALUE;
    }

    /**
     * Get the last IP address
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Get the current IP address
     */
    public int getCurrentIP() {
        return lis.getIP();
    }

    /**
     * Get the number of instructions decoded so far.
     * @return the number of instructions decoded so far.
     */
    public int getInstructionCount() {
        return instructionCount;
    }

    /**
     * Get the last IP address
     */
    public int getLastIP() {
        return lastBytecode;
    }

    /**
     * Read the next logical bytecode.
     * @param s The prefix for tracing.
     */
    public int readBytecode(String s) throws LinkageException {

        int opcode;

        // Get the ip address of the current bytecode
        lastBytecode = lis.getIP();

        // First look for a list entry
        if (nextListEntry <= lastBytecode) {

            // Check for corrupt stackmap
            frame.verify(nextListEntry >= lastBytecode, VEConst.BAD_STACKMAP + ": " + nextListEntry+" < "+lastBytecode);

            // Get the BytecodeAddress entry
            lastTarget = addressList[listEntry++];

            // Get the ip address of the next entry in the address list
            if (listEntry < addressList.length) {
                nextListEntry =  addressList[listEntry].getIP();
            } else {
                nextListEntry = Integer.MAX_VALUE;
            }

            // Get the opcode
            opcode = lastTarget.opcode();
        }
        else {
            // Zero lastTarget for safety
            lastTarget = null;

            // Get the bytecode
            opcode = readUnsignedByte(null);

            // Update the instruction count
            instructionCount++;

            // If at the end make sure that the address list was completely read
            frame.verify(opcode != -1 || nextListEntry == Integer.MAX_VALUE, VEConst.BAD_STACKMAP+"");
        }

        // Do tracing.
        if (tracer.switchedOn()) {
           s = "["+lastBytecode+"] ";
           tracer.trace(s);
           if (opcode < JVMConst.mnemonics.length) {
               tracer.traceln(JVMConst.mnemonics[opcode]);
           } else {
               tracer.traceln(""+opcode);
           }
           //traceln("");
        }

        // Return the bytecode
        return opcode;
    }

   /**
    * Return the target at lastBytecode
    */
    public Target readTarget() {
        tracer.traceln("readTarget "+lastTarget.getIP());
        return (Target)lastTarget;
    }

    /**
     * Return the current "try" start or end point.
     * @param true if this is expected to be a start point.
     */
    public Target readTryPoint(boolean isStart) {
        Assert.that(isStart ? lastTarget instanceof TryStartPoint : lastTarget instanceof TryEndPoint);
        TryPoint point = (TryPoint)lastTarget;
        if (point instanceof TryStartPoint) {
            tracer.traceln("readStartPoint " +  point.handler().getIP());
        }
        else {
            Assert.that(point instanceof TryEndPoint);
            tracer.traceln("readEndPoint " + point.handler().getIP());
        }
        return point.handler();
    }

    /**
     * Read a 2-byte offset from the stream and return the target at lastBytecode + the offset.
     */
    public Target readTarget2() throws LinkageException {
        int offset = lastBytecode + readShort(null);
        Target target = (Target)targets.get(offset);
        frame.verify(target != null, VEConst.TARGET_BAD_TYPE);

        tracer.traceln("readTarget2 "+target.getIP());
        return target;
    }

    /**
     * Read a 4-byte offset from the stream and return the target at lastBytecode + the offset.
     */
    public Target readTarget4() throws LinkageException {
        int offset = lastBytecode + readInt(null);
        Target target = (Target)targets.get(offset);
        frame.verify(target != null, VEConst.TARGET_BAD_TYPE);

        tracer.traceln("readTarget4 "+target.getIP());
        return target;
    }

    /**
     * Read a few nulls until the strean is aligned to a cell boundry.
     */
    public void roundToCellBoundry() throws LinkageException {
        while (lis.getIP()%4 != 0) {
            int ch = readByte(null);
            frame.verify(ch == 0, "Switch instruction not padded with 0's");
            tracer.traceln("roundToCellBoundary++");
        }
    }

    /**
     * Return a constant indexed by a 1-byte constant pool index.
     */
    public Instruction readLdc() throws LinkageException {
        Instruction lc = ldc(readUnsignedByte(null), false);
        tracer.traceln("readLdc "+lc);
        return lc;
    }

    /**
     * Return a constant indexed by a 2-byte constant pool index.
     */
    public Instruction readLdc_w() throws LinkageException {
        Instruction lc = ldc(readUnsignedShort(null), false);
        tracer.traceln("readLdc_w "+lc);
        return lc;
    }

    /**
     * Return a constant long/double indexed by a 2-byte constant pool index.
     */
    public Instruction readLdc2_w() throws LinkageException {
        Instruction lc = ldc(readUnsignedShort(null), true);
        tracer.traceln("readLdc2_w "+lc);
        return lc;
    }

    /**
     * Return a constant given a constant pool index.
     * @param index The constant pool index.
     * @param isLong True if the constant is a long or double.
     */
    private Instruction ldc(int index, boolean isLong) throws LinkageException {
        int tag = pool.getTag(index);
        if (isLong) {
            if (tag == JVMConst.CONSTANT_Long) {
                return ifactory.createLoadConstantLong(pool.getLong(index));
            }
/*if[FLOATS]*/
            if (tag == JVMConst.CONSTANT_Double) {
                return ifactory.createLoadConstantDouble(pool.getDouble(index));
            }
/*end[FLOATS]*/
        } else {
            if (tag == JVMConst.CONSTANT_Integer) {
                return ifactory.createLoadConstantInt(pool.getInt(index));
            }
/*if[FLOATS]*/
            if (tag == JVMConst.CONSTANT_Float) {
                return ifactory.createLoadConstantFloat(pool.getFloat(index));
            }
/*end[FLOATS]*/
            if (tag == JVMConst.CONSTANT_String) {
                return ifactory.createLoadConstantObject(vm.STRING, pool.getString(index));
            }
        }
        frame.verify(false, VEConst.BAD_LDC);
        return null;
    }

    /**
     * Return a new array type.
     */
    public Type readNewArrayType() throws LinkageException {
        int code = readUnsignedByte(null);
        Type result = null;
        switch (code) {
            case JVMConst.T_BOOLEAN: result =  vm.BOOLEAN_ARRAY;   break;
            case JVMConst.T_BYTE:    result =  vm.BYTE_ARRAY;      break;
            case JVMConst.T_CHAR:    result =  vm.CHAR_ARRAY;      break;
            case JVMConst.T_SHORT:   result =  vm.SHORT_ARRAY;     break;
            case JVMConst.T_INT:     result =  vm.INT_ARRAY;       break;
            case JVMConst.T_LONG:    result =  vm.LONG_ARRAY;      break;
/*if[FLOATS]*/
            case JVMConst.T_FLOAT:   result =  vm.FLOAT_ARRAY;     break;
            case JVMConst.T_DOUBLE:  result =  vm.DOUBLE_ARRAY;    break;
/*end[FLOATS]*/
            default: frame.verify(false, "Bad new array type "+code);
        }
        tracer.traceln("readNewArrayType "+code+" = "+result);
        return result;

    }

    /**
     * Return a type.
     */
    public Type readType() throws LinkageException {
        int index = readUnsignedShort(null);
        int tag = pool.getTag(index);
        frame.verify(tag == JVMConst.CONSTANT_Class, VEConst.EXPECT_CLASS);
        Type type = pool.resolveType(index);
        tracer.traceln("readType "+type);
        return type;

    }

    /**
     * Return a type for a new instruction.
     */
    public Type readNewType() throws LinkageException {
       /*
        * Get the constant pool entry
        */
        int typeIndex = readUnsignedShort(null);
        Type newType = pool.resolveType(typeIndex);

       /*
        * Look for a type proxy for this location in the stackmap.
        * If there is one then set the proxy's real type to the one just
        * found, otherwise simply return the constant pool type.
        */
        TypeProxy proxy = null;
        if (map != null) {
            proxy = map.findNewType(lastBytecode);
            if (proxy != null) {
                proxy.setProxy(newType);
                newType = proxy;
            }
        }
        if (proxy == null) {
            int ip = lastBytecode;
            proxy = TypeProxy.createForMap(vm, vm.NEWOBJECT.name()+"@"+ip);
            proxy.setSuperType(vm.NEWOBJECT);
            proxy.setProxy(newType);
            newType = proxy;
        }

        /*
         * Trace and return
         */
        tracer.traceln("readNewType "+newType);
        return newType;
    }


    /**
     * Return a field.
     */
    public Field readField(boolean isStatic) throws LinkageException {
        int index = readUnsignedShort(null);
        int tag = pool.getTag(index);
        frame.verify(tag == JVMConst.CONSTANT_Field, VEConst.EXPECT_FIELDREF);
        Field f = pool.resolveField(index, isStatic);
        tracer.traceln("readField "+f);
        return f;
    }

   /**
    * Return a method
    */
    public Method readMethod(boolean isStatic) throws LinkageException {
        int index = readUnsignedShort(null);
        int tag = pool.getTag(index);
        frame.verify(tag == JVMConst.CONSTANT_Method || tag == JVMConst.CONSTANT_InterfaceMethod, VEConst.EXPECT_METHODREF);
        Method m = pool.resolveMethod(index, tag == JVMConst.CONSTANT_InterfaceMethod, isStatic);
        String verbose = ""+m.parent()+"::";
        tracer.traceln("readMethod "+verbose+m);
        return m;
    }


   /**
    * Verify that a given local variable slot number is within range.
    */
    private int checkSlot(int slot, int width) throws LinkageException {
        frame.verify((slot+width) <= maxLocals, VEConst.LOCALS_OVERFLOW + ": " + (slot+width)+" is more than "+maxLocals);
        tracer.traceln("readSlot "+slot);
        return slot;
    }

   /**
    * Return slot in the next u1
    */
    public int readSlot1(int width) throws LinkageException {
        return checkSlot(readUnsignedByte(null), width);
    }

   /**
    * Return slot in the next u2
    */
    public int readSlot2(int width) throws LinkageException {
        return checkSlot(readUnsignedShort(null), width);
    }
}
