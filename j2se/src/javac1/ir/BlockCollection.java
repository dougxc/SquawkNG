/*
 * @(#)BlockCollection.java             1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javac1.ir.instr.BlockBegin;

/**
 * This class can be used to collect blocks using any traversal algorithm.
 *
 * @see      BlockBegin
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class BlockCollection implements BlockClosure {
    /**
     * The collection of basic blocks.
     */
    private List collection;

    /**
     * Constructs an empty block collection.
     */
    public BlockCollection() {
        collection = new ArrayList();
    }

    /**
     * Constructs an empty block collection with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the collection
     */
    public BlockCollection(int initialCapacity) {
        collection = new ArrayList(initialCapacity);
    }

    /**
     * Returns the collection of basic blocks.
     *
     * @return  collection of basic blocks
     */
    public List getCollection() {
        return collection;
    }

    /**
     * Adds the specified block to the block collection.
     *
     * @param  block  block to be added
     */
    public void doBlock(BlockBegin block) {
        collection.add(block);
    }

    /**
     * Iterates forward over the collected blocks and passes them to the
     * specified block closure.
     *
     * @param  bc  the block closure
     */
    public void iterateForward(BlockClosure bc) {
        int len = collection.size();
        for (int i = 0; i < len; i++) {
            bc.doBlock((BlockBegin) collection.get(i));
        }
    }

    /**
     * Iterates backward over the collected blocks and passes them to the
     * specified block closure.
     *
     * @param  bc  the block closure
     */
    public void iterateBackward(BlockClosure bc) {
        for (int i = collection.size() - 1; i >= 0; i--) {
            bc.doBlock((BlockBegin) collection.get(i));
        }
    }

    /**
     * Applies the operation represented by the specified value closure to each
     * input and state value of each instruction in the collected basic blocks.
     *
     * @param  vc  the value closure
     * @see    BlockBegin#doBlockValues(ValueClosure)
     * @see    javac1.ir.instr.Instruction#doValues(ValueClosure)
     */
    public void doValues(ValueClosure vc) {
        for (int i = collection.size() - 1; i >= 0; i--) {
            ((BlockBegin) collection.get(i)).doBlockValues(vc);
        }
    }

    /**
     * Sorts the collected blocks according to their weights.
     *
     * @see  BlockBegin#compareTo(Object)
     */
    public void sort() {
        Collections.sort(collection);
    }

    /**
     * Returns the number of blocks contained in this collection.
     *
     * @return  number of collected blocks
     */
    public int size() {
        return collection.size();
    }
}
