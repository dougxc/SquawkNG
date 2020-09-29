/*
 * @(#)BlockClosure.java                1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import javac1.ir.instr.BlockBegin;

/**
 * The interface for block traversal and iteration. It represents an operation
 * to be performed on every basic block of the control flow graph.
 *
 * @see      BlockBegin#iteratePreorder(BlockClosure)
 * @see      BlockBegin#iteratePostorder(BlockClosure)
 * @see      BlockBegin#iterateTopological(BlockClosure)
 * @see      BlockBegin#iterateAndSetWeight(BlockClosure)
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public interface BlockClosure {
    public void doBlock(BlockBegin block);
}
