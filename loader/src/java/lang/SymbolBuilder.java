package java.lang;

import com.sun.squawk.loader.Member;

public class SymbolBuilder extends ByteBuffer {

    /**
     * Constructor
     */
    public SymbolBuilder(ByteBuffer name) {
        addBuffer(name);
    }

    /**
     * Constructor
     */
    public SymbolBuilder(String name) {
        int classNameLength = name.length();
        addInt(classNameLength);
        for (int i = 0 ; i < classNameLength ; i++) {
            addByte(name.charAt(i));
        }
    }

    /**
     * Constructor
     */
    public SymbolBuilder(StringOfSymbols symbols) {
        int classNameLength = symbols.getClassNameLength();
        addInt(classNameLength);
        for (int i = 0 ; i < classNameLength ; i++) {
            addByte(symbols.getClassNameChar(i));
        }
    }

    /**
     * Add a segment marker to the buffer
     */
    public void addSegmentMarker(int mtype) {
        addByte(mtype);
    }

    /**
     * Convert to a StringOfSymbols
     */
    public StringOfSymbols toStringOfSymbols() {
        StringOfSymbols res = StringOfSymbols.create(data, count);
        data = null; // the data buffer is make into a string if data.length == count
        return res;
    }

}
