//if[SQUAWK]
package java.lang;

public class Class {

    public static Class forName(String className)       { throw new Error(); }

    // These fields are required (in this order) for the romizer to accept this class.
    Class    superClass;
    String   symbols;
    char[]   interfaceTypes;
    byte[][] interfaceSlotTables;
    char[]   classReferences;
    Object[] objectReferences;
    byte[]   oopMap;
    byte[][] staticMethods;
    byte[][] virtualMethods;
    char     type;
    char     access;
    char     elementType;
    byte     instanceFieldsWordCount;      // In words
    byte     staticFieldsWordCount;        // In words
    byte     pointerStaticFieldsWordCount; // In words
    byte     firstVirtualMethod;
    byte     slot_init;
    byte     slot_clinit;
    byte     slot_main;
    //byte     unused;

/*if[EXCLUDE]*/

    /**
     * Creates a new array of of a class.
     */
    static Object newArray(int cno, int length) throws InstantiationException, IllegalAccessException {
        return null;
    }

    /**
     * addDimension
     */
    static void addDimension(Object[] array, int nextDimention) throws InstantiationException, IllegalAccessException {
    }

    /**
     * Creates a new instance of a class.
     */
    static Object newInstance(int cno) throws InstantiationException, IllegalAccessException {
        return newInstance(cno,false);
    }

    static Object newInstance(int cno, boolean callConstructor) throws InstantiationException, IllegalAccessException {
        return null;
    }




    public Object newInstance() throws InstantiationException, IllegalAccessException {
        throw new Error();
    }


    public boolean isInstance(Object obj)               { throw new Error(); }
    public boolean isAssignableFrom(Class cls)          { throw new Error(); }
    public boolean isInterface()                        { throw new Error(); }
    public boolean isArray()                            { throw new Error(); }
    public String getName()                             { throw new Error(); }
    public String toString()                            { throw new Error(); }

/*end[EXCLUDE]*/
}
