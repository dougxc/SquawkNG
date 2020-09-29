/*
 * @(#)BytecodeTracer.java              1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.frontend;

import java.util.List;
import javac1.Assert;
import javac1.Bytecodes;
import javac1.BytecodeStream;
import javac1.JavaC1;
import javac1.ci.ExceptionHandler;
import javac1.ci.Method;

/**
 * Prints the bytecodes of a method and their attributes. The bytecode tracer
 * iterates over the bytecodes of a method and prints their indices, names and
 * operands. This class is intended for debugging mainly.
 *
 * @see      Bytecodes
 * @author   Thomas Kotzmann
 * @version  1.00
 */
class BytecodeTracer {
    /**
     * The method to be printed.
     */
    private Method method;

    /**
     * The stream to read bytecodes from.
     */
    private BytecodeStream stream;

    /**
     * Constructs a new bytecode tracer.
     *
     * @param  method  the method to be printed
     */
    public BytecodeTracer(Method method) {
        this.method = method;
    }

    /**
     * Prints the index and the name of the current bytecode.
     */
    private void printBytecode() {
        String bci = "    " + String.valueOf(stream.getBci());
        JavaC1.out.print(bci.substring(bci.length() - 4) + " ");
        JavaC1.out.print(Bytecodes.getName(stream.getBytecode()) + " ");
    }

    /**
     * Prints the operands of the current bytecode.
     */
    private void printOperands() {
        switch (stream.getBytecode()) {
        case Bytecodes._illegal:
        	Assert.shouldNotReachHere();
        	break;
        case Bytecodes._bipush:
            JavaC1.out.print(stream.getSigned(1, 1));
            break;
        case Bytecodes._sipush:
            JavaC1.out.print(stream.getSigned(1, 2));
            break;
        case Bytecodes._ldc:
            JavaC1.out.print(stream.getUnsigned(1, 1));
            break;
        case Bytecodes._ldc_w:
            /* falls through */
        case Bytecodes._ldc2_w:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._iload:
            /* falls through */
        case Bytecodes._lload:
            /* falls through */
        case Bytecodes._fload:
            /* falls through */
        case Bytecodes._dload:
            /* falls through */
        case Bytecodes._aload:
            /* falls throuh */
        case Bytecodes._istore:
            /* falls through */
        case Bytecodes._lstore:
            /* falls through */
        case Bytecodes._fstore:
            /* falls through */
        case Bytecodes._dstore:
            /* falls through */
        case Bytecodes._astore:
            JavaC1.out.print(stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._iinc:
            JavaC1.out.print(stream.getUnsigned(1, 1, 2, 2) + " ");
            JavaC1.out.print(stream.getSigned(2, 1, 4, 2));
            break;
        case Bytecodes._ifeq:
            /* falls through */
        case Bytecodes._ifne:
            /* falls through */
        case Bytecodes._iflt:
            /* falls through */
        case Bytecodes._ifge:
            /* falls through */
        case Bytecodes._ifgt:
            /* falls through */
        case Bytecodes._ifle:
            /* falls through */
        case Bytecodes._if_icmpeq:
            /* falls through */
        case Bytecodes._if_icmpne:
            /* falls through */
        case Bytecodes._if_icmplt:
            /* falls through */
        case Bytecodes._if_icmpge:
            /* falls through */
        case Bytecodes._if_icmpgt:
            /* falls through */
        case Bytecodes._if_icmple:
            /* falls through */
        case Bytecodes._if_acmpeq:
            /* falls through */
        case Bytecodes._if_acmpne:
            /* falls through */
        case Bytecodes._goto:
            /* falls through */
        case Bytecodes._jsr:
            JavaC1.out.print(stream.getDestination(1, false));
            break;
        case Bytecodes._ret:
            JavaC1.out.print(stream.getUnsigned(1, 1, 2, 2));
            break;
        case Bytecodes._getstatic:
            /* falls through */
        case Bytecodes._putstatic:
            /* falls through */
        case Bytecodes._getfield:
            /* falls through */
        case Bytecodes._putfield:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._invokevirtual:
            /* falls through */
        case Bytecodes._invokespecial:
            /* falls through */
        case Bytecodes._invokestatic:
            /* falls through */
        case Bytecodes._invokeinterface:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._new:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._newarray:
            JavaC1.out.print(stream.getSigned(1, 1));
            break;
        case Bytecodes._anewarray:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._checkcast:
            /* falls through */
        case Bytecodes._instanceof:
            JavaC1.out.print(stream.getUnsigned(1, 2));
            break;
        case Bytecodes._multianewarray:
            JavaC1.out.print(stream.getUnsigned(1, 2) + " ");
            JavaC1.out.print(stream.getSigned(3, 1));
            break;
        case Bytecodes._ifnull:
            /* falls through */
        case Bytecodes._ifnonnull:
            JavaC1.out.print(stream.getDestination(1, false));
            break;
        case Bytecodes._goto_w:
            /* falls through */
        case Bytecodes._jsr_w:
            JavaC1.out.print(stream.getDestination(1, true));
            break;
        }
    }

    /**
     * Iterates over the bytecodes and prints their names and operands.
     */
    private void printBytecodes() {
        JavaC1.out.println("BYTECODES:");
        JavaC1.out.println();
        stream = new BytecodeStream(method.getCode());
        while (stream.next()) {
            printBytecode();
            printOperands();
            JavaC1.out.println();
        }
        JavaC1.out.println();
    }

    /**
     * Prints the attributes of the specified exception handler.
     *
     * @param  handler  exception handler to be printed
     */
    private void printExceptionHandler(ExceptionHandler handler) {
        StringBuffer buf = new StringBuffer("                   ");
        buf.insert(2, handler.getStart());
        buf.insert(8, handler.getLimit());
        buf.insert(14, handler.getTarget());
        if (handler.getTypeIndex() != 0) {
            buf.insert(22, "class " + handler.getType());
        } else {
            buf.insert(22, "any");
        }
        JavaC1.out.println(buf.toString());
    }

    /**
     * Prints the exception table of the method.
     */
    private void printExceptionTable() {
        JavaC1.out.println("EXCEPTION TABLE:");
        JavaC1.out.println();
        JavaC1.out.println("__from__to____target__type__________________");
        List handlers = method.getExceptionHandlers();
        for (int i = 0; i < handlers.size(); i++) {
            printExceptionHandler((ExceptionHandler) handlers.get(i));
        }
        JavaC1.out.println();
    }

    /**
     * Prints the bytecodes and the exception table of the method.
     */
    public void trace() {
        printBytecodes();
        if (method.hasExceptionHandlers()) {
            printExceptionTable();
        }
    }
}
