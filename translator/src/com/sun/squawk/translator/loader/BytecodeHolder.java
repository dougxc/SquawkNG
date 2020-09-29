
package com.sun.squawk.translator.loader;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.*;
import java.util.Vector;
import java.util.Enumeration;



import java.io.ByteArrayInputStream;

/**
 * A Code object is a container for all the "Code" attribute related components of a Java classfile method.
 */
public class BytecodeHolder {

    /** Parent method. */
    /*private*/ final Method method;
    /** Enclosing translator (convenient short-cut for method.parent().getVM()). */
    private final Translator vm;
    /** The constant pool. */
    private final ConstantPool pool;
    /** The bytecode. */
    private final byte[] bytecodes;
    /** The bytecode instruction count. */
    private int instructionCount;
    /** The stack size needed. */
    /*private*/ final int maxStack;
    /** The number of locals. */
    /*private*/ final int maxLocals;
    /**
     * The stackmap classfile data. The StackMap must not be created until the IR is
     * required as it refers to other types that cannot have their loading triggered
     * too early.
     */
    private final byte[] stackMapData;

    /** The StackMap for the method. */
    /*private*/ StackMap stackMap;
    /**
     * The line number table (or null). This is serialized array of pairs: entry N gives
     * the next start_pc value and entry N+1 gives the corresponding line_number value.
     */
    final int[] lineNumberTable;
    /** Exception handlers. */
    private ExceptionHandler[] handlers;
    /** LocalVariableTable. */
    final LocalVariableTable localVariableTable;
    /** Liveness. */
    final Liveness liveness;

    /**
     * Constructor.
     */
    public BytecodeHolder(Method method,
                          ConstantPool pool,
                          byte[] bytecodes,
                          int maxStack,
                          int maxLocals,
                          ExceptionHandler[] handlers,
                          byte[] stackMapData,
                          int[] lineNumberTable,
                          LocalVariableTable localVariableTable,
                          Liveness liveness)
    {
        this.method    = method;
        this.vm        = method.parent().vm();
        this.pool      = pool;
        this.bytecodes = bytecodes;
        this.maxStack  = maxStack;
        this.maxLocals = maxLocals;
        this.handlers  = handlers;
        this.stackMapData       = stackMapData;
        this.lineNumberTable    = lineNumberTable;
        this.localVariableTable = localVariableTable;
        this.liveness           = liveness;
    }

    /**
     * Check the exception handlers, add the exception types to them, and build
     * a sorted vector of BytecodeAddress objects that contains all the
     * handler start and end points, branch targets, and exception targets
     */
    BytecodeAddress[] checkHandlers(String methodName, StackMap map) throws LinkageException {

        // Exit now if there are no stackmaps
        if (map == null) {
            if (handlers.length != 0) {
                throw new LinkageException(vm.VERIFYERROR, VEConst.TARGET_BAD_TYPE.toString());
            }
            return new BytecodeAddress[0];
        }

        // Get the stackmaps.
        IntHashtable targets = map.getTargets();

        // Allocate an array for all the addresses
        BytecodeAddress[] entries = new BytecodeAddress[targets.size()+(handlers.length * 2)];
        int entriesIndex = 0;

        // Add in the exception handler entries
        if (handlers.length > 0) {
            for (int i = 0 ; i < handlers.length ; i++) {
                ExceptionHandler handler = handlers[i];
                Target target = map.lookup(handler.handler);
                if (target == null) {
                    throw new LinkageException(vm.VERIFYERROR, "No stackmap entry for handler in " + methodName);
                }

                // If catchIndex is zero then this is a try/finally block. In this case
                // the exception type Type.THROWABLE will cover all possibilities
                Type catchType  = (handler.catchType == 0) ? vm.THROWABLE : pool.resolveType(handler.catchType);

                if (!catchType.vIsAssignableTo(vm.THROWABLE)) {
                    throw new LinkageException(vm.VERIFYERROR, "Expect subclass of java.lang.Throwable");
                }

                // Set the exception type in the target
                target.setExceptionType(catchType);

                // Add the start point to the address list
                entries[entriesIndex++] = new TryStartPoint(handler.start, target, i);

                // Add the end handler point to the address list
                entries[entriesIndex++] = new TryEndPoint(handler.end, target, i);
            }
        }

        // Add all the stackmap targets into the address list
        for (Enumeration e = targets.elements() ; e.hasMoreElements() ;) {
            Target target = (Target)e.nextElement();
            entries[entriesIndex++] = target;
        }

        // Return sorted entries
        Arrays.sort(entries, BytecodeAddress.COMPARER); // Here is the sorting....
        return entries;
    }

    /**
     * Convert the bytecodes into the IR.
     */
    public InstructionList buildIR() throws LinkageException {

        // Get the VM for this method
        Translator vm = method.type().vm();

        // Ensure that the parameters and return type are loaded
        method.loadSignatureTypes();

        // Create the stackmap object.
        if (stackMapData != null) {
            ClassFileInputStream in = new ClassFileInputStream(new ByteArrayInputStream(stackMapData), method.name(), vm);

            // Set trace if requested
            in.setTrace(vm.traceraw(method.toString()));

            stackMap = new StackMap(
                  in,
                  pool,
                  method,
                  liveness
                );
        }

        // Get the list of all the targets in this method that represent addresses with StackMap entries
        // and exception handler entry points.
        Vector handlerTableSortList = new Vector();
        BytecodeAddress[] addressList = checkHandlers(method.name(), stackMap);

        int numHandlers = 0;
        for (int i = 0; i != addressList.length; i++) {
            if (addressList[i] instanceof TryStartPoint) {
                numHandlers++;
            }
        }
        if (method.isSynchronized()) {
            numHandlers++;
        }

        // Build the IR from the bytecodes.
        BytecodeInputStream bcis = BytecodeInputStream.create(
               new ByteArrayInputStream(bytecodes),
               bytecodes.length,
               method.toString(),
               method,
               pool,
               maxLocals,
               stackMap,
               addressList
       );

        // Enable trace if requested
        bcis.setTrace(vm.tracebytecodes(method.toString()));

        // Build the IR
        InstructionList ilist = GraphBuilder.read(vm, bcis, this);
        instructionCount = bcis.getInstructionCount();

        Assert.that(numHandlers == 0 || numHandlers == ilist.getHandlerTable().entries().length);

        // Do the liveness analysis and re-allocation of local variables if no
        // Liveness attribute was supplied or verify the Liveness attribute if
        // was supplied. Note that this *must* occur before
        // the GraphTransformer which potentially introduces new Local variables.
        LivenessDFA.analyse(method,
                            ilist,
                            maxLocals,
                            maxStack,
                            liveness,
                            true,
                            true,
                            localVariableTable);


        // Transform the IR
        GraphTransformer.transform(method, ilist);

        return ilist;
    }

    /**
     *
     * @return
     */
    public Method.CodeMetrics getJavacMetrics() {
        return new Method.CodeMetrics(bytecodes.length, maxStack, maxLocals, instructionCount);
    }
}
