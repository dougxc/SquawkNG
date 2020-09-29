/*
 * @(#)CompilationTimer.java            1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

import javac1.ci.Runtime1;

/**
 * Records number of compiled methods as well as compilation time and speed.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class CompilationTimer {
    /**
     * The maximum number of threads that will be timed simultaneously.
     */
    private static final int MAX_THREADS = 5;
    
    /**
     * The maximum number of recursive compilations per thread.
     */
    private static final int MAX_LEVEL = Runtime1.getRecursiveCompileLimit();
    
    /**
     * The size of the stack partition that is used by one single thread.
     */
    private static final int SIZE_PER_THREAD = MAX_LEVEL + 1;
    
    /**
     * The maximum size of the stack used for recording start events.
     */
    private static final int MAX_STACK = SIZE_PER_THREAD * MAX_THREADS;
    
    /**
     * The stack used for recording compilation start events per thread.
     */
    private static long[] compStack = new long[MAX_STACK];
    
    /**
     * The total amount of time required for compilation since last reset.
     */
    private static long totalTicks = 0;
    
    /**
     * The total amount of bytecodes compiled since last reset.
     */
    private static long totalSize = 0;
    
    /**
     * The total number of methods compiled per level.
     */
    private static int[] totalCount = new int[MAX_LEVEL];
    
    /**
     * The number of samples recorded since last reset.
     */
    private static int samples = 0;
    
    /**
     * Records the current time as start event on the stack.
     *
     * @return  index of the start event
     */
    public static synchronized int start() {
        int threadId = Runtime1.getThreadId();
        int empty = -1;
        int index = 0;
        while ((index < MAX_STACK) && ((compStack[index] >> 8) != threadId)) {
            if ((compStack[index] & 0xff) == 0) {
                empty = index;
            }
            index += SIZE_PER_THREAD;
        }
        if (index == MAX_STACK) {
            if (empty >= 0) {
                index = empty;
                compStack[index] = (long) threadId << 8;
            } else {
                return -1;
            }
        }
        compStack[index]++;
        index += compStack[index] & 0xff;
        compStack[index] = Runtime1.getElapsedCounter();
        return index;
    }
    
    /**
     * Calculates the time elapsed since start event and updates statistics.
     *
     * @param  index  index of the start event
     * @param  size   number of bytecodes compiled
     */
    public static synchronized void stop(int index, int size) {
        if (index < 0) {
            return;
        }
        int level = index % SIZE_PER_THREAD - 1;
        if (Assert.ASSERTS_ENABLED) {
            long header = compStack[index - level - 1];
            Assert.that((header & 0xff) - 1 == level, "must stop top level first");
            Assert.that((header >> 8) == Runtime1.getThreadId(), "thread numbers must match");
        }
        long elapsedTicks = Runtime1.getElapsedCounter() - compStack[index];
        totalTicks += elapsedTicks;
        totalSize += size;
        totalCount[level]++;
        while (--index % SIZE_PER_THREAD > 0) {
            compStack[index] += elapsedTicks;
        }
        compStack[index]--;
        if (++samples >= Flags.TimeEach) {
            print();
        }
    }
    
    /**
     * Prints the statistical data and resets the state.
     */
    private static synchronized void print() {
        StringBuffer buf = new StringBuffer("count = [");
        buf.append(totalCount[0]);
        totalCount[0] = 0;
        for (int i = 1; i < MAX_LEVEL; i++) {
            buf.append(' ');
            buf.append(totalCount[i]);
            totalCount[i] = 0;
        }
        buf.append("] size = ");
        buf.append(totalSize);
        double elapsedTime = (double) totalTicks / Flags.ElapsedFrequency;
        double speed = totalSize / elapsedTime;
        buf.append(" speed = ");
        buf.append(speed);
        JavaC1.out.println(buf.toString());
        totalTicks = 0;
        totalSize = 0;
        samples = 0;
    }
    
    /**
     * Don't let anyone instantiate this class.
     */
    private CompilationTimer() {}
}
