package java.lang;

import com.sun.squawk.loader.Member;

/**
 * An editor instance can be passed to the <code>edit</code> method
 * which visits each component in the symbols of a class. The Editor
 * instance can modify or remove the components. The <code>edit</code>
 * method returns the symbols string resulting from the editing.
 */
public interface SymbolEditor {
    public Member editMember(int mtype, Member member, int index);
}
