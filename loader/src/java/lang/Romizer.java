package java.lang;

import com.sun.squawk.suite.*;

/**
 * This is just a stub for compilation purposes.
 */
public class Romizer {
    public static void assume(boolean b) { }
    public static void assume(boolean b, String messge) { }
    public static boolean isTCK() { return false; }

    public static void logOopMapCreation(boolean isShared) {}
    public static void logSuite(String name) {}
    public static void logClass(SuiteClass sc, boolean isProxy) {}
    public static void logMethod() {}
    public static void logInstruction(int opcode) {}
    public static void logLongInstruction(int opcode) {}
    public static void logFloatInstruction(int opcode) {}

    public static int getNextNativeMethodNumber() {
        throw new RuntimeException("Can't dynamically load native methods");
    }
}