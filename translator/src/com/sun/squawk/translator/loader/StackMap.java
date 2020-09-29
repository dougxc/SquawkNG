
package com.sun.squawk.translator.loader;
import com.sun.squawk.util.*;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.ir.*;
import com.sun.squawk.translator.util.*;

import java.io.IOException;

/**
 * The StackMap class represents a CLDC verification stack map for a single method.
 * A stack map's format in a classfile (as a sub-attribute of a Code attribute) is as follows:
 *
 * StackMap_attribute {
 *    u2 attribute_name_index;
 *    u4 attribute_length;
 *    u2 number_of_entries;
 *    {   u2 byte_code_offset;
 *        u2 number_of_locals;
 *        ty types_of_locals[number_of_locals];
 *        u2 number_of_stack_items;
 *        ty types_of_stack_items[number_of_stack_items];
 *    } entries [number_of_entries];
 * }
 *
 * The types (ty) are either 1-byte or 3-byte entities. They are encoded as follows:
 *
 *   Name            Code    Explanation
 *   ITEM_Bogus      0       an unknown or uninitialized value
 *   ITEM_Integer    1       a 32-bit integer
 *   ITEM_Float      2       (not used by the CLDC implementation of the verifier)
 *   ITEM_Double     3       (not used by the CLDC implementation of the verifier)
 *   ITEM_Long       4       a 64-bit integer
 *   ITEM_Null       5       the type of null
 *   ITEM_InitObject 6       explained in more detail below
 *   ITEM_Object     7       explained in more detail below
 *   ITEM_NewObject  8       explained in more detail below
 *
 * Note that the first seven types are encoded in 1 byte, and last two types are
 * encoded in 3 bytes.
 *
 * The meanings of the above types ITEM_InitObject, ITEM_Object, and
 * ITEM_NewObject are as follows:
 *
 *   ITEM_InitObject
 *   Before a constructor (the <init> method) for a class other than
 *   java.lang.Object calls a constructor (the <init> method) of one of its
 *   superclasses, the 'this' pointer has type ITEM_InitObject. (Comment: The
 *   verifier uses this type to enforce that a constructor must first invoke a
 *   superclass constructor before performing other operations on the 'this'
 *   pointer.)
 *
 *   ITEM_Object
 *   A class instance. The 1-byte type code (7) is followed by a 2-byte
 *   type_name_index (a u2). The type_name_index value must be a valid entry
 *   to the constant_pool table. The constant_pool entry at that index must be a
 *   CONSTANT_Class_info structure.
 *
 *   ITEM_NewObject
 *   An uninitialized class instance. The class instance has just been created by the
 *   new instruction, but a constructor (the <init> method) has not yet been
 *   invoked on it. The type code 8 is followed by a 2-byte
 *   new_instruction_index (a u2). The new_instruction_index must be a
 *   valid offset of a byte code instruction. The opcode of the byte code instruction
 *   must be new. (Comment: The uninitialized object is created by this new
 *   instruction. The verifier uses this type to enforce that an instance cannot be
 *   used until it is fully constructed.)
 */
public class StackMap {

    /**
     * This class represents the type maps for a single instruction address.
     */
    public static class Entry {
        /** The types on the operand stack. */
        public final Type[] stack;
        /** The types on the locals stack where all types are one logical entry. */
        public final Type[] logicalLocals;
        /** The types on the locals stack where double word types (longs and doubles) are represented by 2 single word types. */
        public final Type[] locals;

        /**
         * Constructor.
         */
        public Entry(Type[] stack, Type[] logicalLocals, Type[] locals) {
            this.stack          = stack;
            this.logicalLocals  = logicalLocals;
            this.locals         = locals;
        }

        /**
         * Return a String representation of this stack map entry.
         * @return a String representation of this stack map entry.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer(20);
            return buf.append("stack={").
                    append(Type.toSignature(stack, ", ")).
                    append("} locals={").
                    append(Type.toSignature(locals, ", ")).
                    append("}").toString();
        }

    }

    /** The enclosing method. */
    private final Method method;
    /** Enclosing translator (convenient short-cut for method.parent().getVM()). */
    final private Translator vm;
    /** A map from bytecode addresses for which there are stack map entries to the entries themselves. */
    private IntHashtable targets;

/*==============================================================================
 * Constructors
 *============================================================================*/

    /**
     * Construct a StackMap from a classfile input stream.
     * @param in The classfile input stream at the point where the stack map starts.
     * @param pool The constant pool of the enclosing method.
     * @param method The method to which the stack map belongs.
     * @param handlers The handlers of the method.
     */
    public StackMap(ClassFileInputStream in, ConstantPool pool, Method method, Liveness liveness) throws LinkageException {
        this.method  = method;
        this.vm      = in.getVM();

        // Read number_of_entries
        int nmaps  = in.readUnsignedShort("map-nmaps");
        if (nmaps == 0) {
            targets = new IntHashtable(0);
        } else {
            targets = new IntHashtable(nmaps);
        }
        int lastAddress = -1;

        for (int i = 0 ; i < nmaps ; i++) {
            int address = in.readUnsignedShort("map-address");
            if (address <= lastAddress) {
                throw in.classFormatError("Stackmap ip addresses not in order. address="+
                                                     address+" lastAddress="+lastAddress);
            }
           /*
            * An array of three arrays of Type's are produced
            *
            * [0] is an array of stack types that are in logical offsets
            * [1] is an array of local types that are in logical offsets
            * [2] is an array of local types that are in physical offsets
            */

            // Load the list of types on the local's stack for this address.
            Type[] logicalLocals = loadStackMapList(in, pool); // locals
            int localLongs = 0;
            for (int l = 0; l != logicalLocals.length; l++) {
                if (logicalLocals[l].isTwoWords()) {
                    localLongs++;
                }
            }

            // Calculate the physical locals stack from the logical ones
            Type[] physicalLocals;
            if (localLongs == 0) {
                physicalLocals = logicalLocals; // Physical = logical
            } else {
                physicalLocals = new Type[logicalLocals.length + localLongs];
                int javacIndex = 0;
                for (int j = 0 ; j < logicalLocals.length ; j++) {
                    Assert.that(logicalLocals[j] != null);
                    Type type = logicalLocals[j];

                    // Remove stack map entries for locals that are not live
                    // according to a Liveness attribute if it was provided
                    boolean isLive = (liveness == null || liveness.isLive(address, javacIndex, true));

                    physicalLocals[javacIndex++] = isLive ? type : vm.BOGUS;
                    if (type.isTwoWords()) {
                        physicalLocals[javacIndex++] = isLive ? type.secondWordType() : vm.BOGUS;
                    }
                }
                Assert.that(javacIndex == physicalLocals.length);
            }

            // Load the list of types on the operand stack for this address.
            Type[] stack  = loadStackMapList(in, pool); // stack

            Entry entry = new Entry(stack, logicalLocals, physicalLocals);
            targets.put(address, new Target(address, entry));
        }
    }

/*==============================================================================
 * Accessor methods
 *============================================================================*/

    /**
     * Return the number of stack map entries.
     */
    public int size() {
        return targets.size();
    }

    /**
     * Return the stackmap entry for an ip address.
     */
    public Target lookup(int ip) {
        return (Target)targets.get(ip);
    }

    /**
     * Return the stack map entries as a map from bytecode addresses to the
     * stack map entries (wrapped in a Target object) at those addresses.
     */
    public IntHashtable getTargets() {
        return targets;
    }

   /**
    * Read in the list of types on the locals or operand stack.
    * @param in The stream to read from which is currently positioned at the 2-byte number
    * giving the length of the list.
    * @param pool The pool of the enclosing method.
    * otherwise this value is null.
    * @return the list of types.
    */
    private Type[] loadStackMapList(ClassFileInputStream in, ConstantPool pool) throws LinkageException {
        int items = in.readUnsignedShort("map-items");
        Type[] list = new Type[items];
        int item = 0;

        for (; item < items ; item++) {
            byte type = in.readByte("map-type");
            switch (type) {
                case JVMConst.ITEM_Bogus:        list[item] = vm.BOGUS;         break;
                case JVMConst.ITEM_Integer:      list[item] = vm.INT;           break;
                case JVMConst.ITEM_Long:         list[item] = vm.LONG;          break;
/*if[FLOATS]*/
                case JVMConst.ITEM_Float:        list[item] = vm.FLOAT;         break;
                case JVMConst.ITEM_Double:       list[item] = vm.DOUBLE;        break;
/*end[FLOATS]*/
                case JVMConst.ITEM_Null:         list[item] = vm.NULLOBJECT;    break;
                case JVMConst.ITEM_InitObject:   list[item] = vm.INITOBJECT;    break;

                case JVMConst.ITEM_Object: {
                    int classIndex = in.readUnsignedShort("map-ITEM_Object");
                    // Don't want to trigger loading of referenced class
                    list[item] = pool.resolveType(classIndex);
                    break;
                }
                case JVMConst.ITEM_NewObject: {
                    int ipIndex = in.readUnsignedShort("map-ITEM_NewObject");
                    list[item] = addNewType(ipIndex);
                    break;
                }
                default: {
                    throw in.classFormatError("Bad stack map item tag: "+type);
                }
            }
        }
        return list;
    }

    /**
     * Create a temporary type that will represent the result of a "new" instruction
     * This is interned here in order to avoid having it in the VM's interned type
     * hashtable where it will just take up space when the verification is over.
     */
    public TypeProxy addNewType(int ip) throws LinkageException {
        TypeProxy type = (TypeProxy)internedTypes.get(ip);
        if (type == null) {
            type = TypeProxy.createForMap(vm, vm.NEWOBJECT.name()+"@"+ip);
            type.setSuperType(vm.NEWOBJECT);
            internedTypes.put(ip, type);
        }
        return type;
    }

   /**
    * Return a new type if one exists for the ip specifed
    */
    public TypeProxy findNewType(int ip) throws LinkageException {
        return (TypeProxy)internedTypes.get(ip);
    }

    /** Interning for proxy types. */
    private IntHashtable internedTypes = new IntHashtable(0);

}
