/*
 * @(#)FrontEnd.java                    1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import java.util.List;
import javac1.Assert;
import javac1.BailOut;
import javac1.JavaC1;
import javac1.Statistics;
import javac1.ci.Method;
import javac1.ci.Runtime1;
import javac1.ir.BlockCollection;
import javac1.ir.IR;
import javac1.ir.IRScope;
import javac1.ir.ValueStack;
import javac1.ir.instr.BlockBegin;
import javac1.frontend.loops.LoopFinder;
import javac1.frontend.opt.Optimizer;

/**
 * Builds and optimizes the intermediate representation.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class FrontEnd {
    /**
     * Don't let anyone instantiate this class.
     */
    private FrontEnd() {}

    /**
     * Builds the control flow graph for the specified scope.
     *
     * @param  scope   scope to build graph for
     * @param  osrBci  bytecode index for OSR
     */
    protected static void buildGraph(IRScope scope, int osrBci) {
        long start = 0;
        if (javac1.Flags.PrintStatistics) {
            start = Runtime1.getElapsedCounter();
        }
        BlockListBuilder blb = new BlockListBuilder(scope, osrBci);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((osrBci < 0) || (blb.getOsrEntry() != null), "osr entry must exist");
        }
        ValueStack state = new ValueStack(scope, scope.getMethod().getMaxLocals());
        if (scope.getMethod().isSynchronized()) {
            state.lock(scope, javac1.Flags.SyncEntryBCI);
        }
        blb.getStdEntry().setState(state);
        GraphBuilder gb = new GraphBuilder(scope, blb.getBci2block(), blb.getStdEntry());
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((osrBci < 0) || blb.getOsrEntry().isFlagSet(BlockBegin.WAS_VISITED_FLAG), "osr entry must have been visited");
        }
        if (osrBci >= 0) {
            if (gb.foundJsr() && !javac1.Flags.AcceptJsrForOSR) {
                throw new BailOut("compiler does not accept jsr for osr");
            } else if (!blb.getOsrEntry().getState().isStackEmpty()) {
                throw new BailOut("stack not empty at osr entry");
            }
        }
        scope.setStart(blb.getStdEntry(), blb.getOsrEntry());
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.GRAPH_BUILDER_TIMER, elapsedTime);
        }
    }

    /**
     * Optimizes the specified intermediate representation. In the context of
     * this optimization conditional expressions will be eliminated and blocks
     * will be merged wherever possible.
     *
     * @param  ir  intermediate representation to be optimized
     * @see    Optimizer
     */
    private static void optimizeIR(IR ir) {
        long start = 0;
        if (javac1.Flags.PrintStatistics) {
            start = Runtime1.getElapsedCounter();
        }
        Optimizer opt = new Optimizer(ir);
        if (javac1.Flags.DoCEE) {
            opt.eliminateConditionalExpressions();
        }
        if (javac1.Flags.EliminateBlocks) {
            opt.eliminateBlocks();
        }
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.OPTIMIZER_TIMER, elapsedTime);
        }            
    }

    /**
     * Looks for call-free inner loops in the control flow graph.
     *
     * @param  ir  intermediate representation to search for loops
     * @see    LoopFinder
     */
    private static void computeLoops(IR ir) {
        long start = 0;
        if (javac1.Flags.PrintStatistics) {
            start = Runtime1.getElapsedCounter();
        }
        if (ir.getTopScope().hasLoops()) {
            LoopFinder lf = new LoopFinder(ir, ir.countBlocks());
            List loops = lf.computeLoops();
            ir.setLoops(loops);
        }
        if (javac1.Flags.PrintStatistics) {
            long elapsedTime = Runtime1.getElapsedCounter() - start;
            Statistics.increase(Statistics.LOOP_FINDER_TIMER, elapsedTime);
        }            
    }

    /**
     * Computes the weight of each block and the use count of each value. At the
     * end the intermediate representation contains a collection of basic blocks
     * sorted according to their weights.
     *
     * @param  ir  intermediate representation to compute code for
     */
    private static void computeCode(IR ir) {
        BlockCollection code = new BlockCollection();
        ir.iterateAndSetWeight(code);
        code.sort();
        SuxAndWeightAdjuster swa = new SuxAndWeightAdjuster();
        code.iterateForward(swa);
        UseCountComputer ucc = new UseCountComputer();
        code.iterateBackward(ucc);
        ir.setCode(code);
    }

    /**
     * Prints the specified intermediate representation.
     *
     * @param  msg      introducing message
     * @param  ir       intermediate representation to be printed
     * @param  cfgOnly  whether or not to print the control flow graph only
     */
    private static void printIR(String msg, IR ir, boolean cfgOnly) {
        JavaC1.out.println();
        JavaC1.out.println(msg);
        JavaC1.out.println();
        InstructionPrinter ip = new InstructionPrinter();
        BlockPrinter bp = new BlockPrinter(ip, cfgOnly);
        ir.getCode().iterateForward(bp);
    }

    /**
     * Builds the intermediate representation of the specified method.
     *
     * @param   method  the method to be parsed
     * @param   osrBci  bytecode index for OSR
     * @return  the intermediate representation
     */
    public static IR buildIR(Method method, int osrBci) {
        if (javac1.Flags.TraceBytecodes) {
            JavaC1.out.println();
            new BytecodeTracer(method).trace();
        }
        IR ir = new IR(method);
        buildGraph(ir.getTopScope(), osrBci);
        if (javac1.Flags.UseC1Optimizations) {
            if (javac1.Flags.DoCEE || javac1.Flags.EliminateBlocks) {
                optimizeIR(ir);
            }
            if (javac1.Flags.ComputeLoops) {
                computeLoops(ir);
            }
        }
        computeCode(ir);
        if (javac1.Flags.PrintCFG) {
            printIR("CFG BEFORE CODE GENERATION:", ir, true);
        }
        if (javac1.Flags.PrintIR) {
            printIR("IR BEFORE CODE GENERATION:", ir, false);
        }
        if (javac1.Flags.PrintStatistics) {
            Statistics.increase(Statistics.BLOCK_COUNTER, ir.countBlocks());
        }
        return ir;
    }
}
