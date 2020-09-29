/*
 * @(#)ConstantTable.java               1.10 02/11/27
 *
 * Copyright 1993-2002 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package javac1.backend.code;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores floating-point constants and maps them to memory addresses.
 *
 * @author   Thomas Kotzmann
 * @version  1.00
 */
public class ConstantTable {
    /**
     * The mapping of constants to their addresses.
     */
    private Map entries;

    /**
     * Constructs a new constant table.
     */
    public ConstantTable() {
        entries = new HashMap();
    }

    /**
     * Inserts the specified single-precision constant into the table.
     *
     * @param  value  single-precision constant to be inserted
     */
    public void appendFloat(float value) {
        entries.put(new Float(value), null);
    }

    /**
     * Inserts the specified double-precision constant into the table.
     *
     * @param  value  double-precision constant to be inserted
     */
    public void appendDouble(double value) {
        entries.put(new Double(value), null);
    }

    /**
     * Returns the address of the specified single-precision constant.
     *
     * @param   value  single-precision constant to be searched
     * @return  address of the specified constant
     */
    public int addressOfFloatConstant(float value) {
        Integer address = (Integer) entries.get(new Float(value));
        return (address == null) ? 0 : address.intValue();
    }

    /**
     * Returns the address of the specified double-precision constant.
     *
     * @param   value  double-precision constant to be searched
     * @return  address of the specified constant
     */
    public int addressOfDoubleConstant(double value) {
        Integer address = (Integer) entries.get(new Double(value));
        return (address == null) ? 0 : address.intValue();
    }

    /**
     * Emits the floating-point constants into the code buffer. To minimize
     * padding that results from alignment, first all single-precision and then
     * all double-precision values are emitted.
     *
     * @param  masm  assembler that provides access to the code
     */
    public void emitEntries(MacroAssembler masm) {
        Iterator iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mapping = (Map.Entry) iterator.next();
            if (mapping.getKey() instanceof Float) {
                masm.align(4);
                mapping.setValue(new Integer(masm.getCodePos()));
                float value = ((Float) mapping.getKey()).floatValue();
                int temp = Float.floatToIntBits(value);
                masm.emitLong(temp);
            }
        }
        iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry mapping = (Map.Entry) iterator.next();
            if (mapping.getKey() instanceof Double) {
                masm.align(8);
                mapping.setValue(new Integer(masm.getCodePos()));
                double value = ((Double) mapping.getKey()).doubleValue();
                long temp = Double.doubleToLongBits(value);
                masm.emitLong((int) temp);
                masm.emitLong((int) (temp >>> 32));
            }
        }
    }
}
