/*
 * @(#)BackEnd.java                     1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import java.util.ArrayList;
import java.util.List;
import javac1.Assert;
import javac1.JavaC1;
import javac1.Statistics;
import javac1.ci.Method;
import javac1.ci.Runtime1;
import javac1.ir.IR;
import javac1.ir.instr.BlockBegin;
import javac1.backend.code.Assembler;
import javac1.backend.code.CodeBuffer;
import javac1.backend.code.CodeEmitter;
import javac1.backend.code.ConstantTable;
import javac1.backend.code.Disassembler;
import javac1.backend.code.FrameMap;
import javac1.backend.code.MacroAssembler;
import javac1.backend.debug.CodeEmitInfo;
import javac1.backend.reg.RegAlloc;
import javac1.backend.reg.RInfo;

/**
 * Generates code for the intermediate representation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BackEnd {
    /**
     * Don't let anyone instantiate this class.
     */
    private BackEnd() {}

    /**
     * Prints the contents of the specified code buffer.
     *
     * @param  code     code buffer to be printed
     * @param  offsets  the offset descriptor
     */
    private static void printCode(CodeBuffer code) {
        int codeStart = code.getCodeBegin();
        int entryPoint = codeStart + code.getOffsets().getEpOffset();
        int stubsStart = code.getStubsBegin();
        int stubsEnd = code.getStubsEnd();
        Disassembler debug = new Disassembler(code);
        if (javac1.Flags.PrintConstantTable && (entryPoint > codeStart)) {
            JavaC1.out.println("CONSTANT TABLE:");
            debug.hexDump(codeStart, entryPoint);
            JavaC1.out.println();
        }
        if (javac1.Flags.PrintCode && (stubsStart > entryPoint)) {
            JavaC1.out.println("MACHINE CODE:");
            JavaC1.out.println();
            debug.disassemble(entryPoint, stubsStart);
            JavaC1.out.println();
        }
        if (javac1.Flags.PrintCallStubs && (stubsEnd > stubsStart)) {
            JavaC1.out.println("CALL STUBS:");
            JavaC1.out.println();
            debug.disassemble(stubsStart, stubsEnd);
            JavaC1.out.println();
        }
        if (javac1.Flags.PrintRelocInfo && code.getRelocInfo().length > 0) {
            JavaC1.out.println("RELOCATION INFORMATION:");
            JavaC1.out.println();
            debug.printRelocInfo();
            JavaC1.out.println();
        }
        if (javac1.Flags.PrintDebugInfo && code.getBcis().length > 0) {
            JavaC1.out.println("DEBUG INFORMATION:");
            JavaC1.out.println();
            debug.printDebugInfo();
            JavaC1.out.println();
        }
    }

    /**
     * Allocates registers for the specified intermediate representation.
     *
     * @param   ir  the intermediate representation
     * @return  the maximum number of spill elements
     */
    private static int allocateRegisters(IR ir) {
        long start = 0;
        if (javac1.Flags.PrintStatistics) {
            start = Runtime1.getElapsedCounter();
        }
        Method method = ir.getMethod();
        RegAlloc regAlloc = new RegAlloc();
        ValueGenInvariant vgi = new ValueGenInvariant(method, regAlloc, null);
        ValueGen visitor = new ValueGen(vgi, false);
        CodeGenerator gen = new CodeGenerator(visitor, vgi);
        ir.getCode().iterateForward(gen);
        ir.getCode().iterateForward(new ClearItems());
        int maxSpills = gen.getMaxSpills();
        if (javac1.Flags.CacheReceiver) {
            if (!method.isStatic() && !ir.getTopScope().hasLoops()) {
                new LocalCaching(ir).cacheReceiver(regAlloc);
            }
        }
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.REG_ALLOC_TIMER, elapsedTime);
        }
        return maxSpills;
    }

    /**
     * Tries to cache local variables in registers.
     *
     * @param  ir  the intermediate representation
     */
    private static void cacheLocals(IR ir) {
        if (!ir.getMethod().hasExceptionHandlers()) {
            new LocalCaching(ir).cacheLoopLocals();
        }
    }

    /**
     * Emits the code prolog for the specified method.
     *
     * @param  method    the method
     * @param  ir        the intermediate representation
     * @param  frameMap  the frame map
     */
    private static void emitCodeProlog(Method method, IR ir, FrameMap frameMap) {
        frameMap.setSizeArguments(method.getArgSize());
        int sizeLocals = Math.max(method.getArgSize(), method.getMaxLocals());
        frameMap.setSizeLocals(sizeLocals);
        if (method.isNative()) {
            int sizeMonitors = method.isSynchronized() ? 1 : 0;
            frameMap.setSizeMonitors(sizeMonitors);
        } else {
            frameMap.setSizeMonitors(ir.getNumberOfLocks());
        }
        frameMap.setFixedFrameSize();
    }

    /**
     * Emits the code epilog.
     *
     * @param  ir         the intermediate representation
     * @param  emit       the code emitter
     * @param  maxSpills  maximum number of spill elements
     */
    private static void emitCodeEpilog(IR ir, CodeEmitter emit, int maxSpills) {
        BlockBegin osrEntry = (ir == null) ? null : ir.getOsrEntry();
        if (javac1.Flags.UseOnStackReplacement && (osrEntry != null)) {
            emit.emitOsrEntry(maxSpills, osrEntry.getEnd().getState().getLocksSize(),
                osrEntry.getLabel(), osrEntry.getBci());
        }
        emit.emitSlowCaseStubs();
        emit.addNop();
        int offset = emit.emitExceptionHandler(maxSpills);
        emit.getMasm().getCode().setExceptionOffset(offset);
        emit.emitCallStubs();
    }

    /**
     * Emits the code body.
     *
     * @param  ir         the intermediate representation
     * @param  maxSpills  maximum number of spill elements
     * @param  code       the code buffer
     */
    private static void emitCodeBody(IR ir, int maxSpills, CodeBuffer code) {
        long start = 0;
        if (javac1.Flags.PrintStatistics) {
            start = Runtime1.getElapsedCounter();
        }
        Method method = ir.getMethod();
        OopMapGenerator omg = new OopMapGenerator(method);
        omg.computeMap();
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.OOP_MAP_GEN_TIMER, elapsedTime);
            start = Runtime1.getElapsedCounter();
        }
        FrameMap frameMap = new FrameMap(maxSpills, null);
        emitCodeProlog(method, ir, frameMap);
        ConstantTable table = new ConstantTable();
        if (javac1.Flags.UseFPConstTables) {
            CollectConstants collect = new CollectConstants(table);
            ir.getCode().iterateForward(collect);
        }
        MacroAssembler masm = new MacroAssembler(code);
        CodeEmitter emit = new CodeEmitter(masm, method, ir.getScanResult(),
            frameMap, table, omg.getCachedOops());
        emit.emitConstants();
        emit.methodEntry(omg.getInitVars(), maxSpills);
        RegAlloc regAlloc = new RegAlloc();
        ValueGenInvariant vgi = new ValueGenInvariant(method, regAlloc, emit);
        ValueGen visitor = new ValueGen(vgi, true);
        CodeGenerator gen = new CodeGenerator(visitor, vgi);
        ir.getCode().iterateForward(gen);
        emitCodeEpilog(ir, emit, maxSpills);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(maxSpills >= gen.getMaxSpills(), "frame is too small");
        }
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.CODE_EMISSION_TIMER, elapsedTime);
        }
    }

    /**
     * Generates code for the specified intermediate representation.
     *
     * @param  ir  the intermediate representation
     */
    public static CodeBuffer generateCode(IR ir) {
        int maxSpills = allocateRegisters(ir);
        if (javac1.Flags.CacheLocalsInLoops) {
            cacheLocals(ir);
        }
        CodeBuffer code = new CodeBuffer();
        emitCodeBody(ir, maxSpills, code);
        printCode(code);
        return code;
    }
    
    /**
     * Emits machine code for the specified native method.
     *
     * @param  method  the method to generate code for
     * @param  entry   address of the native code
     */
    public static CodeBuffer emitCodeForNative(Method method, int entry) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(method.isNative(), "should not reach here");
        }
        int sizeSpills = method.isStatic() ? 1 : 0;
        FrameMap frameMap = new FrameMap(sizeSpills, null);
        emitCodeProlog(method, null, frameMap);
        CodeBuffer code = new CodeBuffer();
        MacroAssembler masm = new MacroAssembler(code);
        CodeEmitter emit = new CodeEmitter(masm, method, null, frameMap, null, null);
        List spilled = null;
        if (method.isStatic()) {
            spilled = new ArrayList();
            spilled.add(new Integer(0));
        }
        CodeEmitInfo info = new CodeEmitInfo(0, spilled, null, null);
        emit.methodEntry(null, sizeSpills);
        emit.nativeCall(entry, info);
        emit.nativeMethodExit(info);
        emitCodeEpilog(null, emit, sizeSpills);
        return code;
    }
    
    /**
     * Returns the address map for the specified bytecode index.
     *
     * @param   bci  the bytecode index
     * @return  address map for the specified bytecode index
     */
    public static List getAddressMap(Method method, int bci) {
        if ((method.getCodeSize() == 0) || (method.getMaxLocals() == 0)) {
            return null;
        }
        AddressMapGenerator amg = new AddressMapGenerator(method);
        amg.computeMap();
        List addressMap = amg.getAddressMap();
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(addressMap != null, "address map must exist");
        }
        return addressMap;
    }
}
