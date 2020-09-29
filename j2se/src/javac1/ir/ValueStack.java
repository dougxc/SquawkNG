/*
 * @(#)ValueStack.java                  1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.ir;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javac1.Assert;
import javac1.ir.IRScope;
import javac1.ir.instr.Constant;
import javac1.ir.instr.HiWord;
import javac1.ir.instr.Instruction;
import javac1.ir.instr.LoadLocal;
import javac1.ir.instr.Phi;
import javac1.ir.types.ValueType;

/**
 * This class represents a state array. A state array is used to keep track of
 * the most recent assignment to each local variable and of the values on the
 * expression stack.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ValueStack implements Cloneable {
    /**
     * The current scope of the intermediate representation.
     */
    private IRScope scope;
    
    /**
     * The array of local variables.
     */
    private Instruction[] locals;

    /**
     * The expression stack.
     */
    private LinkedList stack;

    /**
     * The monitor stack.
     */
    private LinkedList locks;

    /**
     * Constructs a new state array.
     *
     * @param  scope    the current scope
     * @param  locSize  total size of the local variables
     */
    public ValueStack(IRScope scope, int locSize) {
        this.scope = scope;
        this.locals = new Instruction[locSize];
        this.stack = new LinkedList();
        this.locks = new LinkedList();
    }

    /**
     * Construct a new state array from an existing one. Values on the
     * expression stack are replaced by phi instructions representing them.
     *
     * @param  state  the existing state array
     * @see    Phi
     */
    public ValueStack(ValueStack state) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(state != null, "state must exist");
        }
        this.scope = state.getScope();
        this.locals = new Instruction[state.getLocalsSize()];
        this.stack = new LinkedList();
        int i = 0;
        int size = state.getStackSize();
        while (i < size) {
            ValueType type = state.stackAt(i).getType();
            push(type, new Phi(scope, type, i, size));
            i += type.getSize();
        }
        locks = (LinkedList) state.locks.clone();
    }
    
    /**
     * Returns the current scope of the intermediate representation.
     *
     * @return  the current scope
     */
    protected IRScope getScope() {
        return scope;
    }

    /**
     * Checks that the value that is going to be pushed or popped has the
     * expected type.
     *
     * @param  tag  tag of the expected type
     * @param  x    value to be pushed or popped
     */
    private static void check(int tag, Instruction x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((tag == x.getType().getTag()) || ((tag == ValueType.objectTag) && (x.getType().getTag() == ValueType.addressTag)), "types must correspond");
        }
    }

    /**
     * Checks that two words belong to the same value and that this value has
     * the expected type.
     *
     * @param  tag  tag of the expected type
     * @param  lo   low word
     * @param  hi   high word
     */
    private static void check(int tag, Instruction lo, HiWord hi) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(hi.getLoWord() == lo, "incorrect stack pair");
        }
        check(tag, lo);
    }

    /**
     * Pushes an integer value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void ipush(Instruction x) {
        check(ValueType.intTag, x);
        stack.addLast(x);
    }

    /**
     * Pushes a single-precision floating-point value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void fpush(Instruction x) {
        check(ValueType.floatTag, x);
        stack.addLast(x);
    }

    /**
     * Pushes an object value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void apush(Instruction x) {
        check(ValueType.objectTag, x);
        stack.addLast(x);
    }

    /**
     * Pushes an address value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void rpush(Instruction x) {
        check(ValueType.addressTag, x);
        stack.addLast(x);
    }

    /**
     * Pushes a long integer value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void lpush(Instruction x) {
        check(ValueType.longTag, x);
        stack.addLast(x);
        stack.addLast(new HiWord(scope, x));
    }

    /**
     * Pushes a double-precision floating-point value onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void dpush(Instruction x) {
        check(ValueType.doubleTag, x);
        stack.addLast(x);
        stack.addLast(new HiWord(scope, x));
    }

    /**
     * Pushes a value of a certain type onto the expression stack.
     *
     * @param  type  type of the value
     * @param  x     value to be pushed
     */
    public void push(ValueType type, Instruction x) {
        check(type.getTag(), x);
        stack.addLast(x);
        if (type.isDoubleWord()) {
            stack.addLast(new HiWord(scope, x));
        }
    }

    /**
     * Takes an integer value off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction ipop() {
        Instruction x = (Instruction) stack.removeLast();
        check(ValueType.intTag, x);
        return x;
    }

    /**
     * Takes a single-precision floating-point value off the stack and returns
     * that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction fpop() {
        Instruction x = (Instruction) stack.removeLast();
        check(ValueType.floatTag, x);
        return x;
    }

    /**
     * Takes an object value off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction apop() {
        Instruction x = (Instruction) stack.removeLast();
        check(ValueType.objectTag, x);
        return x;
    }

    /**
     * Takes an address value off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction rpop() {
        Instruction x = (Instruction) stack.removeLast();
        check(ValueType.addressTag, x);
        return x;
    }

    /**
     * Takes a long integer value off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction lpop() {
        HiWord hi = (HiWord) stack.removeLast();
        Instruction lo = (Instruction) stack.removeLast();
        check(ValueType.longTag, lo, hi);
        return lo;
    }

    /**
     * Takes a double-precision floating-point value off the stack and returns
     * that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction dpop() {
        HiWord hi = (HiWord) stack.removeLast();
        Instruction lo = (Instruction) stack.removeLast();
        check(ValueType.doubleTag, lo, hi);
        return lo;
    }

    /**
     * Takes a value of a certain type off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction pop(ValueType type) {
        if (type.isSingleWord()) {
            Instruction x = (Instruction) stack.removeLast();
            check(type.getTag(), x);
            return x;
        } else {
            HiWord hi = (HiWord) stack.removeLast();
            Instruction lo = (Instruction) stack.removeLast();
            check(type.getTag(), lo, hi);
            return lo;
        }
    }

    /**
     * Pushes a value of an arbitrary type onto the expression stack.
     *
     * @param  x  value to be pushed
     */
    public void rawPush(Instruction x) {
        stack.addLast(x);
    }

    /**
     * Takes a value of an arbitrary type off the stack and returns that value.
     *
     * @return  value at the top of the stack
     */
    public Instruction rawPop() {
        return (Instruction) stack.removeLast();
    }

    /**
     * Takes values off the stack and returns them as a list of arguments.
     *
     * @param   argSize  total size of the arguments
     * @return  the list of arguments
     */
    public List popArguments(int argSize) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(stack.size() >= argSize, "not enough values on the stack");
        }
        List args = new ArrayList(argSize);
        int base = stack.size() - argSize;
        int i = base;
        while (i < stack.size()) {
            Instruction value = stackAt(i);
            args.add(value);
            i += value.getType().getSize();
        }
        truncateStack(base);
        return args;
    }

    /**
     * Returns the value on the expression stack at the specified index.
     *
     * @param   index  index into the stack
     * @return  value at the specified index
     */
    public Instruction stackAt(int index) {
        Instruction x = (Instruction) stack.get(index);
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!(x instanceof HiWord), "index points to high word");
            Assert.that((x == null) || x.getType().isSingleWord() || (((HiWord) stack.get(index + 1)).getLoWord() == x), "expression stack inconsistent");
        }
        return x;
    }

    /**
     * Returns the value on the expression stack before the specified index.
     *
     * @param   index  index into the stack
     * @return  value before the specified index
     */
    public Instruction stackBefore(int index) {
        Instruction x = (Instruction) stack.get(index - 1);
        if (x instanceof HiWord) {
            HiWord hi = (HiWord) x;
            x = (Instruction) stack.get(index - 2);
            if (Assert.ASSERTS_ENABLED) {
                Assert.that(x == hi.getLoWord(), "expression stack inconsistent");
            }
        }
        return x;
    }

    /**
     * Clears the expression stack.
     */
    public void clearStack() {
        stack.clear();
    }

    /**
     * Returns the current size of the expression stack.
     *
     * @return  size of the stack
     */
    public int getStackSize() {
        return stack.size();
    }

    /**
     * Returns the value that has been assigned to the local variable at the
     * specified index most recently.
     *
     * @param   index  index into the array of local variables
     * @return  current value of the local variable
     */
    public Instruction loadLocal(int index) {
        Instruction x = locals[index];
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(!(x instanceof HiWord), "index points to high word");
            Assert.that((x == null) || x.getType().isSingleWord() || (((HiWord) locals[index + 1]).getLoWord() == x), "locals inconsistent");
        }
        return x;
    }

    /**
     * Stores the value that has been assigned to the local variable at the
     * specified index most recently.
     *
     * @param  index  index into the array of local variables
     * @param  x      the new value of the variable
     */
    public void storeLocal(int index, Instruction x) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that((x != null) && !(x instanceof HiWord), "illegal value");
        }
        killLocal(index);
        locals[index] = x;
        if (x.getType().isDoubleWord()) {
            killLocal(index + 1);
            locals[index + 1] = new HiWord(scope, x);
        }
    }

    /**
     * Kills the stored value of the local variable at the specified index.
     *
     * @param  index  index into the array of local variables
     */
    public void killLocal(int index) {
        Instruction x = locals[index];
        if (x != null) {
            if (x.getType().isDoubleWord()) {
                if (x instanceof HiWord) {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(((HiWord) x).getLoWord() == locals[index - 1], "stack inconsistent");
                    }
                    locals[index - 1] = null;
                } else {
                    if (Assert.ASSERTS_ENABLED) {
                        Assert.that(((HiWord) locals[index + 1]).getLoWord() == x, "locals inconsistent");
                    }
                    locals[index + 1] = null;
                }
            }
            locals[index] = null;
        }
    }

    /**
     * Clears the array of local variables.
     */
    public void clearLocals() {
        for (int i = 0; i < locals.length; i++) {
            locals[i] = null;
        }
    }

    /**
     * Returns the totals size of all local variables.
     *
     * @return  size of local variables
     */
    public int getLocalsSize() {
        return locals.length;
    }

    /**
     * Pushes the bytecode index of a monitorenter instruction onto the monitor
     * stack and sets the minimal number of locks for the current scope.
     *
     * @param   scope  current scope
     * @param   bci    bytecode index of monitorenter
     * @return  number of this lock
     */
    public int lock(IRScope scope, int bci) {
        locks.addLast(new Integer(bci));
        scope.setMinNumberOfLocks(locks.size());
        return locks.size() - 1;
    }

    /**
     * Removes the lock at the top of the monitor stack.
     *
     * @return  number of the lock removed
     */
    public int unlock() {
        locks.removeLast();
        return locks.size();
    }

    /**
     * Returns the number of locks on the monitor stack.
     *
     * @return  number of locks
     */
    public int getLocksSize() {
        return locks.size();
    }

    /**
     * Pins all instructions that load a variable's value that still resides on
     * the stack and will propably be used later.
     *
     * @param  index  index of the local variable
     */
    public void pinStackLocals(int index) {
        pinStackAll();
    }

    /**
     * Pins all instructions that load any field's value that still resides on
     * the stack and will probably be used later.
     */
    public void pinStackFields() {
        pinStackAll();
    }

    /**
     * Pins all instructions that load an array's element that still resides on
     * the stack and will probably be used later.
     */
    public void pinStackIndexed() {
        pinStackAll();
    }

    /**
     * Pins all instructions that load a field or an array's element that still
     * resides on the stack and will probably be used later.
     */
    public void pinStackForStateSplit() {
        int i = 0;
        while (i < stack.size()) {
            Instruction x = stackAt(i);
            if (!((x instanceof LoadLocal) || (x instanceof Constant))) {
                x.setPinned(true);
            }
            i += x.getType().getSize();
        }
    }

    /**
     * Pins all instructions loading any value that still resides on the stack
     * and will probably be used later.
     */
    public void pinStackAll() {
        int i = 0;
        while (i < stack.size()) {
            Instruction x = stackAt(i);
            x.setPinned(true);
            i += x.getType().getSize();
        }
    }

    /**
     * Truncates the expression stack to the specified size.
     *
     * @param  size  the new size of the stack
     */
    public void truncateStack(int size) {
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(size <= stack.size(), "cannot increase length");
        }
        for (int i = stack.size() - size; i > 0; i--) {
            stack.removeLast();
        }
    }

    /**
     * Tests if there are any values on the stack.
     *
     * @return  whether or not the stack is empty
     */
    public boolean isStackEmpty() {
        return stack.isEmpty();
    }

    /**
     * Applies the operation represented by the specified value closure object
     * to each value on the expression stack.
     *
     * @param  vc  the value closure
     */
    public void doValues(ValueClosure vc) {
        int i = 0;
        while (i < stack.size()) {
            Instruction x = stackAt(i);
            Instruction y = vc.doValue(x);
            stack.set(i, y);
            if (x != y) {
                if (Assert.ASSERTS_ENABLED) {
                    Assert.that(!(y instanceof HiWord), "value is high word");
                    Assert.that(x.getType().getTag() == y.getType().getTag(), "types must match");
                }
                if (y.getType().isDoubleWord()) {
                    stack.set(i + 1, new HiWord(scope, y));
                }
            }
            i += x.getType().getSize();
        }
    }

    /**
     * Indicates whether or not some other state array is equal to this one.
     *
     * @param   obj  the state array with which to compare
     * @return  whether or not the state arrays are equal
     */
    public boolean equals(Object obj) {
        ValueStack state = (ValueStack) obj;
        if (Assert.ASSERTS_ENABLED) {
            Assert.that(state != null, "state must exist");
            Assert.that(getLocalsSize() == state.getLocalsSize(), "locals sizes must correspond");
            Assert.that(getStackSize() == state.getStackSize(), "stack sizes must correspond");
            Assert.that(getLocksSize() == state.getLocksSize(), "locks sizes must correspond");
        }
        int i = 0;
        while (i < stack.size()) {
            Instruction x = stackAt(i);
            Instruction y = state.stackAt(i);
            if (x.getType().getTag() != y.getType().getTag()) {
                return false;
            }
            i += x.getType().getSize();
        }
        for (int j = 0; j < locks.size(); j++) {
            if (locks.get(j) != state.locks.get(j)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a copy of this state array with cleared locals.
     *
     * @return  copy of this state array
     */
    public synchronized Object clone() {
        ValueStack cloned = null;
        try {
            cloned = (ValueStack) super.clone();
            cloned.locals = new Instruction[locals.length];
            cloned.stack = (LinkedList) stack.clone();
            cloned.locks = (LinkedList) locks.clone();
        } catch (CloneNotSupportedException exc) {
            Assert.shouldNotReachHere();
        }
        return cloned;
    }
}
