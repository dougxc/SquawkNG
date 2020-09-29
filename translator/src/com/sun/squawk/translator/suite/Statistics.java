package com.sun.squawk.translator.suite;

import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

/**
 * A collection of various interesting suite statistics.
 */
public class Statistics {

    static class Delta {
        public final Object object;
        public final int before;
        public final int after;
        public int afterAsPercentOfBefore() {
            return asPercentOf(after, before);
        }
        public Delta(Object object, int before, int after) {
            this.object = object;
            this.before = before;
            this.after  = after;
        }
        public static Comparer COMPARER = new Comparer() {
            public int compare(Object d1, Object d2) {
                int percent1 = ((Delta)d1).afterAsPercentOfBefore();
                int percent2 = ((Delta)d2).afterAsPercentOfBefore();
                if (percent1 < percent2) {
                    return -1;
                }
                else if (percent1 > percent2) {
                    return 1;
                }
                else {
                    return 0;
                }
            }
        };
    }

    private final Vector methods = new Vector();
    public void addTranslatedMethod(Method method) {
        methods.addElement(method);
    }

    void printMetric(String metric, Delta[] deltas, PrintStream out) {
        int oldTotal = 0;
        int newTotal = 0;
        out.println();
        out.println("-----------------------------------------------------------");
        out.println(metric);
        out.println("-----------------------------------------------------------");
        for (int i = 0; i != deltas.length; i++) {
            Delta delta = deltas[i];
            Method m = (Method)delta.object;
            oldTotal += delta.before;
            newTotal += delta.after;
            Method.CodeMetrics metrics = m.getSquawkMetrics();
            String floatLongSuffix = "";
/*if[FLOATS]*/
            if (metrics.usesFloatOrDouble) {
                floatLongSuffix = " (FD)";
            }
/*end[FLOATS]*/
            if (metrics.usesLong) {
                floatLongSuffix += " (L)";
            }
            out.println(delta.afterAsPercentOfBefore()+"%:\t "+delta.before+"\t->\t"+delta.after+"\t[ "+m+" ]"+floatLongSuffix);
        }
        out.println("Total: " +asPercentOf(newTotal, oldTotal)+"%\t"+oldTotal+"\t->\t"+newTotal);

    }

    public void print(PrintStream out) {
        // print method size changes
        Delta[] deltas = new Delta[methods.size()];
        int i = 0;
        for (Enumeration e = methods.elements(); e.hasMoreElements();) {
            Method m = (Method)e.nextElement();
            deltas[i++] = new Delta(m, m.getJavacMetrics().length, m.getSquawkMetrics().length);
        }
        Arrays.sort(deltas, Delta.COMPARER);
        printMetric("Difference in bytecode size:", deltas, out);
        out.println();

        // print instruction count changes
        i = 0;
        for (Enumeration e = methods.elements(); e.hasMoreElements();) {
            Method m = (Method)e.nextElement();
            deltas[i++] = new Delta(m, m.getJavacMetrics().instructionCount, m.getSquawkMetrics().instructionCount);
        }
        Arrays.sort(deltas, Delta.COMPARER);
        printMetric("Difference in instruction counts:", deltas, out);
        out.println();

        // print maxStack changes
        i = 0;
        for (Enumeration e = methods.elements(); e.hasMoreElements();) {
            Method m = (Method)e.nextElement();
            deltas[i++] = new Delta(m, m.getJavacMetrics().maxStack, m.getSquawkMetrics().maxStack);
        }
        Arrays.sort(deltas, Delta.COMPARER);
        printMetric("Difference in max stack:", deltas, out);
        out.println();

        // print maxLocal changes
        i = 0;
        for (Enumeration e = methods.elements(); e.hasMoreElements();) {
            Method m = (Method)e.nextElement();
            deltas[i++] = new Delta(m, m.getJavacMetrics().maxLocals, m.getSquawkMetrics().maxLocals);
        }
        Arrays.sort(deltas, Delta.COMPARER);
        printMetric("Difference in max locals:", deltas, out);
        out.println();
    }

    /**
     * Compute value1 as a percent of value2.
     * @param value1
     * @param value2
     * @return value2 as a percent of value1.
     */
    static int asPercentOf(int value1, int value2) {
        int v1 = (value1 == 0 ? 1 : value1);
        int v2 = (value2 == 0 ? 1 : value2);
        return (v1*100) / v2;
    }
}

