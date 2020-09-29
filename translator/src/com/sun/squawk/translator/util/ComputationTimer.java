package com.sun.squawk.translator.util;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Stack;
import com.sun.squawk.translator.Assert;

/**
 * This is a utility for timing computations.
 */
public class ComputationTimer {

    /**
     * A computation to be timed that does not throw a checked exception.
     * The computation is performed by invoking
     * Timer.time on the ComputationTimer.Computation object.
     */
    public static interface Computation {
        /**
         * Performs the computation that will be timed.
         * @return a context dependent value that may represent the result of
         * the computation. Each class that implements PrivilegedAction should
         * document what (if anything) this value represents.
         */
        public Object run();
    }

    /**
     * A computation to be timed that throws a checked exception.
     * The computation is performed by invoking
     * Timer.time on the ComputationTimer.Computation object.
     */
    public static interface ComputationException {
        /**
         * Performs the computation that will be timed.
         * @return a context dependent value that may represent the result of
         * the computation. Each class that implements PrivilegedAction should
         * document what (if anything) this value represents.
         */
        public Object run() throws Exception;
    }

    static class Execution {
        long nestedTimes;
        Object result;
        Exception exception;
    }

    /** The collected times. */
    private Hashtable flatTimes  = new Hashtable();
    private Hashtable totalTimes = new Hashtable();


    private Stack executions = new Stack();

    private Execution execute(String id, Object c) {
        long start = System.currentTimeMillis();
        Execution e = new Execution();
        executions.push(e);
        Long currentTotal = (Long)totalTimes.get(id);
        try {
            if (c instanceof Computation) {
                e.result = ((Computation)c).run();
            } else {
                e.result = ((ComputationException)c).run();
            }
        } catch (Exception ex) {
            e.exception = ex;
        } finally {
            executions.pop();
            long time = System.currentTimeMillis() - start;
            if (!executions.isEmpty()) {
                ((Execution)executions.peek()).nestedTimes += time;
            }
            totalTimes.put(id, new Long(time+(currentTotal == null ? 0L : currentTotal.longValue())));

            Long flatTime = (Long)flatTimes.get(id);
            if (flatTime == null) {
                flatTimes.put(id, new Long(time - e.nestedTimes));
            } else {
                flatTimes.put(id, new Long(flatTime.longValue() + (time - e.nestedTimes)));
            }
        }
        return e;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     * @param id The identifier for the computation.
     * @param computation The computation to be performed and timed.
     * @return the result of the computation.
     */
    public Object time(String id, Computation computation) {
        Execution e = execute(id, computation);
        if (e.exception != null) {
            Assert.that(e.exception instanceof RuntimeException);
            throw (RuntimeException)e.exception;
        }
        return e.result;
    }

    /**
     * Time a specified computation denoted by a specified identifier. The
     * time taken to perform the computation is added to the accumulative
     * time to perform all computations with the same identifier.
     * @param id The identifier for the computation.
     * @param computation The computation to be performed and timed.
     * @return the result of the computation.
     */
    public Object time(String id, ComputationException computation) throws Exception {
        Execution e = execute(id, computation);
        if (e.exception != null) {
            throw e.exception;
        }
        return e.result;
    }

    public Enumeration getComputations() {
        return flatTimes.keys();
    }
    public Enumeration getFlatTimes() {
        return flatTimes.elements();
    }
    public Enumeration getTotalTimes() {
        return totalTimes.elements();
    }

    public Long getFlatTime(String id) {
        return (Long)flatTimes.get(id);
    }
    public Long getTotalTime(String id) {
        return (Long)totalTimes.get(id);
    }

    /**
     * Returns a string representation of the times accumulated by this Timer
     * object in the form of a set of entries, enclosed in braces and separated
     * by the ASCII characters ", " (comma and space). Each entry is rendered
     * as the computation identifier, a colon sign ':', the flat time associated
     * with the computation, a colon sign ':' and the total time associated
     * with the computation.
     * @return a string representation of this timer.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("{ ");
        Enumeration keys = flatTimes.keys();
        Enumeration ftimes = flatTimes.elements();
        Enumeration ttimes = totalTimes.elements();
        while (keys.hasMoreElements()) {
            String id = (String)keys.nextElement();
            Long ftime = (Long)ftimes.nextElement();
            Long ttime = (Long)ttimes.nextElement();
            buf.append(id).append(":").append(ftime.toString()).append(":").append(ttime.toString());
            if (keys.hasMoreElements()) {
                buf.append(", ");
            }
        }
        return buf.append(" }").toString();
    }
}
