
package com.sun.squawk.translator.ir;
import com.sun.squawk.translator.util.*;

public interface ParameterVisitor {
    public Instruction doParameter(Instruction inst, Instruction parm);
}
