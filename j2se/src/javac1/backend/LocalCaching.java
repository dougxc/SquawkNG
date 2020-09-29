/*
 * @(#)LocalCaching.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import javac1.Assert;
import javac1.JavaC1;
import javac1.backend.items.BlockItem;
import javac1.backend.reg.CachedLocals;
import javac1.backend.reg.RegAlloc;
import javac1.backend.reg.RegMask;
import javac1.backend.reg.RInfo;
import javac1.ci.Method;
import javac1.ir.IR;
import javac1.ir.Local;
import javac1.ir.Loop;
import javac1.ir.ScanBlocks;
import javac1.ir.instr.BlockBegin;
import javac1.ir.instr.BlockEnd;

/**
 * This class is used to cache local variables into registers. There are two
 * situations where this can be done. Since the receiver is always passed in a
 * certain register, the receiver can be cached throughout the whole method if
 * no instruction destroys that register. Additionally the most used local
 * variables inside leaf loops without calls and slow cases will be cached in
 * free registers.
 *
 * @see      CachedLocals
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class LocalCaching {
    /**
     * The intermediate representation.
     */
    private IR ir;

    /**
     * Constructs a new object that maps variables to registers.
     *
     * @param  ir  the intermediate representation
     */
    public LocalCaching(IR ir) {
        this.ir = ir;
    }

    /**
     * Collects the registers that will not be used inside the blocks of the
     * specified loop.
     *
     * @param   loop  loop to be examined
     * @return  list of free registers
     */
    private List collectFreeRegisters(Loop loop) {
        Method method = ir.getMethod();
        RegAlloc regAlloc = new RegAlloc();
        ValueGenInvariant vgi = new ValueGenInvariant(method, regAlloc, null);
        ValueGen visitor = new ValueGen(vgi, false);
        CodeGenerator gen = new CodeGenerator(visitor, vgi);
        loop.iterateBlocks(gen);
        boolean hasSpills = (gen.getMaxSpills() != 0);
        RegMask regs = regAlloc.getFreeRegisters();
        loop.setLockout(regs);
        List freeRegs = hasSpills ? regs.getRInfoCollection() : new ArrayList();
        if (javac1.Flags.PrintLoops) {
            JavaC1.out.print("loop " + loop.getStart().getBci());
            JavaC1.out.print(".." + loop.getEnd().getBci());
            if (hasSpills) {
                JavaC1.out.println(" has spills, no caching of locals");
            } else {
                JavaC1.out.print(" lockout ");
                for (int i = 0; i < freeRegs.size(); i++) {
                    RInfo reg = (RInfo) freeRegs.get(i);
                    JavaC1.out.print(reg.getRegister().toString() + " ");
                }
                JavaC1.out.println();
            }
        }
        loop.iterateBlocks(new ClearItems());
        return freeRegs;
    }

    /**
     * Assigns free registers to the most used local variables.
     *
     * @param  desc      descriptor that stores the mapping
     * @param  locals    most used local variables
     * @param  freeRegs  list of free registers
     * @param  loop      loop that uses the variables
     */
    private void assignRegisters(CachedLocals desc, List locals, List freeRegs,
            Loop loop) {
        int count = Math.min(locals.size(), freeRegs.size());
        for (int i = 0; i < count; i++) {
            Local local = (Local) locals.get(i);
            RInfo rinfo = (RInfo) freeRegs.get(i);
            local.setRInfo(rinfo);
            loop.addCachedLocal(local);
            desc.cacheLocal(local.getIndex(), rinfo, local.isOop());
        }
    }

    /**
     * Assigns the specified block item to each basic block in the list.
     *
     * @param  blocks  list of basic blocks
     * @param  item    block item to be assigned
     */
    private void addBlockItems(List blocks, BlockItem item) {
        for (int i = blocks.size() - 1; i >= 0; i--) {
            BlockBegin block = (BlockBegin) blocks.get(i);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(block.getBlockItem() == null, "destroying information");
            }
            block.setBlockItem(item);
        }
    }

    /**
     * Tries to cache local variables in the loops of the control flow graph.
     */
    public void cacheLoopLocals() {
        List loops = ir.getLoops();
        if ((loops == null) || ir.getMethod().hasExceptionHandlers()) {
            return;
        }
        for (int i = 0; i < loops.size(); i++) {
            Loop loop = (Loop) loops.get(i);
            ScanBlocks scan = new ScanBlocks();
            loop.iterateBlocks(scan);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(!scan.hasCalls(), "loops must not have calls");
            }
            if (scan.canCacheLocals()) {
                List freeRegs = collectFreeRegisters(loop);
                List locals = scan.mostUsedLocals();
                if (javac1.Flags.PrintLoops) {
                    JavaC1.out.print("most used locals ");
                    for (int j = 0; j < locals.size(); j++) {
                        int index = ((Local) locals.get(j)).getIndex();
                        JavaC1.out.print(index + " ");
                    }
                    JavaC1.out.println();
                }
                CachedLocals desc = new CachedLocals();
                assignRegisters(desc, locals, freeRegs, loop);
                boolean is32bit = scan.hasFloats() && !scan.hasDoubles();
                BlockItem item = new BlockItem(desc.getMapping(),
                    desc.getOops(), loop.getLockout(), is32bit);
                addBlockItems(loop.getBlocks(), item);
            }
        }
    }

    /**
     * Caches the receiver selectively. If the receiver cannot be cached in all
     * basic blocks of the control flow graph, this method at least tries to set
     * the specified block item into as many blocks as possible. A block cannot
     * cache the receiver if it contains inappropriate instructions or if one of
     * its predecessors cannot cache the receiver. Moreover no exception handler
     * entry block can cache the receiver.
     *
     * @param  item  caching information for cached receiver
     * @see    ScanBlocks#canCacheReceiver()
     */
    private void selectiveCaching(BlockItem item) {
        if (ir.getScanResult().hasJsr()) {
            return;
        }
        BitSet mark = new BitSet(ir.countBlocks());
        LinkedList workset = new LinkedList();
        BlockBegin start = ir.getTopScope().getStart();
        workset.addLast(start);
        start.setBlockItem(item);
        while (!workset.isEmpty()) {
            BlockBegin block = (BlockBegin) workset.removeLast();
            BlockItem blockItem = block.getBlockItem();
            if (!mark.get(block.getBlockId())) {
                mark.set(block.getBlockId());
                if (blockItem != null) {
                    ScanBlocks scan = new ScanBlocks();
                    scan.doBlock(block);
                    if (!scan.canCacheReceiver()) {
                        blockItem = null;
                    } else {
                        RegAlloc regAlloc = new RegAlloc();
                        ValueGenInvariant vgi = new ValueGenInvariant(
                            ir.getMethod(), regAlloc, null);
                        ValueGen visitor = new ValueGen(vgi, false);
                        CodeGenerator gen = new CodeGenerator(visitor, vgi);
                        gen.doBlock(block);
                        if (regAlloc.didUseRegister(ValueGen.RECV_REG)) {
                            blockItem = null;
                        }
                        new ClearItems().doBlock(block);
                    }
                }
                block.setBlockItem(blockItem);
                BlockEnd end = block.getEnd();
                for (int i = block.countExceptionHandlers() - 1; i >= 0; i--) {
                    BlockBegin handler = block.exceptionHandlerAt(i);
                    if (!mark.get(handler.getBlockId())) {
                        handler.setBlockItem(null);
                        workset.addLast(handler);
                    }
                }
                for (int i = end.countSux() - 1; i >= 0; i--) {
                    BlockBegin sux = end.suxAt(i);
                    if (mark.get(sux.getBlockId())) {
                        if ((sux.getBlockItem() != null) && (blockItem == null)) {
                            sux.setBlockItem(null);
                            workset.addLast(sux);
                            mark.clear(sux.getBlockId());
                        }
                    } else {
                        workset.addLast(sux);
                        sux.setBlockItem(blockItem);
                    }
                }
            }
        }
    }

    /**
     * Tries to cache the receiver throughout the whole method or in as many
     * blocks as possible.
     *
     * @param  regAlloc  allocator with register usage information
     * @see    #selectiveCaching(BlockItem)
     */
    public void cacheReceiver(RegAlloc regAlloc) {
        RegMask lockout = new RegMask(ValueGen.RECV_REG);
        List mapping = new ArrayList();
        mapping.add(ValueGen.RECV_REG);
        BlockItem item = new BlockItem(mapping, null, RegMask.EMPTY_SET, false);
        if (!regAlloc.didUseRegister(ValueGen.RECV_REG)
                && ir.getScanResult().canCacheReceiver()) {
            addBlockItems(ir.getCode().getCollection(), item);
        } else if (javac1.Flags.UseCachedReceiver2) {
            selectiveCaching(item);
        }
        item.setLockout(lockout);
    }
}
