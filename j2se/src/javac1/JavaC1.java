/*
 * @(#)JavaC1.java                      1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import javac1.backend.BackEnd;
import javac1.backend.code.CodeBuffer;
import javac1.backend.debug.OffsetDesc;
import javac1.ci.InstanceKlass;
import javac1.ci.Klass;
import javac1.ci.Method;
import javac1.ci.Runtime1;
import javac1.frontend.FrontEnd;
import javac1.ir.IR;

/**
 * This class provides the main entry point for JavaC1. It exports a method that
 * can be called by the JVM in order to compile a method.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class JavaC1 {
    /**
     * The standard output stream for debugging.
     */
    public static final PrintWriter out = new PrintWriter(System.out, true);

    /**
     * Whether or not the compiler has already been initialized.
     */
    private static boolean initialized = false;
    
    /**
     * Initializes the compiler and precompiles a Java archive if specified.
     */
    static {
        try {
            if (Flags.Precompile != null) {
                compileArchive(Flags.Precompile);
            }
            initialized = true;
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
    
    /**
     * Installs the generated machine code into the VM.
     *
     * @param  method           the method
     * @param  entryBci         bytecode index of the entry
     * @param  argCount         number of arguments
     * @param  insts            machine instruction sequence
     * @param  codeStart        start address of the code
     * @param  codeSize         total size of the machine code
     * @param  stubsSize        size of the stubs in the code
     * @param  exceptionOffset  offset of exception handler
     * @param  relocs           relocation information
     * @param  bcis             bytecode indices of debug information
     * @param  offsets          program counter offsets
     * @param  atCalls          selects debug information at calls
     * @param  frameSizes       array of frame sizes
     * @param  oopRegsArr       registers that can contain pointers
     * @param  oops             array of object pointers
     * @param  iepOffset        interpreter entry point
     * @param  epOffset         standard entry point
     * @param  vepOffset        verified entry point
     * @param  codeOffset       code begin
     * @param  osrOffset        offset of the OSR entry
     */
    private static native void installCode(Object method, int entryBci,
        int argCount, byte[] insts, int codeStart, int codeSize, int stubsSize,
        int exceptionOffset, short[] relocs, int[] bcis, int[] offsets,
        boolean[] atCalls, int[] frameSizes, int[][] oopRegsArr, Object[] oops,
        int iepOffset, int epOffset, int vepOffset, int codeOffset, int osrOffset);
    
    /**
     * Installs the generated machine code into the VM.
     *
     * @param  method  the method
     * @param  code    the code buffer
     */
    private static void installCode(Method method, CodeBuffer code, int osrBci) {
        OffsetDesc offsets = code.getOffsets();
        installCode(method.getOop(), osrBci, method.getArgSize(),
            code.getBytes(), code.getCodeBegin(), code.getCodeSize(),
            code.getStubsSize(), code.getExceptionOffset(),
            code.getRelocInfo(), code.getBcis(), code.getCodeOffsets(),
            code.getAtCalls(), code.getFrameSizes(), code.getOopRegs(),
            code.getOops(), offsets.getIepOffset(), offsets.getEpOffset(),
            offsets.getVepOffset(), offsets.getCodeOffset(),
            offsets.getOsrOffset());
    }
    
    /**
     * Don't let anyone instantiate this class.
     */
    private JavaC1() {}

    /**
     * Compiles the specified native method.
     *
     * @param  method  the method to be compiled
     */
    private static void compileNativeMethod(Method method) {
        if (Flags.PrintCompilation) {
            JavaC1.out.println("native " + method.toString());
        }
        try {
            int entry = method.getNativeEntry();
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(entry != 0, "native entry must have been precomputed");
            }
            CodeBuffer code = BackEnd.emitCodeForNative(method, entry);
            installCode(method, code, -1);
        } catch (BailOut bailout) {
            if (Flags.PrintBailouts) {
                JavaC1.out.println(bailout.getMessage());
            }
        }
    }
    
    /**
     * Compiles the specified Java method.
     *
     * @param  method  the method to be compiled
     * @param  osrBci  bytecode index for OSR
     */
    private static void compileJavaMethod(Method method, int osrBci) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!method.isAbstract(), "cannot compile abstract methods");
            Assert.that(!method.isNative(), "should not reach here");
        }
        if (Flags.PrintCompilation) {
            JavaC1.out.print(method.toString());
            if (osrBci >= 0) {
                JavaC1.out.print(" (osr bci = " + osrBci + ")");
            }
            JavaC1.out.println();
        }
        try {
            IR ir = FrontEnd.buildIR(method, osrBci);
            CodeBuffer code = BackEnd.generateCode(ir);
            installCode(method, code, osrBci);
        } catch (BailOut bailout) {
            if (Flags.PrintBailouts) {
                JavaC1.out.println(bailout.getMessage());
            }
        }
    }
    
    /**
     * Compiles the specified method and installs the generated machine code.
     *
     * @param  method  the method to be compiled
     * @param  osrBci  bytecode index for OSR
     */
    public static void compileMethod(Method method, int osrBci) {
        if (!initialized
                || ((Flags.CompileOnly != null) && (method.toString().indexOf(Flags.CompileOnly) < 0))
                || ((Flags.DontCompile != null) && (method.toString().indexOf(Flags.DontCompile) >= 0))) {
            return;
        }
        int index = (Flags.TimeEach > 0) ? CompilationTimer.start() : -1;
        if (method.isNative()) {
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(osrBci < 0, "no osr for native methods");
            }
            compileNativeMethod(method);
        } else {
            compileJavaMethod(method, osrBci);
        }
        if (Flags.TimeEach > 0) {
            CompilationTimer.stop(index, method.getCodeSize());
        }
        if (Flags.PrintStatistics) {
            Statistics.increase(Statistics.METHOD_COUNTER);
            Statistics.increase(Statistics.BYTECODE_COUNTER, method.getCodeSize());
        }
    }
    
    /**
     * Compiles all classes in the specified archive file.
     *
     * @param   fname  name of the archive file
     */
    private static void compileArchive(String fname) throws Exception {
        ZipFile jar = null;
        try {
            jar = new ZipFile(fname);
            Enumeration enum = jar.entries();
            while (enum.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) enum.nextElement();
                String file = entry.getName();
                if (!entry.isDirectory() && file.endsWith(".class")) {
                    String clazz = file.substring(0, file.length() - 6);
                    Class.forName(clazz.replace('/', '.'));
                    InstanceKlass klass = (InstanceKlass) Klass.lookup(clazz);
                    Method[] methods = klass.getMethods();
                    for (int i = 0; i < methods.length; i++) {
                        if (methods[i].isAbstract()) {
                            /* nothing to do */
                        } else if (methods[i].isNative()) {
                            compileNativeMethod(methods[i]);
                        } else {
                            compileJavaMethod(methods[i], -1);
                        }
                    }
                }
            }
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }
    
    /**
     * The main method initiating the compilation of one certain method. This
     * method is called when the user runs the compiler from the command line.
     *
     * @param  args  command line arguments
     */
    public static void main(String[] args) throws ClassNotFoundException {
        if (args.length == 3) {
            Class.forName(args[0].replace('/', '.'));
            Method method = Method.lookup(args[0], args[1], args[2]);
            compileMethod(method, -1);
        } else {
            System.err.println("invalid number of arguments");
            System.exit(1);
        }
    }
}
