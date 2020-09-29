package java.awt;

import java.io.*;
import java.util.*;
import com.sun.squawk.vm.ChannelOpcodes;

public class Toolkit {

    static Toolkit kit = new Toolkit();

    static public Toolkit getDefaultToolkit () {
        return kit;
    }

    public Image createImage(String ressourceName) {
        throw new RuntimeException ("jar ressources not yet supported");
    }

    public Image createImage(byte[] data, int offset, int length) {
        Image r = new Image();
        Native.execIO3(ChannelOpcodes.TOOLKIT_CREATEIMAGE, r.id(), offset, length, 0, 0, 0, data, null, null);
        return r;
    }
}



