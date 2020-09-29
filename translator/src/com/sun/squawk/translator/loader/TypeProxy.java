
package com.sun.squawk.translator.loader;
import com.sun.squawk.translator.*;
import com.sun.squawk.translator.util.*;

public class TypeProxy extends Type {

    private Type proxy;

   /**
    * Static constructor only called from the Translator.
    */
    public static TypeProxy createForMap(Translator vm, String name) {
        return new TypeProxy(vm, name);
    }

    private TypeProxy(Translator vm, String name) {
        super(vm, name);
    }

    public void setProxy(Type proxy) {
        this.proxy = proxy;
    }

    public Type getProxy() {
        return proxy;
    }
}
