/*
 * @(#)Statistics.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

/**
 * Maintains various counters and timers used for performance tests.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Statistics {
    /**
     * A constant that identifies a counter.
     */
    public static final int
            METHOD_COUNTER      = 0, BYTECODE_COUNTER    = 1,
            BLOCK_COUNTER       = 2, GRAPH_BUILDER_TIMER = 3,
            OPTIMIZER_TIMER     = 4, LOOP_FINDER_TIMER   = 5,
            REG_ALLOC_TIMER     = 6, OOP_MAP_GEN_TIMER   = 7,
            CODE_EMISSION_TIMER = 8;
    
    /**
     * The total number of counters.
     */
    public static final int NUM_COUNTERS = 9;
    
    /**
     * The description printed together with the statistical data.
     */
    private static String[] labels = new String[NUM_COUNTERS];
    
    /**
     * Whether or not the counters record elapsed time.
     */
    private static boolean[] timers = new boolean[NUM_COUNTERS];
    
    /**
     * The current value of each counter.
     */
    private static long[] values = new long[NUM_COUNTERS];
    
    /**
     * Defines the counters and installs a shutdown hook that dumps the results.
     */
    static {
        if (Flags.PrintStatistics) {
            define(METHOD_COUNTER,      "number of methods           ", false);
            define(BYTECODE_COUNTER,    "number of bytecodes         ", false);
            define(BLOCK_COUNTER,       "number of basic blocks      ", false);
            define(GRAPH_BUILDER_TIMER, "time in graph builder       ", true );
            define(OPTIMIZER_TIMER,     "time in optimizer           ", true );
            define(LOOP_FINDER_TIMER,   "time in loop finder         ", true );
            define(REG_ALLOC_TIMER,     "time for register allocation", true );
            define(OOP_MAP_GEN_TIMER,   "time for oop map generation ", true );
            define(CODE_EMISSION_TIMER, "time for code emission      ", true );
            Runtime.getRuntime().addShutdownHook(new PrintStatistics());
        }
    }
    
    /**
     * Defines a new counter with the specified index and label.
     *
     * @param  index  index of the counter
     * @param  label  description of the value
     * @param  timer  whether or not counter records time
     */
    private static void define(int index, String label, boolean timer) {
        labels[index] = label;
        timers[index] = timer;
    }
    
    /**
     * Increases the value of a counter by the specified value.
     *
     * @param  index  index of the counter to be increased
     * @param  delta  value to increase counter by
     */
    public static synchronized void increase(int index, long delta) {
        values[index] += delta;
    }
    
    /**
     * Increases the value of a counter by 1.
     *
     * @param  index  index of the counter to be increased
     */
    public static synchronized void increase(int index) {
        values[index]++;
    }
    
    /**
     * Don't let anyone instantiate this class.
     */
    private Statistics() {}
    
    /**
     * Used as shutdown hook that prints statistical data.
     */
    static class PrintStatistics extends Thread {
        /**
         * Prints the statistical data.
         */
        public void run() {
            JavaC1.out.println();
            JavaC1.out.println("STATISTICS:");
            JavaC1.out.println();
            for (int i = 0; i < NUM_COUNTERS; i++) {
                JavaC1.out.print(labels[i] + " = ");
                if (timers[i]) {
                    JavaC1.out.println((double) values[i] / Flags.ElapsedFrequency);
                } else {
                    JavaC1.out.println(values[i]);
                }
            }
        }
    }
}
