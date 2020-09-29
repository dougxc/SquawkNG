/*
 * @(#)Flags.java                       1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

import javac1.ci.Runtime1;

/**
 * Defines all global flags used by the compiler. The actual values of these
 * flags affect the behavior of the compiler as well as the quality of the
 * generated code.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class Flags {
    /**
     * Returns the value of the integer flag with the specified name.
     *
     * @param  name  the name of the flag
     * @param  def   a default value
     */
    public static int getIntFlag(String name, int def) {
        String value = System.getProperty(name);
        return (value != null) ? Integer.parseInt(value) : def;
    }
    
    /**
     * Returns the value of the boolean flag with the specified name.
     *
     * @param  name  the name of the flag
     * @param  def   a default value
     */
    public static boolean getBoolFlag(String name, boolean def) {
        String value = System.getProperty(name);
        return (value != null) ? value.equals("true") : def;
    }
    
    /**
     * Returns the value of the string flag with the specified name.
     *
     * @param  name  the name of the flag
     * @param  def   a default value
     */
    public static String getStringFlag(String name, String def) {
        return System.getProperty(name, def);
    }
    
    /**
     * Whether or not on-stack-replacement is performed on methods with
     * subroutines.
     */
    public static final boolean AcceptJsrForOSR = getBoolFlag("javac1.AcceptJsrForOSR", true);
    
    /**
     * Whether or not all objects should be aligned on double word boundaries.
     */
    public static final boolean AlignAllObjects = true;

    /**
     * The number of bytes per word. The constant value of this field is
     * <tt>4</tt>.
     */
    public static final int BytesPerWord = 4;

    /**
     * The number of bits per word.
     */
    public static final int BitsPerWord = BytesPerWord * 8;

    /**
     * Whether or not a breakpoint is set at the entry of each compiled method.
     */
    public static final boolean C1Breakpoint = getBoolFlag("javac1.C1Breakpoint", false);

    /**
     * Whether or not the receiver is cached in a register throughout the
     * method.
     */
    public static final boolean CacheReceiver = getBoolFlag("javac1.CacheReceiver", true);

    /**
     * Whether or not local variables are cached in registers inside leaf loops.
     */
    public static final boolean CacheLocalsInLoops = getBoolFlag("javac1.CacheLocalsInLoops", true);

    /**
     * Turns canonicalization of graph nodes on or off.
     */
    public static final boolean CanonicalizeNodes = getBoolFlag("javac1.CanonicalizeNodes", true);

    /**
     * Whether or not the code is optimized for P6 family processors.
     */
    public static final boolean CodeForP6 = javac1.ci.Runtime1.isP6();
    
    /**
     * Limits the set of methods that will be compiled.
     */
    public static final String CompileOnly = getStringFlag("javac1.CompileOnly", null);

    /**
     * Turns the computation of loop structures on or off.
     */
    public static final boolean ComputeLoops = getBoolFlag("javac1.ComputeLoops", true);

    /**
     * Whether or not conditional expressions are eliminated.
     */
    public static final boolean DoCEE = getBoolFlag("javac1.DoCEE", true);
    
    /**
     * Prevents certain methods from being compiled.
     */
    public static final String DontCompile = getStringFlag("javac1.DontCompile", null);

    /**
     * The frequency of the high-resolution performance counter.
     */
    public static final long ElapsedFrequency = Runtime1.getElapsedFrequency();

    /**
     * Whether or not unneccessary basic blocks are eliminated.
     */
    public static final boolean EliminateBlocks = getBoolFlag("javac1.EliminateBlocks", true);

    /**
     * Whether or not unneccessary load instructions are eliminated.
     */
    public static final boolean EliminateLoads = getBoolFlag("javac1.EliminateLoads", true);

    /**
     * Turns generation of code for array store checks on or off.
     */
    public static final boolean GenerateArrayStoreCheck = getBoolFlag("javac1.GenerateArrayStoreCheck", true);

    /**
     * Turns generation of explicit null checks on or off.
     */
    public static final boolean GenerateCompilerNullChecks = getBoolFlag("javac1.GenerateCompilerNullChecks", true);

    /**
     * Whether or not pointer maps are generated during code emission.
     */
    public static final boolean GenerateOopMaps = getBoolFlag("javac1.GenerateOopMaps", true);

    /**
     * Turns generation of range checks for array accesses on or off.
     */
    public static final boolean GenerateRangeChecks = getBoolFlag("javac1.GenerateRangeChecks", true);

    /**
     * Turns generation of locking and unlocking code for synchronized methods
     * and monitors on or off.
     */
    public static final boolean GenerateSynchronizationCode = getBoolFlag("javac1.GenerateSynchronizationCode", true);

    /**
     * Turns implicit division by zero checks on or off.
     */
    public static final boolean ImplicitDiv0Checks = getBoolFlag("javac1.ImplicitDiv0Checks", true);

    /**
     * Turns generation of code for implicit null checks on or off.
     */
    public static final boolean ImplicitNullChecks = getBoolFlag("javac1.ImplicitNullChecks", true);

    /**
     * Whether or not methods that can be statically resolved are inlined.
     */
    public static final boolean Inline = getBoolFlag("javac1.Inline", true);

    /**
     * Turns inlining of intrinsics that can be statically resolved on or off.
     */
    public static final boolean InlineIntrinsics = getBoolFlag("javac1.InlineIntrinsics", true);

    /**
     * Turns inlining of simple methods that can be statically resolved on or
     * off.
     */
    public static final boolean InlineSimpleMethods = getBoolFlag("javac1.InlineSimpleMethods", true);

    /**
     * The maximum bytecode size of a method to be inlined. The constant value
     * of this field is <tt>35</tt>.
     */
    public static final int MaxInlineSize = getIntFlag("javac1.MaxInlineSize", 35);

    /**
     * Whether or not an explicit null check is generated at caller.
     */
    public static final boolean NullCheckAtCaller = getBoolFlag("javac1.NullCheckAtCaller", true);

    /**
     * Whether or not parameters are passed in registers.
     */
    public static final boolean PassParametersInRegisters = false;

    /**
     * Whether or not all instructions are considered to be pinned by default.
     */
    public static final boolean PinAllInstructions = getBoolFlag("javac1.PinAllInstructions", false);
    
    /**
     * The name of a Java archive to be precompiled during start-up.
     */
    public static final String Precompile = getStringFlag("javac1.Precompile", null);
    
    /**
     * Whether or not bailouts and their reasons are printed.
     */
    public static final boolean PrintBailouts = getBoolFlag("javac1.PrintBailouts", false);

    /**
     * Whether or not basic block elimination is printed.
     */
    public static final boolean PrintBlockElimination = getBoolFlag("javac1.PrintBlockElimination", false);

    /**
     * Whether or not the code of the call stubs is printed.
     */
    public static final boolean PrintCallStubs = getBoolFlag("javac1.PrintCallStubs", false);

    /**
     * Whether or not graph node canonicalization is printed.
     */
    public static final boolean PrintCanonicalization = getBoolFlag("javac1.PrintCanonicalization", false);

    /**
     * Whether or not the generated machine code is printed.
     */
    public static final boolean PrintCode = getBoolFlag("javac1.PrintCode", false);

    /**
     * Whether or not the table of floating-point constants is printed.
     */
    public static final boolean PrintConstantTable = getBoolFlag("javac1.PrintConstantTable", false);

    /**
     * Whether or not the names of methods being compiled are printed.
     */
    public static final boolean PrintCompilation = getBoolFlag("javac1.PrintCompilation", false);
    
    /**
     * Whether or not conditional expression elimination is printed.
     */
    public static final boolean PrintCEE = getBoolFlag("javac1.PrintCEE", false);

    /**
     * Whether or not the control flow graph is printed before code generation.
     */
    public static final boolean PrintCFG = getBoolFlag("javac1.PrintCFG", false);
    
    /**
     * Whether or not the debug information is printed.
     */
    public static final boolean PrintDebugInfo = getBoolFlag("javac1.PrintDebugInfo", false);

    /**
     * Whether or not inlining optimizations are printed.
     */
    public static final boolean PrintInlining = getBoolFlag("javac1.PrintInlining", false);

    /**
     * Whether or not the intermediate representation is printed.
     */
    public static final boolean PrintIR = getBoolFlag("javac1.PrintIR", false);

    /**
     * Whether or not the elimination of load instruction is printed.
     */
    public static final boolean PrintLoadElimination = getBoolFlag("javac1.PrintLoadElimination", false);

    /**
     * Whether or not loop structures are printed.
     */
    public static final boolean PrintLoops = getBoolFlag("javac1.PrintLoops", false);

    /**
     * Whether or not information about register allocation is printed.
     */
    public static final boolean PrintRegAlloc = getBoolFlag("javac1.PrintRegAlloc", false);

    /**
     * Whether or not relocation information is printed.
     */
    public static final boolean PrintRelocInfo = getBoolFlag("javac1.PrintRelocInfo", false);
    
    /**
     * Whether or not statistical data is printed at shutdown of the VM.
     */
    public static final boolean PrintStatistics = getBoolFlag("javac1.PrintStatistics", false);
    
    /**
     * Whether or not floating-point values must be rounded when storing them.
     */
    public static final boolean RoundFloatsWithStore = true;

    /**
     * The bytecode index for the synchronization entry.
     */
    public static final int SyncEntryBCI = -1;
    
    /**
     * The number of timer samples that are accumulated into one output.
     */
    public static final int TimeEach = getIntFlag("javac1.TimeEach", 0);
    
    /**
     * Whether or not the bytecodes of the method to be compiled are printed.
     */
    public static final boolean TraceBytecodes = getBoolFlag("javac1.TraceBytecodes", false);

    /**
     * Turns client compiler optimizations on or off.
     */
    public static final boolean UseC1Optimizations = getBoolFlag("javac1.UseC1Optimizations", true);

    /**
     * Turns the use of the receiver cached in a register on or off.
     */
    public static final boolean UseCachedReceiver = getBoolFlag("javac1.UseCachedReceiver", true);

    /**
     * Turns the improved caching of the receiver on or off.
     */
    public static final boolean UseCachedReceiver2 = getBoolFlag("javac1.UseCachedReceiver2", true);

    /**
     * Whether or not safepoints are used in compiled code.
     */
    public static final boolean UseCompilerSafepoints = getBoolFlag("javac1.UseCompilerSafepoints", true);

    /**
     * Whether or not fast inlined locking code is used.
     */
    public static final boolean UseFastLocking = getBoolFlag("javac1.UseFastLocking", true);

    /**
     * Whether or not fast inlined instance allocation is used.
     */
    public static final boolean UseFastNewInstance = getBoolFlag("javac1.UseFastNewInstance", true);

    /**
     * Whether or not fast inlined object array allocation is used.
     */
    public static final boolean UseFastNewObjectArray = getBoolFlag("javac1.UseFastNewObjectArray", true);

    /**
     * Whether or not fast inlined type array allocation is used.
     */
    public static final boolean UseFastNewTypeArray = getBoolFlag("javac1.UseFastNewTypeArray", true);

    /**
     * Whether or not floating-point constants are stored in a table.
     */
    public static final boolean UseFPConstTables = getBoolFlag("javac1.UseFPConstTables", true);
    
    /**
     * Turns on-stack-replacement on or off.
     */
    public static final boolean UseOnStackReplacement = getBoolFlag("javac1.UseOnStackReplacement", true);

    /**
     * Turns common subexpression elimination on or off.
     */
    public static final boolean UseValueNumbering = getBoolFlag("javac1.UseValueNumbering", true);

    /**
     * Turns plausibility checks for object pointers on or off.
     */
    public static final boolean VerifyOops = getBoolFlag("javac1.VerifyOops", false);

    /**
     * Whether the receiver is verified to be a subclass of the method holder.
     */
    public static final boolean VerifyReceiver = getBoolFlag("javac1.VerifyReceiver", false);

    /**
     * Don't let anyone instantiate this class.
     */
    private Flags() {}
}
